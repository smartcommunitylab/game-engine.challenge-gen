package eu.trentorise.game.challenges.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.game.bean.ChallengeDataDTO;
import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.model.ChallengeModel;

/**
 * A facade for handling logic for Gamification Engine Rest api
 * 
 * @see <a
 *      href="https://github.com/smartcommunitylab/smartcampus.gamification">Gamification
 *      Engine</a>
 */
public class GamificationEngineRestFacade {

	private static final Logger logger = LogManager
			.getLogger(GamificationEngineRestFacade.class);

	private static final String STATE = "state";
	private static final String GAME = "game";
	private static final String RULE = "rule";
	private static final String DB = "db";
	private static final String EXECUTE = "execute";
	private static final String PLAYER = "player";
	private static final String MODEL = "model/game";

	private static final String CHALLENGE = "challenge";

	private WebTarget target;

	/**
	 * Create gamification engine rest facade on selected endpoint
	 * 
	 * @param endpoint
	 */
	public GamificationEngineRestFacade(final String endpoint) {
		if (endpoint == null) {
			throw new NullPointerException("Endpoint cannot be null");
		}
		logger.debug("GamificationEngineRestFacade created with no authentication");
		target = ClientBuilder.newClient().target(endpoint);
	}

	/**
	 * Create gamification engine rest facade on selected endpoint and select
	 * http basic authentication credentials
	 * 
	 * @param endpoint
	 * @param username
	 * @param password
	 * @throws NullPointerException
	 *             if parameters are null
	 */
	public GamificationEngineRestFacade(final String endpoint,
			final String username, final String password)
			throws NullPointerException {
		if (endpoint == null || username == null || password == null) {
			throw new NullPointerException(
					"Endpoint, username and password cannot be null");
		}
		logger.debug("GamificationEngineRestFacade created");
		// build endpoint
		// with htt p basic authentication
		HttpAuthenticationFeature feature = HttpAuthenticationFeature
				.basicBuilder().credentials(username, password).build();
		Client client = ClientBuilder.newClient();
		client.register(feature);
		target = client.target(endpoint);
		logger.debug("created endpoint for " + endpoint);
	}

	/**
	 * @return {@link WebTarget} to be used by facade
	 */
	private WebTarget getTarget() {
		return target;
	}

	/**
	 * Read game state
	 * 
	 * @param gameId
	 * @return a list of {@link Content}, null if error
	 */
	public List<Content> readGameState(String gameId) {
		if (gameId == null) {
			throw new IllegalArgumentException("gameId cannot be null");
		}
		WebTarget target = getTarget().path(STATE).path(gameId);
		// for testing pagination: target.queryParam("page",
		// 1).queryParam("size", 1).request().get(Paginator.class);
		Paginator response = target.request().get(Paginator.class);
		if (response == null) {
			logger.error("error in reading game state");
			return null;
		}
		if (response.getLast()) {
			logger.info("service return only one page of result");
			return response.getContent();
		}
		List<Content> result = new ArrayList<Content>();
		int page = 2;
		boolean end = false;
		Paginator pageResponse;
		result.addAll(response.getContent());
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
		logger.debug("service return " + page + " pages of result");
		return result;
	}

	/**
	 * Insert given rule inside gamification engine
	 * 
	 * @param gameId
	 *            unique id for game
	 * @param toInsert
	 *            to insert
	 * @return {@link RuleDto} instance, null if error
	 */
	public InsertedRuleDto insertGameRule(String gameId,
			InsertedRuleDto toInsert) {
		if (gameId == null || toInsert == null) {
			throw new IllegalArgumentException("input cannot be null");
		}
		WebTarget target = getTarget().path(GAME).path(gameId).path(RULE)
				.path(DB);
		Response response = target.request().post(Entity.json(toInsert));

		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			logger.debug("response code: " + response.getStatus());
			return response.readEntity(InsertedRuleDto.class);
		}
		logger.error("response code: " + response.getStatus() + ", reason: "
				+ response.getStatusInfo());
		return null;
	}

	/**
	 * Delete rule from game
	 * 
	 * @param gameId
	 *            - unique id for game
	 * @param ruleId
	 *            - unique id for rule
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
			logger.debug("response code: " + response.getStatus());
			return true;
		}
		logger.error("response code: " + response.getStatus());
		return false;
	}

	public boolean saveItinerary(ExecutionDataDTO input) {
		if (input == null) {
			throw new IllegalArgumentException("input cannot be null");
		}
		WebTarget target = getTarget().path(EXECUTE);
		Response response = target.request().post(Entity.json(input));
		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			logger.debug("response code: " + response.getStatus());
			return true;
		}
		logger.error("response code: " + response.getStatus());
		return false;
	}

	public boolean updateChallengeCustomData(String gameId, String playerId,
			Map<String, Object> customData) {
		if (gameId == null || playerId == null) {
			throw new IllegalArgumentException("input cannot be null");
		}
		if (customData == null) {
			logger.debug("No customdata to update");
			return false;
		}
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("gameId", gameId);
		values.put("playerId", playerId);
		values.putAll(customData);
		ObjectMapper mapper = new ObjectMapper();
		try {
			logger.debug(mapper.writeValueAsString(values));
		} catch (JsonProcessingException e) {
			logger.error(e);
			return false;
		}
		WebTarget target = getTarget().path(GAME).path(gameId).path(PLAYER)
				.path(playerId);
		Response response = target.request().put(Entity.json(values));
		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			logger.debug("response code: " + response.getStatus());
			return true;
		}
		logger.error("response code: " + response.getStatus());
		return false;
	}

	public boolean executeAction(ExecutionDataDTO input) {
		if (input == null) {
			throw new IllegalArgumentException("input cannot be null");
		}
		WebTarget target = getTarget().path(EXECUTE);
		Response response = target.request().post(Entity.json(input));
		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			logger.debug("response code: " + response.getStatus());
			return true;
		}
		logger.error("response code: " + response.getStatus());
		return false;
	}

	public boolean insertChallengeModel(String gameId, ChallengeModel model) {
		if (gameId == null || model == null) {
			throw new IllegalArgumentException(
					"gameId and model cannot be null");
		}
		WebTarget target = getTarget().path(MODEL).path(gameId).path(CHALLENGE);
		Response response = target.request().post(Entity.json(model));
		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			logger.debug("response code: " + response.getStatus());
			return true;
		}
		logger.error("response code: " + response.getStatus());
		return false;
	}

	public boolean assignChallengeToPlayer(ChallengeDataDTO cdd, String gameId,
			String playerId) {
		if (gameId == null || gameId == null || playerId == null) {
			throw new IllegalArgumentException(
					"challenge, gameId and playerId cannot be null");
		}
		WebTarget target = getTarget().path(gameId).path("player")
				.path(playerId).path("challenges");
		Response response = target.request().post(Entity.json(cdd));
		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			logger.debug("response code: " + response.getStatus());
			return true;
		}
		logger.error("response code: " + response.getStatus());
		return false;
	}
}
