package eu.fbk.das.rs.challenges.generation;

import static eu.fbk.das.GamificationEngineRestFacade.jodaToOffset;
import static eu.fbk.das.rs.challenges.ChallengeUtil.getLevel;
import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.rs.utils.Utils.daysApart;
import static eu.fbk.das.rs.utils.Utils.dbg;
import static eu.fbk.das.rs.utils.Utils.p;
import static eu.fbk.das.rs.utils.Utils.parseDate;
import static eu.fbk.das.rs.utils.Utils.sortByValuesList;
import static it.smartcommunitylab.model.ChallengeConcept.StateEnum.COMPLETED;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.utils.Utils;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import it.smartcommunitylab.model.ChallengeConcept;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;

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
    
    private Set<String> modelTypes;

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
    public List<ChallengeExpandedDTO> recommend(String pId, Set<String> modelTypes,  Map<String, String> creationRules, Map<String, Object> challengeValues) {

        int chaWeek = (Integer) challengeValues.get("challengeWeek");
        Date execDateParam = (Date) challengeValues.get("exec");
        DateTime execDate = new DateTime(execDateParam.getTime());
        stats.checkAndUpdateStats(execDate);
        rscv.prepare(stats);
        rscg.prepare(chaWeek);
        
        this.modelTypes = modelTypes;

        PlayerStateDTO state = facade.getPlayerState(gameId, pId);
        int lvl = getLevel(state);

        // TODO check, serve ancora?
        String exp = getPlayerExperiment(pId);

        // OLD method
        // List<ChallengeExpandedDTO> cha = generation2019(pId, state, d, lvl, exp);

       List<ChallengeExpandedDTO> cha = generationRule(pId, state, execDate, lvl, creationRules, exp);

        for (ChallengeExpandedDTO c: cha) {
            c.setInfo("playerLevel", lvl);
            c.setInfo("player", pId);
            c.setInfo("experiment", exp);

            c.setHide(true);
        }

        return cha;

    }

    private List<ChallengeExpandedDTO> generationRule(String pId, PlayerStateDTO state, DateTime d, int lvl, Map<String, String> creationRules, String exp) {
        String rule = creationRules.get(String.valueOf(lvl));
        if (rule == null) rule = creationRules.get("other");

        if ("empty".equals(rule))
            return new ArrayList<>();

        List<ChallengeExpandedDTO> s = new ArrayList<>();
        ChallengeExpandedDTO g = rscg.getRepetitive(pId);
        s.add(g);

        if ("fixedOne".equals(rule))
            s.addAll(getAssigned(state, d, 1, exp));
        else if ("choiceTwo".equals(rule))
            s.addAll(assignLimit(2, state, d, exp));
        else if ("choiceThree".equals(rule))
            s.addAll(assignLimit(3, state, d, exp));
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

    public void assignRecommendFriend(String pId, List<ChallengeExpandedDTO> s) {

        String l = "first_recommend";

        if (existsAssignedChallenge(pId, l)) return;

        ChallengeExpandedDTO cha = rscg.prepareChallange(l, "Recommendations");
        cha.setStart(jodaToOffset(new DateTime()));
        cha.setModelName("absoluteIncrement");
        cha.setData("target", 1.0);
        cha.setData("bonusScore", 200.0);
        s.add(cha);
    }

    private boolean existsAssignedChallenge(String pId, String l) {
        List<ChallengeConcept> currentChallenges = facade.getChallengesPlayer(cfg.get("GAME_ID"), pId);
        for (ChallengeConcept cha: currentChallenges) {
            if (cha.getName().contains(l))
                return true;
        }
        return  false;
    }

    private boolean existsAssignedSurvey(String pId, String l) {
        List<ChallengeConcept> currentChallenges = facade.getChallengesPlayer(cfg.get("GAME_ID"), pId);
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
        List<ChallengeConcept> currentChallenges = facade.getChallengesPlayer(cfg.get("GAME_ID"), pId);
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
        Map<String, Object> cs = facade.getCustomDataPlayer(cfg.get("GAME_ID"), pId);
        return (String) cs.get("exp");
    }

    private void assignSurveyEvaluation(String pId, List<ChallengeExpandedDTO> s) {

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

        ChallengeExpandedDTO cha = rscg.prepareChallange("survey_" + l);

        DateTime dt = new DateTime();

        cha.setStart(jodaToOffset(dt));

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

        ChallengeExpandedDTO cha = rscg.prepareChallange("survey_" + l);

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
        List<ChallengeExpandedDTO> list = recommendAll(state, d, exp);
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
        return assignLimit(limit, state, d, "treatment");
    }

    protected List<ChallengeExpandedDTO> assignLimit(int limit, PlayerStateDTO state, DateTime d, String exp) {

        List<ChallengeExpandedDTO> list = recommendAll(state, d, exp);
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

        for (int i = 0; i < limit -1; i++) {
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

    public List<ChallengeExpandedDTO> recommendAll(PlayerStateDTO state, DateTime d) {
        return recommendAll(state, d, "treatment");
    }

    public List<ChallengeExpandedDTO> recommendAll(PlayerStateDTO state, DateTime d, String exp) {

        List<ChallengeExpandedDTO> challanges = new ArrayList<>();
        for (String mode : modelTypes) {
            List<ChallengeExpandedDTO> l_cha = rscg.generate(state, mode, d, exp);

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
            PlayerStateDTO state = facade.getPlayerState(cfg.get("GAME_ID"), pId);
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

            PlayerStateDTO state = facade.getPlayerState(cfg.get("GAME_ID"), pId);

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
