package eu.fbk.das.rs.challenges.generation;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.fbk.das.rs.utils.Utils;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.*;
import eu.trentorise.game.challenges.util.ExcelUtil;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.util.*;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getLevel;
import static eu.fbk.das.rs.utils.Utils.*;

/**
 * Recommandation System main class, requires running Gamification Engine in
 * order to run
 */
public class RecommendationSystem {

    private static final Logger logger = LogManager.getLogger(RecommendationSystem.class);

    public GamificationEngineRestFacade facade;
    public RecommendationSystemConfig cfg;
    public String gameId;
    public String host;

    public RecommendationSystemChallengeGeneration rscg;
    public RecommendationSystemChallengeValuator rscv;
    public RecommendationSystemChallengeFilteringAndSorting rscf;
    private RecommendationSystemStatistics stats;

    public RecommendationSystem(String host, String user, String pass, String gameId) {
        this.host = host;
        this.gameId = gameId;

        facade = new GamificationEngineRestFacade(host, user, pass);

        rscv = new RecommendationSystemChallengeValuator();
        rscg = new RecommendationSystemChallengeGeneration(this);
        rscf = new RecommendationSystemChallengeFilteringAndSorting();
        stats = new RecommendationSystemStatistics(this);
        dbg(logger, "Recommendation System init complete");
    }

    public RecommendationSystem(RecommendationSystemConfig cfg) {
        this(cfg.get("HOST"), cfg.get("USERNAME"), cfg.get("PASSWORD"), cfg.get("GAME_ID"));
    }

    public RecommendationSystem() {
        this(new RecommendationSystemConfig());
    }

    // generate challenges
    public List<ChallengeDataDTO> recommend(String pId, DateTime d) {

        Player state = facade.getPlayerState(gameId, pId);

        int lvl = getLevel(state);

        rscg.prepare(d);

        String exp = getPlayerExperiment(pId);

        List<ChallengeDataDTO> cha = generation2019(pId, state, d, lvl, exp);

        // If level high enough, additionally choose to perform experiment
        /*
        if (Utils.randChance(0.2))
            for (ChallengeDataDTO c: cha) {
                c.setInfo("experiment", "tgt");
            }
            */

        //List<ChallengeDataDTO> cha = assignLimit(3, state, d);

        // cha = assignLimit(3, state, d);

        // return assignOne(state, d);

        // return recommendForecast(state, d);

        // return recommendAll(state, d);

        for (ChallengeDataDTO c: cha) {
            c.setInfo("playerLevel", lvl);
            c.setInfo("player", pId);
            c.setInfo("experiment", exp);

            c.setHide(true);
        }

        return cha;

    }


    private List<ChallengeDataDTO> generation2019(String pId, Player state, DateTime d, int lvl, String exp) {

        // if level is 0, none
        if (lvl == 0)
            return new ArrayList<>();

        List<ChallengeDataDTO> s = new ArrayList<>();
        assignSurveyPrediction(pId, s);
        assignSurveyEvaluation(pId, s);
        assignRecommendFriend(pId, s);

        ChallengeDataDTO g = rscg.getRepetitive(pId);

        // if level is 1, assign two fixed
        if (lvl == 1) {
            s.add(g);
            s.addAll(getAssigned(state, d, 1, exp));
            return s;
        }

        // if level is 2, assign 1 repetitive and two choices
        if (lvl == 2) {
            s.add(g);
            s.addAll(assignLimit(2, state, d, exp));
            return s;
        }

        // if level is 3, assign 1 repetitive and three choices
        if (lvl == 3) {
            s.add(g);
            s.addAll(assignLimit(3, state, d, exp));
            return s;
        }

        s.addAll(assignLimit(3, state, d, exp));
        return s;

    }

    public void assignRecommendFriend(String pId, List<ChallengeDataDTO> s) {

        String l = "first_recommend";

        if (existsAssignedChallenge(pId, l)) return;

        ChallengeDataDTO cha = rscg.prepareChallange(l, "Recommendations");
        cha.setStart(new DateTime());
        cha.setModelName("absoluteIncrement");
        cha.setData("target", 1.0);
        cha.setData("bonusScore", 200.0);
        s.add(cha);
    }

    private boolean existsAssignedChallenge(String pId, String l) {
        List<LinkedHashMap<String, Object>> currentChallenges = facade.getChallengesPlayer(cfg.get("GAME_ID"), pId);
        for (LinkedHashMap<String, Object> cha: currentChallenges) {
            if (((String) cha.get("name")).contains(l))
                return true;
        }
        return  false;
    }

    private boolean existsAssignedSurvey(String pId, String l) {
        List<LinkedHashMap<String, Object>> currentChallenges = facade.getChallengesPlayer(cfg.get("GAME_ID"), pId);
        for (LinkedHashMap<String, Object> cha: currentChallenges) {
            if (!("survey".equals(cha.get("modelName"))))
                continue;

            Map<String, Object> f = (Map<String, Object>) cha.get("fields");
            String sv = (String) f.get("surveyType");

            if (l.equals(sv))
                return true;
        }
        return  false;
    }


    private boolean existsAssignedAnCompletedSurvey(String pId, String l) {
        List<LinkedHashMap<String, Object>> currentChallenges = facade.getChallengesPlayer(cfg.get("GAME_ID"), pId);
        for (LinkedHashMap<String, Object> cha: currentChallenges) {
            if (!("survey".equals(cha.get("modelName"))))
                continue;

            Map<String, Object> f = (Map<String, Object>) cha.get("fields");
            String sv = (String) f.get("surveyType");

            if (!l.equals(sv))
                continue;

            String c = (String) cha.get("state");

            if (!"COMPLETED".equals(c))
                continue;

            return true;
        }
        return  false;
    }

    private String getPlayerExperiment(String pId) {
        Map<String, Object> cs = facade.getCustomDataPlayer(cfg.get("GAME_ID"), pId);
        return (String) cs.get("exp");
    }

    private void assignSurveyEvaluation(String pId, List<ChallengeDataDTO> s) {

        String l = "evaluation";

        Map<String, Object> cs = facade.getCustomDataPlayer(cfg.get("GAME_ID"), pId);

        int w = this.getChallengeWeek(new DateTime());

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

        ChallengeDataDTO cha = rscg.prepareChallange("survey_" + l);

        DateTime dt = new DateTime();

        cha.setStart(dt);

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

    private void assignSurveyPrediction(String pId, List<ChallengeDataDTO> s) {

        String l = "prediction";

        if (existsAssignedSurvey(pId, l)) return;

        // ADD NEW CHALLENGE SURVEY PREDICTION HERE

        ChallengeDataDTO cha = rscg.prepareChallange("survey_" + l);

        DateTime dt = new DateTime();

        cha.setStart(dt);

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


    private List<ChallengeDataDTO> firstWeeks(Player state, DateTime d, int lvl) {

        if (lvl <= 0)
            return new ArrayList<>();
        else
            // assign only one
            return getAssigned(state, d, 1);
    }

    private List<ChallengeDataDTO> oldWeeks(Player state, DateTime d, int lvl) {

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
            List<ChallengeDataDTO> res = assignForecast(state, d);
            if (res != null && res.size() == 3)
                return  res;
        }

            return  assignLimit(3, state, d);
    }

    // cerca di prevedere obiettivo, fornisce stime diverse di punteggio
    protected List<ChallengeDataDTO> assignForecast(Player state, DateTime execDate) {

        String max_mode = null;
        int max_pos = -1;
        Double max_value = 0.0;

        for (String mode : ChallengesConfig.defaultMode) {
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

            List<ChallengeDataDTO> l_cha = rscg.forecast(state, max_mode, execDate);

            int ix = 0;
            for(ChallengeDataDTO cha: l_cha) {

                if (cha == null)
                    p("ciao");

                cha.setOrigin("rs");
                if (ix == 0)
                    cha.setPriority("2");
                else
                    cha.setPriority("1");

                cha.setInfo("id", ix);
                // cha.setInfo("experiment", "tgt");
                cha.setState("proposed");

                ix++;
            }

        List<ChallengeDataDTO> new_l_cha = rscf.filter(l_cha, state, execDate);

        return new_l_cha;

    }

    private List<ChallengeDataDTO> getAssigned(Player state, DateTime d, int num) {
        return getAssigned(state, d, num, "treatment");
    }

    private List<ChallengeDataDTO> getAssigned(Player state, DateTime d, int num, String exp) {
        List<ChallengeDataDTO> list = recommendAll(state, d, exp);
        if (list == null || list.isEmpty())
            return null;

        ArrayList<ChallengeDataDTO> res = new ArrayList<ChallengeDataDTO>();

        for (int ix = 0; ix < num && ix < list.size(); ix ++) {

            ChallengeDataDTO chosen = list.get(ix);

            chosen.setState("assigned");
            chosen.setOrigin("rs");
            chosen.setInfo("id", ix);

            res.add(chosen);
        }

        return res;

    }

    protected List<ChallengeDataDTO> assignLimit(int limit, Player state, DateTime d) {
        return assignLimit(limit, state, d, "treatment");
    }

    protected List<ChallengeDataDTO> assignLimit(int limit, Player state, DateTime d, String exp) {

        List<ChallengeDataDTO> list = recommendAll(state, d, exp);
        if (list == null || list.isEmpty())
            return null;

        Set<String> modes = new HashSet<>();

        ArrayList<ChallengeDataDTO> res = new ArrayList<>();
        ChallengeDataDTO cha = list.get(0);
        cha.setPriority("2");
        res.add(cha);
        String counter = (String) cha.getData().get("counterName");
        modes.add(counter);
        cha.setInfo("id", 0);

        int ix = 1;

        for (int i = 0; i < limit -1; i++) {
            boolean found = false;
            while (!found) {

                cha = list.get(ix++);
                counter = (String) cha.getData().get("counterName");
                if (modes.contains(counter))
                    continue;
                modes.add(counter);
                cha.setPriority("1");
                cha.setInfo("id", i+1);
                res.add(cha);
                found = true;
            }
        }

        for (ChallengeDataDTO cdd: res) {
            // cdd.setInfo("experiment", "cho");
            cdd.setOrigin("rs");
            cdd.setState("proposed");
        }

        return res;

    }

    public List<ChallengeDataDTO> recommendAll(Player state, DateTime d) {
        return recommendAll(state, d, "treatment");
    }

    public List<ChallengeDataDTO> recommendAll(Player state, DateTime d, String exp) {

        List<ChallengeDataDTO> challanges = new ArrayList<>();
        for (String mode : ChallengesConfig.defaultMode) {
            List<ChallengeDataDTO> l_cha = rscg.generate(state, mode, d, exp);

            if (l_cha.isEmpty())
                continue;

            challanges.addAll(l_cha);

        }

        return rscf.filter(challanges, state, d);
    }

    public int getChallengeWeek(DateTime d) {
        int s = getChallengeDay(d);
        return (s/7) +1;
    }

    public int getChallengeDay(DateTime d) {
        return daysApart(d, parseDate("29/10/2018"));
    }

    /*
    public Map<String, List<ChallengeDataDTO>> recommendation(
            List<Player> gameData, DateTime start, DateTime end)
            throws NullPointerException {

        logger.info("Recommendation system challenge generation start");
        if (gameData == null) {
            throw new IllegalArgumentException("gameData must be not null");
        }
        List<Player> listofContent = new ArrayList<Player>();
        for (Player c : gameData) {
            if (cfg.isUserfiltering()) {
                if (cfg.getPlayerIds().contains(c.getPlayerId())) {
                    listofContent.add(c);
                }
            } else {
                listofContent.add(c);
            }
        }
        dbg(logger, "Generating challenges");
        Map<String, List<ChallengeDataDTO>> challengeCombinations = rscg
                .generate(listofContent, start, end);

        // Map<String, List<ChallengeDataDTO>> evaluatedChallenges = rscv.valuate(challengeCombinations, listofContent);
        Map<String, List<ChallengeDataDTO>> evaluatedChallenges = null;


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
        Map<String, List<ChallengeDataDTO>> filteredChallenges = rscf
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
                toWriteChallenge.put(key, new ArrayList<ChallengeDataDTO>());
            }

            // filter used modes
            List<String> usedModes = new ArrayList<String>();

            for (ChallengeDataDTO dto : filteredChallenges.get(key)) {

                if (cfg.isSelecttoptwo()) {
                    if (count.get(key) < 2) {
                        String counter = (String) dto.getData().get(
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
        for (Player content : gameData) {
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
     * @param toWriteChallenge
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void writeToFile(Map<String, List<ChallengeDataDTO>> toWriteChallenge)
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
            for (ChallengeDataDTO dto : toWriteChallenge.get(key)) {
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
    }

    /* fac_body è per facade di testing non ancora passati in produzione */
    public void prepare(GamificationEngineRestFacade facade,  DateTime date, String host) {
        this.host = host;
        this.facade = facade;

        stats.checkAndUpdateStats(date);
        rscg.prepare(date);
        rscv.prepare(stats);
    }

    public void prepare(GamificationEngineRestFacade facade, DateTime date) {
        prepare(facade, date, "test");
    }

    public RecommendationSystemStatistics getStats() {
        return stats;
    }

    public Double getWeeklyContentMode(Player cnt, String mode, DateTime execDate) {
        return getContentMode(cnt, "weekly", mode, execDate);
    }


    public Double getDailyContentMode(Player cnt, String mode, DateTime execDate) {
        return getContentMode(cnt, "daily", mode, execDate);
    }

    public Double getContentMode(Player cnt, String period, String mode, DateTime execDate) {
        for (PointConcept pc : cnt.getState().getPointConcept()) {

            String m = pc.getName();
            if (!m.equals(mode))
                continue;

            return pc.getPeriodScore(period, execDate);
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
            Player state = facade.getPlayerState(cfg.get("GAME_ID"), pId);
            int lvl = getLevel(state);

            if (lvl == 0) {
                Map<String, Object> cs = new HashMap<String, Object>();
                facade.setCustomDataPlayer(cfg.get("GAME_ID"), pId, cs);
                continue;
            }

            Map<String, Object> cs = facade.getCustomDataPlayer(cfg.get("GAME_ID"), pId);
            String exp = (String) cs.get("exp");
            if (exp == null) continue;

            cs.put("exp-start", this.getChallengeWeek(new DateTime()));
            facade.setCustomDataPlayer(cfg.get("GAME_ID"), pId, cs);
        }*/

        BufferedReader csvReader = null;
        // System.out.println("Working Directory = " + System.getProperty("user.dir"));
        Map<String, Integer> daysPlayed = new HashMap<>();
        try {
            csvReader = new BufferedReader(new FileReader("firstGameActions.csv"));
            String row;
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",");

                DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss");
                DateTime dt = formatter.parseDateTime(data[1]);

                int am = Days.daysBetween(dt, new DateTime()).getDays();

                if (am > 0 && am < 100)
                    daysPlayed.put(data[0], am);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Map<String, Double> toEvaluate = new HashMap<>();

        int i = 0;

        int w = this.getChallengeWeek(new DateTime());

        for (String pId: playerIds) {
            Map<String, Object> cs = facade.getCustomDataPlayer(cfg.get("GAME_ID"), pId);

            if (cs == null)
                cs = new HashMap<String, Object>();

            String exp = (String) cs.get("exp");

            if (exp != null) {

                int dw =  w - (Integer) cs.get("exp-start");
                if (dw >= 3 && ! exp.equals("treatment")) {
                    cs.put("exp", "treatment");
                    facade.setCustomDataPlayer(cfg.get("GAME_ID"), pId, cs);
                }

                continue;
            }

            Player state = facade.getPlayerState(cfg.get("GAME_ID"), pId);

            int lvl = getLevel(state);
            if (lvl <= 1) continue;

            double green_leaves = -1;
            for (PointConcept pt: state.getState().getPointConcept()) {
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

            Map<String, Object> cs = facade.getCustomDataPlayer(cfg.get("GAME_ID"), pId);

            String exp = "treatment";
            if (control) exp = "control";
            control = !control;

            cs.put("exp", exp);
            cs.put("exp-start", this.getChallengeWeek(new DateTime()));

            facade.setCustomDataPlayer(cfg.get("GAME_ID"), pId, cs);
        }

        p("done");
    }

}
