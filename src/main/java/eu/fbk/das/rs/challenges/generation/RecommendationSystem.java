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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static eu.fbk.das.rs.utils.Utils.*;

/**
 * Recommandation System main class, requires running Gamification Engine in
 * order to run
 */
public class RecommendationSystem {

    private static final Logger logger = LogManager.getLogger(RecommendationSystem.class);

    public GamificationEngineRestFacade facade;
    public RecommendationSystemConfig cfg;
    private Map<String, List<ChallengeDataDTO>> toWriteChallenge = new HashMap<String, List<ChallengeDataDTO>>();
    private RecommendationSystemChallengeGeneration rscg;
    private RecommendationSystemChallengeValuator rscv;
    private RecommendationSystemChallengeFilteringAndSorting rscf;
    private RecommendationSystemStatistics stats;

    private String[] configuration = new String[]{"HOST", "GAME_ID", "USERNAME", "PASSWORD", "DATE", "PLAYER_IDS"};

    private int totPlayers;
    private int currentPlayer;
    private GamificationEngineRestFacade copy;

    public RecommendationSystem() {
        this(new RecommendationSystemConfig());
    }

    public RecommendationSystem(RecommendationSystemConfig configuration) {
        this.cfg = configuration;

        rscv = new RecommendationSystemChallengeValuator(configuration);
        rscg = new RecommendationSystemChallengeGeneration(configuration, rscv);
        rscf = new RecommendationSystemChallengeFilteringAndSorting(
                configuration);
        stats = new RecommendationSystemStatistics();
        dbg(logger, "Recommendation System init complete");

    }

    // generate challenges
    public List<ChallengeDataDTO> recommend(String pId, DateTime d) {

        Player state = facade.getPlayerState(cfg.get("GAME_ID"), pId);

        int lvl = getLevel(state);

        List<ChallengeDataDTO> cha = mediumWeeks(state, d, lvl);





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
        }

        return cha;

    }

    private List<ChallengeDataDTO> firstWeeks(Player state, DateTime d, int lvl) {

        if (lvl <= 0)
            return new ArrayList<>();
        else
            // assign only one
            return assignOne(state, d);
    }

    private List<ChallengeDataDTO> mediumWeeks(Player state, DateTime d, int lvl) {

        // if level is 0, none
        if (lvl == 0)
            return new ArrayList<>();

        // if level is 1, assign only one
        if (lvl == 1)
            return   assignOne(state, d);

        // if level is 2, two standard
        if (lvl == 2)
            return assignLimit(2, state, d);

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

        for (String mode : ChallengesConfig.defaultMode) {
            Double modeValue = getWeeklyContentMode(state, mode, execDate);
            int pos = stats.getPosition(mode, modeValue);

            if (modeValue > 0 && pos > max_pos) {
                max_pos = pos;
                max_mode = mode;
            }
        }

        if (max_mode == null)
            return assignLimit(3, state, execDate);

            List<ChallengeDataDTO> l_cha = rscg.forecast(state, max_mode, execDate, this);

            int ix = 0;
            for(ChallengeDataDTO cha: l_cha) {
                cha.setOrigin("rs");
                if (ix == 0)
                    cha.setPriority("2");
                else
                    cha.setPriority("1");

                cha.setInfo("id", ix);
                cha.setInfo("experiment", "tgt");
                cha.setState("proposed");

                ix++;
            }

        List<ChallengeDataDTO> new_l_cha = rscf.filter(l_cha, state, execDate);

        return new_l_cha;

    }

    public int getLevel(Player state) {

        // check the level of the player
        List<PlayerLevel> lvls = state.getLevels();

        for (PlayerLevel lvl: lvls) {
            if (!equal(lvl.getPointConcept(), "green leaves"))
                continue;

            return lvl.getLevelIndex();

            /*

            String s = slug(lvl.getLevelValue());
            for (int i = 0; i < cfg.levelNames.length; i++)
                if (equal(s, slug(cfg.levelNames[i])))
                    return i;


            pf("Could not decode value %s of player level %s \n", lvl.getLevelValue(), lvl);

            return -1;

            */

        }

        pf("Could not find level based on green leaves! %s - Assuming level 0 \n", lvls);

        return 0;
    }

    private List<ChallengeDataDTO> assignOne(Player state, DateTime d) {
        List<ChallengeDataDTO> list = recommendAll(state, d);
        if (list == null || list.isEmpty())
            return null;

        ChallengeDataDTO chosen = list.get(0);

        chosen.setState("assigned");
        chosen.setOrigin("rs");
        chosen.setInfo("id", 1);

        ArrayList<ChallengeDataDTO> res = new ArrayList<ChallengeDataDTO>();
        res.add(chosen);

        return res;

    }

    private List<ChallengeDataDTO> assignTwo(Player state, DateTime d) {

        return assignLimit(2, state, d);
    }

    protected List<ChallengeDataDTO> assignLimit(int limit, Player state, DateTime d) {

        List<ChallengeDataDTO> list = recommendAll(state, d);
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
            cdd.setInfo("experiment", "cho");
            cdd.setOrigin("rs");
            cdd.setState("proposed");
        }

        return res;

    }


    public List<ChallengeDataDTO> recommendAll(Player state, DateTime d) {



        List<ChallengeDataDTO> challanges = new ArrayList<>();
        for (String mode : ChallengesConfig.defaultMode) {
            List<ChallengeDataDTO> l_cha = rscg.generate(state, mode, d, this);

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
    public void prepare(GamificationEngineRestFacade facade,  DateTime date) {
        this.facade = facade;

        stats.checkAndUpdateStats(facade, date, cfg);
        rscg.prepare(stats);
        rscv.prepare(stats);
        rscf.prepare(stats);
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
}
