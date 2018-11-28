
package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.model.ChallengeModel;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.*;

import static eu.fbk.das.rs.Utils.*;

/**
 * A facade for handling logic for Gamification Engine Rest api
 *
 * @see <a
 * href="https://github.com/smartcommunitylab/smartcampus.gamification">Gamification
 * Engine</a>
 */
public class GamificationEngineRestFacade {

    // API: https://dev.smartcommunitylab.it/gamification/swagger-ui.html

    private static final Logger logger = LogManager
            .getLogger(GamificationEngineRestFacade.class);

    private static final String GENGINE = "gengine";
    private static final String STATE = "state";
    private static final String GAME = "game";
    private static final String RULE = "rule";
    private static final String DB = "db";
    private static final String EXECUTE = "execute";
    private static final String PLAYER = "player";
    private static final String CHALLENGE = "challenge";

    // CONTROLLERS
    private static final String DATA = "data/game";
    private static final String MODEL = "model/game";

    // PATHS
    private static final String SEARCH = "player/search";

    private HashMap<String, Map<String, Content>> gameStateCache;

    private WebTarget target;

    protected int verbosity;

    /**
     * Create gamification engine rest facade on selected endpoint
     *
     * @param endpoint
     */
    public GamificationEngineRestFacade(final String endpoint) {
        if (endpoint == null) {
            throw new NullPointerException("Endpoint cannot be null");
        }
        dbg(logger, "GamificationEngineRestFacade created with no authentication");
        target = ClientBuilder.newClient().target(endpoint);

        prepare();
    }

    private void prepare() {
        gameStateCache = new HashMap<String, Map<String, Content>>();
    }

    /**
     * Create gamification engine rest facade on selected endpoint and select
     * http basic authentication credentials
     *
     * @param endpoint
     * @param username
     * @param password
     * @throws NullPointerException if parameters are null
     */
    public GamificationEngineRestFacade(final String endpoint, final String username, final String password)
            throws NullPointerException {

        if (endpoint == null || username == null || password == null) {
            throw new NullPointerException("Endpoint, username and password cannot be null");
        }
        dbg(logger, "GamificationEngineRestFacade created");
        // build endpoint with http basic authentication
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().credentials(username, password).build();
        Client client = ClientBuilder.newClient();
        client.register(feature);
        target = client.target(endpoint);
        dbg(logger, "created endpoint for %s", endpoint);
        prepare();
    }

    private JsonObject decode(Response response) {
        JsonParser parser = new JsonParser();
        return parser.parse(response.readEntity(String.class)).getAsJsonObject();
    }

    private Response get(WebTarget target) {
        Response response = target.request().get();
        return check(response);
    }

    private Response put(WebTarget target, Object json) {
        Response response = target.request().put(Entity.json(json));
        return check(response);
    }

    private Response post(WebTarget target, Object json) {
        Response response = target.request().post(Entity.json(json));
        return check(response);
    }


    private Response check(Response response) {

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            if (verbosity >= 2) dbg(logger, "response code: %s", response.getStatus());
            return response;
        }

        pf("Error in post request %s - %s \n", response.getStatus(), response.getStatusInfo());
        p(response);
        p(response.getEntity());
        return null;
    }


    /**
     * Read game state
     *
     * @param gameId
     * @return a list of {@link Content}, null if error
     */
    public Map<String, Content> readGameState(String gameId) {
        checkGameId(gameId);

        if (!gameStateCache.containsKey(gameId)) {
            WebTarget target = getTarget().path(GENGINE).path(STATE).path(gameId);
            p(target.getUri());
            // for testing pagination: target.queryParam("page",
            // 1).queryParam("size", 1).request().get(Paginator.class);
            Paginator response = target.request().get(Paginator.class);
            if (response == null) {
                err(logger, "error in reading game state");
                return null;
            }
            List<Content> result = new ArrayList<Content>();
            result.addAll(response.getContent());

            if (!response.getLast()) {
                int page = 2;
                boolean end = false;
                Paginator pageResponse;
                while (!end) {
                    pageResponse = target.queryParam("page", page).request()
                            .get(Paginator.class);
                    if (pageResponse != null) {
                        result.addAll(pageResponse.getContent());
                        if (pageResponse.getLast()) {
                            end = true;
                        } else {
                            page++;
                        }
                    }
                }
                dbg(logger, "service return " + page + " pages of result");
            }

            Map<String, Content> contentCache = new HashMap<>();

            for (Content cnt: result) {
                contentCache.put(cnt.getPlayerId(), cnt);
            }

            gameStateCache.put(gameId, contentCache);
        }

        return gameStateCache.get(gameId);
    }

    /**
     * Gets the state of the given player
     *
     * @param pId id of the player
     * @return state
     */
    public Content getPlayerState(String gameId, String pId) {

        checkGameId(gameId);
        checkPlayerId(pId);

        if (!gameStateCache.containsKey(gameId))
            gameStateCache.put(gameId, new HashMap<String, Content>());

        Map<String, Content> contentCache = gameStateCache.get(gameId);

        if (!contentCache.containsKey(pId)) {
            WebTarget target = getTarget().path(DATA).path(gameId).path(PLAYER).path(pId).path(STATE);
            Response response = get(target);
            Content cnt = response.readEntity(Content.class);
            contentCache.put(pId, cnt);
        }

        return contentCache.get(pId);
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
        WebTarget target = getTarget().path(DATA).path(gameId).path(SEARCH);
        SearchPlayer search = new SearchPlayer();
        // initial fake value
        int totalPages = 999;
        Response response;
        JsonObject o;
        String s = search.json();
        target = target.queryParam("size", 200);

        Set<String> players = new TreeSet<>();

        for (int i = 1; i <= totalPages; i++) {
            response = post(target.queryParam("page", i), s);
            if (response == null)
                return players;
            o = decode(response);

            // update totalPages value
            if (totalPages == 999)
                totalPages = Integer.valueOf(String.valueOf(o.get("totalPages")));

            for (JsonElement e : o.get("content").getAsJsonArray()) {
                String pId = e.getAsJsonObject().get("playerId").toString().replace("\"", "");
                players.add(pId);
            }

        }

        return players;
    }


    /**
     * Insert given rule inside gamification engine
     *
     * @param gameId   unique id for game
     * @param toInsert to insert
     * @return {@link RuleDto} instance, null if error
     */
    public InsertedRuleDto insertGameRule(String gameId,
                                          InsertedRuleDto toInsert) {
        if (gameId == null || toInsert == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        WebTarget target = getTarget().path(GAME).path(gameId).path(RULE)
                .path(DB);

        Response response = post(target, toInsert);
        if (response != null)
            return response.readEntity(InsertedRuleDto.class);

        return null;
    }

    /**
     * Delete rule from game
     *
     * @param gameId - unique id for game
     * @param ruleId - unique id for rule
     * @return
     */
    public boolean deleteGameRule(String gameId, String ruleId) {
        if (gameId == null || ruleId == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        String ruleUrl = StringUtils.removeStart(ruleId, Constants.RULE_PREFIX);
        WebTarget target = getTarget().path(GAME).path(gameId).path(RULE)
                .path(DB).path(ruleUrl);
        Response response = target.request().delete();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            dbg(logger, "response code: " + response.getStatus());
            return true;
        }
        err(logger, "response code: " + response.getStatus() + " , reason="
                + response.getStatusInfo().getReasonPhrase());
        return false;
    }

    public boolean saveItinerary(ExecutionDataDTO input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        WebTarget target = getTarget().path(EXECUTE);
        Response response = post(target, input);
        return response != null;
    }

    public boolean updateChallengeCustomData(String gameId, String playerId,
                                             Map<String, Object> customData) {
        if (gameId == null || playerId == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        if (customData == null) {
            dbg(logger, "No customdata to update");
            return false;
        }
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("gameId", gameId);
        values.put("playerId", playerId);
        values.putAll(customData);
        ObjectMapper mapper = new ObjectMapper();
        try {
            dbg(logger, mapper.writeValueAsString(values));
        } catch (JsonProcessingException e) {
            err(logger, e.getMessage());
            return false;
        }

        WebTarget target = getTarget().path(GAME).path(gameId).path(PLAYER).path(playerId);
        Response response = put(target, values);
        return response != null;
    }

    public boolean executeAction(ExecutionDataDTO input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        WebTarget target = getTarget().path(EXECUTE);
        Response response = post(target, input);
        return response != null;
    }

    public boolean insertChallengeModel(String gameId, ChallengeModel model) {
        if (gameId == null || model == null) {
            throw new IllegalArgumentException(
                    "gameId and model cannot be null");
        }
        WebTarget target = getTarget().path(MODEL).path(gameId).path(CHALLENGE);
        Response response = post(target, model);
        return response != null;
    }

    public boolean assignChallengeToPlayer(ChallengeDataDTO cdd, String gameId, String playerId) {
        if (cdd == null || gameId == null || playerId == null) {
            throw new IllegalArgumentException("challenge, gameId and playerId cannot be null");
        }
        WebTarget target = challengePlayerPath(gameId, playerId);
        Response response = post(target, cdd);

        return response != null;
    }

    public List<LinkedHashMap<String, Object>> getChallengesPlayer(String gameId, String playerId) {
        if (gameId == null || playerId == null) {
            throw new IllegalArgumentException("challenge, gameId and playerId cannot be null");
        }
        WebTarget target = challengePlayerPath(gameId, playerId);
        Response response = get(target);

        List<LinkedHashMap<String, Object>> res = response.readEntity(List.class);

        return res;
    }

    private WebTarget challengePlayerPath(String gameId, String playerId) {
        return getTarget().path(DATA).path(gameId).path("player").path(playerId).path("challenges");
    }

    public Set<ChallengeModel> readChallengesModel(String gameId) {
        checkGameId(gameId);
        WebTarget target = getTarget().path(gameId).path("challenge");
        Response response = get(target);
        if (response == null)
            return new HashSet<ChallengeModel>();

        return (Set<ChallengeModel>) response.readEntity(Set.class);
    }

    private WebTarget getTarget() {
        return target;
    }

    public Response readGameStatistics(String gameId) {
        checkGameId(gameId);
        WebTarget target = getTarget().path(DATA).path(gameId).path("statistics");
        Response response = get(target);
return check(response);
    }
}

