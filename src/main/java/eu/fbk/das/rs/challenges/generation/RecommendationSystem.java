package eu.fbk.das.rs.challenges.generation;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getLevel;
import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.utils.Utils.*;
import static it.smartcommunitylab.model.ChallengeConcept.StateEnum.COMPLETED;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.fbk.das.GamificationConfig;
import eu.fbk.das.utils.Pair;
import org.apache.commons.io.IOUtils;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.utils.Utils;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import it.smartcommunitylab.model.ChallengeConcept;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Recommandation System main class, requires running Gamification Engine in
 * order to run
 */
public class RecommendationSystem {

    private static final Logger logger = Logger.getLogger(RecommendationSystem.class);
    
    private final Map<String, String> cfg;

    public DateTime lastMonday;
    private DateTime execDate;
    protected Integer chaWeek;

    public GamificationEngineRestFacade facade;
    public String gameId;
    public String host;
    public String user;
    public String pass;

    public RecommendationSystemChallengeGeneration rscg;
    public RecommendationSystemChallengeValuator rscv;
    public RecommendationSystemChallengeFilteringAndSorting rscf;
    private RecommendationSystemStatistics stats;
    
    private Set<String> modelTypes;

    boolean debug = false;
    
    public RecommendationSystem(Map<String, String> cfg) {
        this.cfg = cfg;

        this.host = cfg.get("HOST");
        this.user = cfg.get("USER");
        this.pass = cfg.get("PASS");
        this.gameId = cfg.get("GAMEID");;

        facade = new GamificationEngineRestFacade(host, user, pass);

        rscv = new RecommendationSystemChallengeValuator();
        rscg = new RecommendationSystemChallengeGeneration(this);
        rscf = new RecommendationSystemChallengeFilteringAndSorting();
        stats = new RecommendationSystemStatistics(this, true);
        // dbg(logger, "Recommendation System init complete");
    }

    public RecommendationSystem() {
        this(new GamificationConfig(true).extract());
    }

    // generate challenges
    public List<ChallengeExpandedDTO> recommend(String pId, Set<String> modelTypes,  Map<String, String> creationRules, Map<String, Object> challengeValues) {

        prepare(challengeValues);

        this.modelTypes = modelTypes;

        PlayerStateDTO state = facade.getPlayerState(gameId, pId);
        int lvl = getLevel(state);

        // TODO check, serve ancora?
        String exp = getPlayerExperiment(pId);

        // OLD method
        // List<ChallengeExpandedDTO> cha = generation2019(pId, state, d, lvl);

       List<ChallengeExpandedDTO> cha = generationRule(pId, state, execDate, lvl, creationRules);

        for (ChallengeExpandedDTO c: cha) {
            c.setInfo("playerLevel", lvl);
            c.setInfo("player", pId);

            c.setHide(true);
        }

        return cha;

    }

    protected void prepare(Map<String, Object> challengeValues) {
        chaWeek = (Integer) challengeValues.get("challengeWeek");
        Date execDateParam = (Date) challengeValues.get("exec");
        execDate = new DateTime(execDateParam.getTime());
        // Set next monday as start, and next sunday as end
        int week_day = execDate.getDayOfWeek();
        int d = (7 - week_day) + 1;
        lastMonday = execDate.minusDays(week_day-1).minusDays(7);
        stats.checkAndUpdateStats(execDate);
        rscv.prepare(stats);
        rscg.prepare(chaWeek);
    }

    private List<ChallengeExpandedDTO> generationRule(String pId, PlayerStateDTO state, DateTime d, int lvl, Map<String, String> creationRules) {
        String rule = creationRules.get(String.valueOf(lvl));
        if (rule == null) rule = creationRules.get("other");

        if ("empty".equals(rule))
            return new ArrayList<>();

        List<ChallengeExpandedDTO> s = new ArrayList<>();
        ChallengeExpandedDTO g = rscg.getRepetitive(pId);
        s.add(g);

        if ("fixedOne".equals(rule))
            s.addAll(getAssigned(state, d, 1));
        else if ("choiceTwo".equals(rule))
            s.addAll(assignLimit(2, state, d));
        else if ("choiceThree".equals(rule))
            s.addAll(assignLimit(3, state, d));
        else if ("choiceTwoV2".equals(rule))
            s.addAll(assignLimitV2(2, state, d));
        else if ("choiceThreeV2".equals(rule))
            s.addAll(assignLimitV2(3, state, d));
        else
            s.clear();

        return s;

    }



    private List<ChallengeExpandedDTO> generation2019(String pId, PlayerStateDTO state, DateTime d, int lvl, String exp) {


        // If level high enough, additionally choose to perform experiment
        /*
        if (Utils.randChance(0.2))
            for (ChallengeExpandedDTO c: cha) {
                c.setInfo("experiment", "tgt");
            }
            */

        //List<ChallengeExpandedDTO> cha = assignLimit(3, state, d);

        // cha = assignLimit(3, state, d);

        // return assignOne(state, d);

        // return recommendForecast(state, d);

        // return recommendAll(state, d);


        // if level is 0, none
        if (lvl == 0)
            return new ArrayList<>();

        List<ChallengeExpandedDTO> s = new ArrayList<>();
        assignSurveyPrediction(pId, s);
        assignSurveyEvaluation(pId, s);
        assignRecommendFriend(pId, s);

        ChallengeExpandedDTO g = rscg.getRepetitive(pId);

        // if level is 1, assign two fixed
        if (lvl == 1) {
            s.add(g);
            s.addAll(getAssigned(state, d, 1));
            return s;
        }

        // if level is 2, assign 1 repetitive and two choices
        if (lvl == 2) {
            s.add(g);
            s.addAll(assignLimit(2, state, d));
            return s;
        }

        // if level is 3, assign 1 repetitive and three choices
        if (lvl == 3) {
            s.add(g);
            s.addAll(assignLimit(3, state, d));
            return s;
        }

        s.addAll(assignLimit(3, state, d));
        return s;

    }

    public void assignRecommendFriend(String pId, List<ChallengeExpandedDTO> s) {

        String l = "first_recommend";

        if (existsAssignedChallenge(pId, l)) return;

        ChallengeExpandedDTO cha = rscg.prepareChallangeImpr(l);
        cha.setStart(execDate.toDate());
        cha.setModelName("absoluteIncrement");
        cha.setData("target", 1.0);
        cha.setData("bonusScore", 200.0);
        s.add(cha);
    }

    private boolean existsAssignedChallenge(String pId, String l) {
        List<ChallengeConcept> currentChallenges = facade.getChallengesPlayer(gameId, pId);
        for (ChallengeConcept cha: currentChallenges) {
            if (cha.getName().contains(l))
                return true;
        }
        return  false;
    }

    private boolean existsAssignedSurvey(String pId, String l) {
        List<ChallengeConcept> currentChallenges = facade.getChallengesPlayer(gameId, pId);
        for (ChallengeConcept cha: currentChallenges) {
            if (!("survey".equals(cha.getModelName())))
                continue;

            Map<String, Object> f = (Map<String, Object>) cha.getFields();
            String sv = (String) f.get("surveyType");

            if (l.equals(sv))
                return true;
        }
        return  false;
    }


    private boolean existsAssignedAnCompletedSurvey(String pId, String l) {
        List<ChallengeConcept> currentChallenges = facade.getChallengesPlayer(gameId, pId);
        for (ChallengeConcept cha: currentChallenges) {
            if (!("survey".equals(cha.getModelName())))
                continue;

            Map<String, Object> f = (Map<String, Object>) cha.getFields();
            String sv = (String) f.get("surveyType");

            if (!l.equals(sv))
                continue;

            // if (!"COMPLETED".equals(c))
            if (cha.getState() != COMPLETED)
                continue;

            return true;
        }
        return  false;
    }

    private String getPlayerExperiment(String pId) {
        Map<String, Object> cs = facade.getCustomDataPlayer(gameId, pId);
        return (String) cs.get("exp");
    }

    private void assignSurveyEvaluation(String pId, List<ChallengeExpandedDTO> s) {

        String l = "evaluation";

        Map<String, Object> cs = facade.getCustomDataPlayer(gameId, pId);

        int w = this.getChallengeWeek(execDate);

        if (cs == null)
            return;

        String exp = (String) cs.get("exp");

        if (exp == null)
            return;

        int dw =  w - (Integer) cs.get("exp-start");
        if (dw < 6) return;

        p(pId);

        if (existsAssignedSurvey(pId, l)) return;
        //  if (existsAssignedAnCompletedSurvey(pId, l)) return;

        // ADD NEW CHALLENGE SURVEY PREDICTION HERE

        ChallengeExpandedDTO cha = rscg.prepareChallangeImpr("survey_" + l);

        cha.setStart(execDate.toDate());

        cha.setModelName("survey");
        cha.setData("surveyType", l);

        cha.delData("counterName");
        cha.delData("periodName");

        // link?

        s.add(cha);

        /* modelName: survey
                surveyType: prediction
                bonusScore
                link
                bonusPointType */
    }

    private void assignSurveyPrediction(String pId, List<ChallengeExpandedDTO> s) {

        String l = "prediction";

        if (existsAssignedSurvey(pId, l)) return;

        // ADD NEW CHALLENGE SURVEY PREDICTION HERE

        ChallengeExpandedDTO cha = rscg.prepareChallangeImpr("survey_" + l);

        cha.setStart(execDate.toDate());

        cha.setModelName("survey");
        cha.setData("surveyType", l);

        cha.delData("counterName");
        cha.delData("periodName");

        // link?

        s.add(cha);

        /* modelName: survey
                surveyType: prediction
                bonusScore
                link
                bonusPointType */
    }


    private List<ChallengeExpandedDTO> firstWeeks(PlayerStateDTO state, DateTime d, int lvl) {

        if (lvl <= 0)
            return new ArrayList<>();
        else
            // assign only one
            return getAssigned(state, d, 1);
    }

    private List<ChallengeExpandedDTO> oldWeeks(PlayerStateDTO state, DateTime d, int lvl) {

        // if level is 0, none
        if (lvl == 0)
            return new ArrayList<>();

        // if level is 1, assign two fixed
        if (lvl == 1)
            return getAssigned(state, d, 2);

        if (lvl == 2) {
            return assignLimit(2, state, d);
        }

        // See if we want to perform tests 

        if (Utils.randChance(0.7)) {
            List<ChallengeExpandedDTO> res = assignForecast(state, d);
            if (res != null && res.size() == 3)
                return  res;
        }

            return  assignLimit(3, state, d);
    }

    // cerca di prevedere obiettivo, fornisce stime diverse di punteggio
    protected List<ChallengeExpandedDTO> assignForecast(PlayerStateDTO state, DateTime execDate) {

        String max_mode = null;
        int max_pos = -1;
        Double max_value = 0.0;

        for (String mode : modelTypes) {
            Double modeValue = getWeeklyContentMode(state, mode, execDate);
            int pos = stats.getPosition(mode, modeValue);

            if (modeValue > 0 && pos > max_pos) {
                max_pos = pos;
                max_mode = mode;
                max_value = modeValue;
            }
        }

        if (max_mode == null || max_value == 0)
            return assignLimit(3, state, execDate);

            List<ChallengeExpandedDTO> l_cha = rscg.forecast(state, max_mode, execDate);

            int ix = 0;
            for(ChallengeExpandedDTO cha: l_cha) {

                if (cha == null)
                    p("ciao");

                cha.setOrigin("rs");
                if (ix == 0)
                    cha.setPriority(2);
                else
                    cha.setPriority(1);

                cha.setInfo("id", ix);
                // cha.setInfo("experiment", "tgt");
                cha.setState("proposed");

                ix++;
            }

        List<ChallengeExpandedDTO> new_l_cha = rscf.filter(l_cha, state, execDate);

        return new_l_cha;

    }

    private List<ChallengeExpandedDTO> getAssigned(PlayerStateDTO state, DateTime d, int num) {
        return getAssigned(state, d, num, "treatment");
    }

    private List<ChallengeExpandedDTO> getAssigned(PlayerStateDTO state, DateTime d, int num, String exp) {
        List<ChallengeExpandedDTO> list = recommendAll(state, d);
        if (list == null || list.isEmpty())
            return null;

        ArrayList<ChallengeExpandedDTO> res = new ArrayList<ChallengeExpandedDTO>();

        for (int ix = 0; ix < num && ix < list.size(); ix ++) {

            ChallengeExpandedDTO chosen = list.get(ix);

            chosen.setState("assigned");
            chosen.setOrigin("rs");
            chosen.setInfo("id", ix);

            res.add(chosen);
        }

        return res;

    }


    protected List<ChallengeExpandedDTO> assignLimit(int limit, PlayerStateDTO state, DateTime d) {

        List<ChallengeExpandedDTO> list = recommendAll(state, d);
        if (list == null || list.isEmpty())
            return null;

        Set<String> modes = new HashSet<>();

        ArrayList<ChallengeExpandedDTO> res = new ArrayList<>();
        ChallengeExpandedDTO cha = list.get(0);
        cha.setPriority(2);
        res.add(cha);
        String counter = (String) cha.getData("counterName");
        modes.add(counter);
        cha.setInfo("id", 0);

        int ix = 1;

        for (int i = 0; i < limit -1 && ix < list.size(); i++) {
            boolean found = false;
            while (!found) {

                cha = list.get(ix++);
                counter = (String) cha.getData("counterName");
                if (modes.contains(counter))
                    continue;
                modes.add(counter);
                cha.setPriority(1);
                cha.setInfo("id", i+1);
                res.add(cha);
                found = true;
            }
        }

        for (ChallengeExpandedDTO cdd: res) {
            // cdd.setInfo("experiment", "cho");
            cdd.setOrigin("rs");
            cdd.setState("proposed");
        }

        return res;

    }


    protected List<ChallengeExpandedDTO> assignLimitV2(int limit, PlayerStateDTO state, DateTime d) {

        // Check if we have to intervene
        if (repetitiveIntervene(state, d.toDate()))
            // TODO
            return null;

        return assignLimit(limit, state, d);
    }

    public boolean repetitiveIntervene(PlayerStateDTO state, Date dt) {

        try {
            Map<Integer, double[]> cache = extractRipetitivePerformance(state, dt);
            // if null does not intervene
            if (cache == null) return false;
            // analyze if we have to assign repetitive
            double ent = repetitiveInterveneAnalyze(cache);

            if (ent > 1.4)
                return true;

        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected Map<Integer, double[]> extractRipetitivePerformance(PlayerStateDTO state, Date dt) throws IOException, ParseException {
        // p(state.getPlayerId());
        String query = getRepetitiveQuery(state.getPlayerId(), dt);
        // p(query);

        String url = "https://api-dev.smartcommunitylab.it/gamification-stats-" + this.gameId + "-*/_search?size=0";
        String user = cfg.get("API_USER");
        String pass = cfg.get("API_PASS");

        String response = getHttpResponse(query, url, user, pass);
        // p(response);
        Map<Integer, double[]> cache = getTimeSeriesPerformance(response);

        return cache;
    }

    public double repetitiveTarget(PlayerStateDTO state, double repDifficulty) {
        Pair<Double, Double> res = rscg.forecastMode(state, "green leaves");
        // repDifficulty should be in (1-15) range, def value 5
        double repTarget = res.getSecond() / (15 - repDifficulty);
        return repTarget;
    }

    protected double repetitiveInterveneAnalyze(Map<Integer, double[]> cacheEnt) {

            List<Double> allEnt = new ArrayList<>();

            for (Integer wk: cacheEnt.keySet()) {
                double[] cacheWeek = cacheEnt.get(wk);
                double tot = 0, ent = 0;
                for (double e: cacheWeek) tot += e;
                if (tot <= 0) continue;
                for (double e: cacheWeek) {
                    // p(e);
                    if (e <= 0) continue;
                    double p = e / tot;
                    ent += p * Math.log(p);
                }

                allEnt.add(ent);
            }

            // WMA to entropy

        double den = 0;
        double num = 0;

        int v = allEnt.size();

            for (int ix = 0; ix < v; ix++) {
                double c = allEnt.get(ix);

                den += (v -ix) * c;
                num += (v -ix);
            }


        double ent = den / num;

            return ent;
    }

    protected Map<Integer, double[]> getTimeSeriesPerformance(String response) throws ParseException {

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)  parser.parse(response);
        JSONObject aggr = (JSONObject) json.get("aggregations");
        JSONObject spd = (JSONObject) aggr.get("score_per_day");
        JSONArray buckets = (JSONArray) spd.get("buckets");

        boolean start = false;
        int week = 1;
        int gt_week = 0;
        Map<String, Map<Integer, double[]>> cacheAll = new HashMap<>();

        Map<String, Double> totMode = new HashMap<>();

        for (int i = 0; i < buckets.size(); i++) {
            JSONObject bck = (JSONObject) buckets.get(i);
            Long k = (Long) bck.get("key");
            Date d = new Date(k);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);

            int dof = cal.get(Calendar.DAY_OF_WEEK);
            if (dof == Calendar.MONDAY) {
                if (!start)
                    start = true;
                else
                    week += 1;
            }
            if (!start) continue;

            JSONObject by_concept = (JSONObject) bck.get("by_concept");
            JSONArray buckets_m = (JSONArray) by_concept.get("buckets");

            if (dof == 1)
                dof = 6;
            else dof -= 2;

            for (int j = 0; j < buckets_m.size();j++) {
                JSONObject aux = (JSONObject) buckets_m.get(j);

                String mode = (String) aux.get("key");
                JSONObject aux2 = (JSONObject) aux.get("score");
                Double score = (Double) aux2.get("value");

                if (!totMode.containsKey(mode))
                    totMode.put(mode, score);
                else
                    totMode.put(mode, totMode.get(mode) + score);


                if (!cacheAll.containsKey(mode))
                    cacheAll.put(mode, new HashMap<>());

                Map<Integer, double[]> cacheMode = cacheAll.get(mode);
                if (!cacheMode.containsKey(week))
                    cacheMode.put(week, new double[7]);

                double[] cacheWeek = cacheMode.get(week);
                cacheWeek[dof] = score;
                cacheMode.put(week, cacheWeek);

                if (week > gt_week) gt_week = week;
                        // p(mode);
                // p(score);
            }

            // p(d);
            // p(dof);
            // p(buckets_m);
        }

        // Get strongest mode
        String max_mode = null;
        double max_value = -1;
        for (String m: totMode.keySet()) {
            double v = totMode.get(m);
            if (v > max_value) {
                max_value = v;
                max_mode = m;
            }
        }

        if (max_mode == null) return null;

        double perf = 0;
        for (Double v: cacheAll.get(max_mode).get(gt_week))
            perf += v;

        int position = stats.getPosition(max_mode, perf);

        // check if strongest mode is in the 10% performers, otherwise return none
        if (position < 5)
            return null;

        /*
        if (debug)
            pf("%.2f,%d,", perf, position);
         */

        return cacheAll.get(max_mode);
    }

    private String getHttpResponse(String query, String url, String user, String pass) throws IOException {

        Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);

        HttpHost targetHost = new HttpHost("api-dev.smartcommunitylab.it", 403, "https");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(user, pass));

        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());

        // Add AuthCache to the execution context
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credsProvider);
        context.setAuthCache(authCache);

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(query));

        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(
                httpPost, context);

        int statusCode = response.getStatusLine().getStatusCode();

        String jsonResponse = null;

        try(InputStream content = response.getEntity().getContent()) {
            //With apache
            jsonResponse = IOUtils.toString(content, "UTF-8");
        } catch (UnsupportedOperationException | IOException e) {
            logExp(e);
        }

        return jsonResponse;
    }

    protected String getRepetitiveQuery(String playerId, Date dt) throws IOException, ParseException {
        InputStream is = RecommendationSystem.class.getClassLoader().getResourceAsStream("query/past-performances.json");
        // p(is);
        String json = IOUtils.toString(is, StandardCharsets.UTF_8.name());
        JSONParser parser = new JSONParser();
        JSONObject all = (JSONObject) parser.parse(json);
        // replace parameters in query
        JSONObject query = (JSONObject) all.get("query");
        JSONObject bool = (JSONObject) query.get("bool");
        JSONArray must = ((JSONArray) bool.get("must"));
        // p(must);
        // Replace player Id
        JSONObject mustPl = (JSONObject) must.get(0);
        JSONObject match = (JSONObject) mustPl.get("match");
        match.put("playerId", playerId);
        // Replace date
        JSONObject mustDt = (JSONObject) must.get(2);
        // p(mustDt);
        JSONObject mustRange = (JSONObject) mustDt.get("range");
        JSONObject mustDtExe = (JSONObject) mustRange.get("executionTime");
        // p(mustDtExe);
        // last saturday
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(dt);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date start = cal.getTime();
        // p(start);
        mustDtExe.put("lt", start.getTime());
        // previous five weeks, start monday
        cal.add(Calendar.WEEK_OF_YEAR, -5);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date end = cal.getTime();
        // p(end);
        mustDtExe.put("gte", end.getTime());

        /*
        Writer wr = new FileWriter("test.json");
        all.writeJSONString(wr);;
        wr.close();
        */

        return all.toString();
    }

    public List<ChallengeExpandedDTO> recommendAll(PlayerStateDTO state, DateTime d) {

        List<ChallengeExpandedDTO> challanges = new ArrayList<>();
        for (String mode : modelTypes) {
            List<ChallengeExpandedDTO> l_cha = rscg.generate(state, mode);

            if (l_cha.isEmpty())
                continue;

            challanges.addAll(l_cha);

        }

        return rscf.filter(challanges, state, d);
    }

    public static int getChallengeWeek(DateTime d) {
        int s = getChallengeDay(d);
        return (s/7) +1;
    }

    public static int getChallengeDay(DateTime d) {
        return daysApart(d, parseDate("29/10/2018"));
    }

    /*
    public Map<String, List<ChallengeExpandedDTO>> recommendation(
            List<Player> gameData, DateTime start, DateTime end)
            throws NullPointerException {

        logger.info("Recommendation system challenge generation start");
        if (gameData == null) {
            throw new IllegalArgumentException("gameData must be not null");
        }
        List<Player> listofContent = new ArrayList<Player>();
        for (PlayerStateDTO c : gameData) {
            if (cfg.isUserfiltering()) {
                if (cfg.getPlayerIds().contains(c.getPlayerId())) {
                    listofContent.add(c);
                }
            } else {
                listofContent.add(c);
            }
        }
        dbg(logger, "Generating challenges");
        Map<String, List<ChallengeExpandedDTO>> challengeCombinations = rscg
                .generate(listofContent, start, end);

        // Map<String, List<ChallengeExpandedDTO>> evaluatedChallenges = rscv.valuate(challengeCombinations, listofContent);
        Map<String, List<ChallengeExpandedDTO>> evaluatedChallenges = null;


        // build a leaderboard, for now is the current, to be parameterized for
        // weekly or general leaderboard
        List<LeaderboardPosition> leaderboard = buildLeaderBoard(listofContent);
        Collections.sort(leaderboard);
        int index = 0;
        for (LeaderboardPosition pos : leaderboard) {
            pos.setIndex(index);
            index++;
        }

        // rscf
        dbg(logger, "Filtering challenges");
        Map<String, List<ChallengeExpandedDTO>> filteredChallenges = rscf
                .filterAndSort(evaluatedChallenges, leaderboard);

        filteredChallenges = rscf.removeDuplicates(filteredChallenges);

        Map<String, Integer> count = new HashMap<String, Integer>();

        // select challenges and avoid duplicate mode
        for (String key : filteredChallenges.keySet()) {
            // upload and assign challenge
            if (count.get(key) == null) {
                count.put(key, 0);
            }
            if (toWriteChallenge.get(key) == null) {
                toWriteChallenge.put(key, new ArrayList<ChallengeExpandedDTO>());
            }

            // filter used modes
            List<String> usedModes = new ArrayList<String>();

            for (ChallengeExpandedDTO dto : filteredChallenges.get(key)) {

                if (cfg.isSelecttoptwo()) {
                    if (count.get(key) < 2) {
                        String counter = (String) dto.getData(
                                "counterName");
                        if (counter != null && !usedModes.contains(counter)) {
                            usedModes.add(counter);
                            count.put(key, count.get(key) + 1);
                            toWriteChallenge.get(key).add(dto);
                        }
                    } else {
                        break;
                    }
                } else {
                    toWriteChallenge.get(key).add(dto);
                }
            }
        }
        logger.info("Generated challenges " + toWriteChallenge.size());
        return to WriteChallenge;
    } */

    /**
     * Build game leaderboard using players green leaves's points
     *
     * @param gameData
     * @return
     */
    /*
    private List<LeaderboardPosition> buildLeaderBoard(List<Player> gameData) {
        List<LeaderboardPosition> result = new ArrayList<LeaderboardPosition>();
        for (PlayerStateDTO content : gameData) {
            for (PointConcept pc : content.getState().getPointConcept()) {
                if (pc.getName().equals("green leaves")) {
                    Integer score = (int) Math.round(pc
                            .getPeriodCurrentScore("weekly"));
                    result.add(new LeaderboardPosition(score, content
                            .getPlayerId()));
                }
            }
        }
        return result;
    } */

    /**
     * Write challenges to xlsx (Excel) file and configuration as json file
     *
     */
    /*
    public void writeToFile(Map<String, List<ChallengeExpandedDTO>> toWriteChallenge)
            throws FileNotFoundException, IOException {
        if (toWriteChallenge == null) {
            throw new IllegalArgumentException(
                    "Impossible to write null or empty challenges");
        }
        // Get the workbook instance for XLS file
        Workbook workbook = new XSSFWorkbook();

        // Get first sheet from the workbook
        Sheet sheet = workbook.createSheet();
        String[] labels = {"PLAYER_ID", "CHALLENGE_TYPE_NAME",
                "CHALLENGE_NAME", "MODE", "MODE_WEIGHT", "DIFFICULTY", "WI",
                "BONUS_SCORE", "BASELINE", "TARGET", "PERCENTAGE"};

        Row header = sheet.createRow(0);
        int i = 0;
        for (String label : labels) {
            header.createCell(i).setCellValue(label);
            i++;
        }
        int rowIndex = 1;

        for (String key : toWriteChallenge.keySet()) {
            for (ChallengeExpandedDTO dto : toWriteChallenge.get(key)) {
                Row row = sheet.createRow(rowIndex);
                sheet = ExcelUtil.buildRow(cfg, sheet, row, key, dto);
                rowIndex++;
            }
        }

        workbook.write(new FileOutputStream(new File(
                "reportGeneratedChallenges.xlsx")));
        workbook.close();
        logger.info("written reportGeneratedChallenges.xlsx");

        // print configuration
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        FileOutputStream oout;

        try {
            oout = new FileOutputStream(new File(
                    "recommendationSystemConfiguration.json"));
            IOUtils.write(mapper.writeValueAsString(cfg), oout);

            oout.flush();
            logger.info("written recommendationSystemConfiguration.json");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    } */

    public RecommendationSystemStatistics getStats() {
        return stats;
    }

    public Double getWeeklyContentMode(PlayerStateDTO cnt, String mode, DateTime execDate) {
        return getContentMode(cnt, "weekly", mode, execDate);
    }


    public Double getDailyContentMode(PlayerStateDTO cnt, String mode, DateTime execDate) {
        return getContentMode(cnt, "daily", mode, execDate);
    }

    public Double getContentMode(PlayerStateDTO state, String period, String mode, DateTime execDate) {
        for (GameConcept gc : state.getState().get("PointConcept")) {
            PointConcept pc = (PointConcept) gc;

            String m = pc.getName();
            if (!m.equals(mode))
                continue;

            return getPeriodScore(pc,period, execDate);
        }

        return 0.0;
    }

    public static String fixMode(String mode) {
        return mode.replace(" ", "_").toLowerCase();
    }

    public void preprocess(Set<String> playerIds) {
        // pre-process players, assigning control / treatment

        /*
        for (String pId: playerIds) {
            PlayerStateDTO state = facade.getPlayerState(gameId, pId);
            int lvl = getLevel(state);

            if (lvl == 0) {
                Map<String, Object> cs = new HashMap<String, Object>();
                facade.setCustomDataPlayer(gameId, pId, cs);
                continue;
            }

            Map<String, Object> cs = facade.getCustomDataPlayer(gameId, pId);
            String exp = (String) cs.get("exp");
            if (exp == null) continue;

            cs.put("exp-start", this.getChallengeWeek(execDate));
            facade.setCustomDataPlayer(gameId, pId, cs);
        }*/

        // System.out.println("Working Directory = " + System.getProperty("user.dir"));
        Map<String, Integer> daysPlayed = new HashMap<>();
        FirstActionAnalyzer firstActionAnalyzer =
                new OfflineFirstActionAnalyzer("firstGameActions.csv");
        // final String ELASTIC_URL = "";
        // final String ELASTIC_USER = "";
        // final String ELASTIC_PWD = "";
        // final String GAMEID = "";
        // FirstActionAnalyzer firstActionAnalyzer =
        // new OnlineFirstActionAnalyzer(ELASTIC_URL, ELASTIC_USER, ELASTIC_PWD, GAMEID);
        for (String playerId : playerIds) {
            Optional<DateTime> dt = firstActionAnalyzer.firstActionDate(playerId);
            dt.ifPresent(date -> {
                int am = Days.daysBetween(date, execDate).getDays();

                if (am > 0 && am < 100)
                    daysPlayed.put(playerId, am);
            });
        }
        Map<String, Double> toEvaluate = new HashMap<>();

        int i = 0;

        // int w = this.getChallengeWeek(execDate);
/*
        for (String pId: playerIds) {
            Map<String, Object> cs = facade.getCustomDataPlayer(gameId, pId);

            if (cs == null)
                cs = new HashMap<String, Object>();

            String exp = (String) cs.get("exp");

            if (exp != null) {

                int dw =  w - (Integer) cs.get("exp-start");
                if (dw >= 3 && ! exp.equals("treatment")) {
                    cs.put("exp", "treatment");
                    facade.setCustomDataPlayer(gameId, pId, cs);
                }

                continue;
            }

            PlayerStateDTO state = facade.getPlayerState(gameId, pId);

            int lvl = getLevel(state);
            if (lvl <= 1) continue;

            double green_leaves = -1;
            for (GameConcept gc: state.getState().get("PointConcept")) {
                PointConcept pt = (PointConcept) gc;
                if ("green leaves".equals(pt.getName()))
                    green_leaves = pt.getScore();
            }

            int played = 20;
            if (daysPlayed.containsKey(pId))
                played = daysPlayed.get(pId);

            toEvaluate.put(pId, green_leaves / played);

           //  if (i > 10)
           //     break;
           //  i++;

        }

        boolean control = new Random().nextBoolean();

        for (String pId: sortByValuesList(toEvaluate)) {

            Map<String, Object> cs = facade.getCustomDataPlayer(gameId, pId);

            String exp = "treatment";
            if (control) exp = "control";
            control = !control;

            cs.put("exp");
            cs.put("exp-start", this.getChallengeWeek(execDate));

            facade.setCustomDataPlayer(gameId, pId, cs);
        }

        p("done");
        *
 */
    }

    private interface FirstActionAnalyzer {
        Optional<DateTime> firstActionDate(String playerId);
    }

    private class OfflineFirstActionAnalyzer implements FirstActionAnalyzer {
        private Map<String, DateTime> actionsDates = new HashMap<>();

        public OfflineFirstActionAnalyzer(String dataFilePath) {
            try {
                BufferedReader csvReader = new BufferedReader(new FileReader(dataFilePath));
                String row;
                while ((row = csvReader.readLine()) != null) {
                    String[] data = row.split(",");

                    DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss");
                    DateTime dt = formatter.parseDateTime(data[1]);
                    actionsDates.put(data[0], dt);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Optional<DateTime> firstActionDate(String playerId) {
            return Optional.ofNullable(actionsDates.get(playerId));
        }
    }


    private class OnlineFirstActionAnalyzer implements FirstActionAnalyzer {
        final private Client client;
        final private static String serviceUrlTemplate =
                "%s/gamification-stats-%s-*/_search?size=1";
        final private static String bodyTemplate =
                "" + "{\"sort\" : [" + "{ \"executionTime\" : {\"order\" : \"asc\"} }" + "],"
                        + "\"query\": {" + "\"bool\": {" + "\"must\" : ["
                        + "{ \"match\" : {\"actionName\" : \"save_itinerary\"} },"
                        + "{ \"bool\" : {\"should\" : [{ \"match\" : {\"actionName\" : \"save_itinerary\"} }, { \"match\" : {\"actionName\" : \"start_survey_complete\"} } ] } },"
                        + "{ \"match\" : {\"playerId\" : \"%s\"} } " + "]" + "}" + "}" + "}";

        final private String serviceUrl;

        final private Pattern executionTimePattern;

        public OnlineFirstActionAnalyzer(String serviceUrl, String username, String password,
                String gameId) {
            HttpAuthenticationFeature authentication =
                    HttpAuthenticationFeature.basic(username, password);
            client = ClientBuilder.newClient();
            client.register(authentication);
            this.serviceUrl = String.format(serviceUrlTemplate, serviceUrl, gameId);
            executionTimePattern = Pattern.compile("\"executionTime\":\"(\\d+)\"");
        }

        @Override
        public Optional<DateTime> firstActionDate(String playerId) {
            final String body = String.format(bodyTemplate, playerId);

            Response response = client.target(serviceUrl).request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(body));

            if (response.getStatus() == 200) {
                return extractExecutionTime(response.readEntity(String.class));
            } else {
                return Optional.empty();
            }
        }

        private Optional<DateTime> extractExecutionTime(String response) {
            Matcher matchers = executionTimePattern.matcher(response);
            if (matchers.find()) {
                return Optional.of(new DateTime(Long.valueOf(matchers.group(1))));
            } else {
                return Optional.empty();
            }
        }
    }


}
