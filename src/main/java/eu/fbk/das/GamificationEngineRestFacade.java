
package eu.fbk.das;

import static eu.fbk.das.utils.Utils.p;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;

import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.utils.Pair;
import it.smartcommunitylab.ApiClient;
import it.smartcommunitylab.ApiException;
import it.smartcommunitylab.basic.api.GameControllerApi;
import it.smartcommunitylab.basic.api.PlayerControllerApi;
import it.smartcommunitylab.model.ChallengeConcept;
import it.smartcommunitylab.model.GameStatistics;
import it.smartcommunitylab.model.PagePlayerStateDTO;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.Projection;
import it.smartcommunitylab.model.RawSearchQuery;
import it.smartcommunitylab.model.WrapperQuery;
import it.smartcommunitylab.model.ext.ChallengeAssignmentDTO;
import it.smartcommunitylab.model.ext.GroupChallengeDTO;
import it.smartcommunitylab.model.ext.GroupChallengeDTO.AttendeeDTO;
import it.smartcommunitylab.model.ext.GroupChallengeDTO.PointConceptDTO;
import it.smartcommunitylab.model.ext.GroupChallengeDTO.RewardDTO;

public class GamificationEngineRestFacade {

    // API: https://dev.smartcommunitylab.it/gamification/swagger-ui.html

    private static final Logger logger = Logger
            .getLogger(GamificationEngineRestFacade.class);

    private final PlayerControllerApi playerApi;
    private final GameControllerApi gameApi;

    private final HashMap<String, Map<String, PlayerStateDTO>> gameStateCache;

    public GamificationEngineRestFacade(final String endpoint, final String username, final String password) {

        ApiClient client = new ApiClient(endpoint);
        client.setUsername(username);
        client.setPassword(password);

        playerApi = new PlayerControllerApi(client);
        gameApi = new GameControllerApi(client);

        gameStateCache = new HashMap<>();
    }

    public PlayerStateDTO getPlayerState(String gameId, String pId) {

        checkGameId(gameId);
        checkPlayerId(pId);

        Map<String, PlayerStateDTO> contentCache = getGameCache(gameId);

        if (!contentCache.containsKey(pId)) {
            try {
                PlayerStateDTO state = playerApi.readPlayerUsingGET(gameId, pId, true, null, null);
                contentCache.put(pId, state);
            } catch (Exception e) {
                apiErr(e);
                return null;
            }
        }

        return contentCache.get(pId);
    }

    private Map<String, PlayerStateDTO> getGameCache(String gameId) {
        if (!gameStateCache.containsKey(gameId))
            gameStateCache.put(gameId, new HashMap<>());

        return gameStateCache.get(gameId);
    }

    private void checkPlayerId(String pId) {
        if (pId == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
    }

    private void checkGameId(String gameId) {
        if (gameId == null || "".equals(gameId)) {
            throw new IllegalArgumentException("gameId cannot be null");
        }
    }

    /**
     * Gets the list of playerIds in the given game
     *
     * @param gameId id of the game
     * @return list of playerIds
     */
    public Set<String> getGamePlayers(String gameId) {
        checkGameId(gameId);

        Set<String> players = new TreeSet<>();
        String size = "100";

        Projection p = new Projection();
        p.addIncludeItem("playerId");
        RawSearchQuery rw = new RawSearchQuery();
        rw.setProjection(p);
        WrapperQuery q = new WrapperQuery();
        q.setRawQuery(rw);

        try {
            PagePlayerStateDTO result = playerApi.searchByQueryUsingPOST(gameId,  q,"1", size);
            int totPages = result.getTotalPages();
            addAllPlayers(players, result.getContent());
            if(totPages > 1) {
	            for (int i = 2; i <= totPages; i++) {
	                result = playerApi.searchByQueryUsingPOST(gameId, q, Integer.toString(i), size);
	                addAllPlayers(players, result.getContent());
	            }
            }
        } catch (Exception e) {
            apiErr(e);
            return null;
        }

        return players;
    }

    private void addAllPlayers(Set<String> players, List<PlayerStateDTO> content) {
        // Map<String, PlayerStateDTO> contentCache = getGameCache(gameId);

        for (PlayerStateDTO st: content) {
            String playerId = st.getPlayerId();
            players.add(playerId);
           //  contentCache.put(playerId, st);
        }
    }

    private void apiErr(Exception e) {
        p("ERRORE NELL'ESECUZIONE DI UNA API");
        logger.error(e);
    }

    private void apiErr(ApiException e) {
        p("ERRORE NELL'ESECUZIONE DI UNA API");
        logger.error(e.getResponseBody());
    }

    public boolean assignChallengeToPlayer(ChallengeAssignmentDTO cdd, String gameId, String playerId) {
        if (cdd == null || gameId == null || playerId == null) {
            throw new IllegalArgumentException("challenge, gameId and playerId cannot be null");
        }

        try {
            playerApi.assignChallengeUsingPOST(cdd, gameId, playerId);
        } catch (ApiException e) {
            apiErr(e);
            return false;
        }

        return true;
    }

    public boolean assignGroupChallenge(GroupChallengeDTO cdd, String gameId) {
        if (cdd == null || gameId == null) {
            throw new IllegalArgumentException("challenge, gameId and playerId cannot be null");
        }

        try {
            playerApi.assignGroupChallengeUsingPOST(cdd, gameId);
        } catch (ApiException e) {
            apiErr(e);
            return false;
        }

        return true;
    }

    public List<ChallengeConcept> getChallengesPlayer(String gameId, String playerId) {
        if (gameId == null || playerId == null) {
            throw new IllegalArgumentException("challenge, gameId and playerId cannot be null");
        }

        try {
            return playerApi.getPlayerChallengeUsingGET(gameId, playerId);
        } catch (ApiException e) {
            apiErr(e);
            return null;
        }
    }

    public List<GameStatistics> readGameStatistics(String gameId, DateTime timestamp, String pcName) {
        checkGameId(gameId);
        List<GameStatistics> res;

        try {
            res = gameApi.readGameStatisticsUsingGET(gameId, pcName, "weekly",
                    timestamp.getMillis(), null, 1, 100); // take first 100 stats
        } catch (ApiException e) {
            apiErr(e);
            return null;
        }

        return res;
    }

    public List<GameStatistics> readGameStatistics(String gameId, DateTime timestamp) {
        return readGameStatistics(gameId, timestamp, "");
    }
    
    public GroupExpandedDTO makeGroupChallengeDTO(String gameId, String mode, String counter,
            String pId1, String pId2, DateTime start, DateTime end, Map<String, Double> res) {

        GroupExpandedDTO gcd = new GroupExpandedDTO();
        gcd.setChallengeModelName(mode);
        gcd.setGameId(gameId);
        gcd.setAttendees(new ArrayList<GroupChallengeDTO.AttendeeDTO>());
        AttendeeDTO a1 = new AttendeeDTO();
        a1.setPlayerId(pId1);
        a1.setRole("GUEST");

        gcd.getAttendees().add(a1);

        AttendeeDTO a2 = new AttendeeDTO();
        a2.setPlayerId(pId2);
        a2.setRole("GUEST");
        gcd.getAttendees().add(a2);

        gcd.setChallengeTarget(Math.ceil(res.get("target")));

        PointConceptDTO cpc = new PointConceptDTO();
        cpc.setName(counter);
        cpc.setPeriod("weekly");
        gcd.setChallengePointConcept(cpc);

        RewardDTO r = new RewardDTO();
        Map<String, Double> bonusScore = new HashMap<>();
        bonusScore.put(pId1, Math.ceil(res.get("player1_prz")));
        bonusScore.put(pId2, Math.ceil(res.get("player2_prz")));
        r.setBonusScore(bonusScore);
        
        final PointConceptDTO calculationPointConcept = new PointConceptDTO();
        calculationPointConcept.setName(counter);
        // FIXME valid only for play&go
        calculationPointConcept.setPeriod("weekly");
        r.setCalculationPointConcept(calculationPointConcept);
        final PointConceptDTO targetPointConcept = new PointConceptDTO();
        targetPointConcept.setName(counter);
        r.setTargetPointConcept(targetPointConcept);
        
        gcd.setReward(r);

        gcd.setOrigin("gca");
        gcd.setState("ASSIGNED");

        gcd.setStart(start.toDate());
        gcd.setEnd(end.toDate());

        return gcd;
    }

    public static OffsetDateTime jodaToOffset(DateTime dt) {
        long millis = dt.getMillis();
        // java.time.Instant
        Instant instant = Instant.ofEpochMilli(millis);

        // get total offset (joda returns milliseconds, java.time takes seconds)
        int offsetSeconds = dt.getZone().getOffset(millis) / 1000;
        return OffsetDateTime.ofInstant(instant, ZoneOffset.ofTotalSeconds(offsetSeconds));
    }

    public static OffsetDateTime dateToOffset(Date date) {
        String s = formatDateAsUTC(date);
        return OffsetDateTime.parse(s);
    }


    public static String formatDateAsUTC(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        // sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String s = sdf.format(date);
        return addChar(s, ':', 22);
    }

    public static String addChar(String str, char ch, int position) {
        return str.substring(0, position) + ch + str.substring(position);
    }

    public static Pair<Date, Date> getDates(Object start, Object duration) {

        Date startDate = null;
        Date endDate = null;

        if (start == null || duration == null) {
            p("NULL START / DURATION!");
            return new Pair<>(startDate, endDate);
        }

        try {
            startDate =
                    ISODateTimeFormat.dateTimeNoMillis().parseLocalDateTime((String) start)
                            .toDate();

            String periodAsIsoFormat = "P" + ((String) duration).toUpperCase();
            java.time.Period p = java.time.Period.parse(periodAsIsoFormat);
            java.time.LocalDateTime endDateTime =
                    new Timestamp(startDate.getTime()).toLocalDateTime().plus(p);
            endDate = Date.from(endDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());

        } catch (Exception e) {
            logger.error(e);
        }

        return new Pair<>(startDate, endDate);
    }

    public Map<String, Object> getCustomDataPlayer(String gameId, String pId) {
        try {
            return playerApi.readCustomDataUsingGET(gameId, pId);
        } catch (ApiException e) {
            apiErr(e);
            return null;
        }
    }

    public void setCustomDataPlayer(String gameId, String pId, Map<String, Object> cs) {
        try {
            playerApi.updateCustomDataUsingPUT1(gameId, pId, cs);
        } catch (ApiException e) {
            apiErr(e);
        }
    }

    public Map<String, PlayerStateDTO> readGameState(String gameId) {
        Set<String> players = getGamePlayers(gameId);
        Map<String, PlayerStateDTO> res = new HashMap<>();
        for (String pId: players) {
            res.put(pId, getPlayerState(gameId, pId));
        }
        return res;
    }

    /*
    public Map<String, Object> getCustomDataPlayer(String gameId, String playerId) {
        if (gameId == null || playerId == null) {
            throw new IllegalArgumentException("challenge, gameId and playerId cannot be null");
        }

        try {
            res = playerApi.read.readGameStatisticsUsingGET(gameId, pcName, "weekly", timestamp.getMillis(), "", -1, -1);
        } catch (ApiException e) {
            apiErr(e);
            return null;
        }

        return res;
    }

    public boolean setCustomDataPlayer(String gameId, String playerId, Map<String, Object> cs) {
        if (gameId == null || playerId == null) {
            throw new IllegalArgumentException("challenge, gameId and playerId cannot be null");
        }
        WebTarget target = getCustomDataPath(gameId, playerId);
        Response response = put(target, cs);
        return response != null;
    }*/

}

