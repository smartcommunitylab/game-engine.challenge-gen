package eu.trentorise.game.challenges.util;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.trentorise.game.challenges.rest.BadgeCollectionConcept;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.PointConcept;

public class Matcher {

	private static final Logger logger = LogManager.getLogger(Matcher.class);
	private static final String[] operators = { "&&" };
	private static final String[] comparisonOperator = { "==", "<=", ">=", "<",
			">" };
	private ChallengeRuleRow challenge;
	private ScriptEngineManager manager;
	private ScriptEngine engine;

	public Matcher(ChallengeRuleRow challenge) throws IllegalArgumentException {
		this.challenge = challenge;
		this.manager = new ScriptEngineManager();
		this.engine = manager.getEngineFactories().get(0).getScriptEngine();
		// check if at least one type of criteria is defined
		// TODO: Do we need some sort of validation of criteria before use them
		// ?
		if (challenge.getSelectionCriteriaCustomData() != null
				&& !challenge.getSelectionCriteriaCustomData().isEmpty()) {
			// ok
		} else if (challenge.getSelectionCriteriaPoints() != null
				&& !challenge.getSelectionCriteriaPoints().isEmpty()) {
			// ok
		} else if (challenge.getSelectionCriteriaBadges() != null
				&& !challenge.getSelectionCriteriaBadges().isEmpty()) {
			// ok
		} else {
			throw new IllegalArgumentException(
					"no criteria defined for challenge: " + challenge.getName());
		}
	}

	public List<Content> match(List<Content> users) {
		if (users == null || users.isEmpty()) {
			logger.warn("No users to match");
			return new ArrayList<Content>();
		}
		List<Content> result = new ArrayList<Content>();
		for (Content user : users) {
			if (challenge.getSelectionCriteriaCustomData() != null
					&& !challenge.getSelectionCriteriaCustomData().isEmpty()) {
				if (customDataMatch(user)) {
					result.add(user);
				}
			} else if (challenge.getSelectionCriteriaPoints() != null
					&& !challenge.getSelectionCriteriaPoints().isEmpty()) {
				if (pointsMatch(user)) {
					result.add(user);
				}
			} else if (challenge.getSelectionCriteriaBadges() != null
					&& !challenge.getSelectionCriteriaBadges().isEmpty()) {
				if (badgeMatch(user)) {
					result.add(user);
				}
			}

		}
		return result;
	}

	private boolean customDataMatch(Content user) {
		String criteria = challenge.getSelectionCriteriaCustomData();
		logger.debug("criteria to evaluate: " + criteria);
		if (criteria.equalsIgnoreCase("true")) {
			return true;
		}
		if (isUserValidCustomData(user, criteria)) {
			List<String> vars = getVariablesFromCriteria(criteria);
			for (String var : vars) {
				engine.put(var, user.getCustomData().getAdditionalProperties()
						.get(var));
			}
			try {
				Object result = engine.eval(criteria);
				if (result instanceof Boolean) {
					return (Boolean) result;
				}
				return false;
			} catch (ScriptException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return false;
	}

	private boolean pointsMatch(Content user) {
		String criteria = challenge.getSelectionCriteriaPoints();
		logger.debug("criteria to evaluate: " + criteria);
		if (criteria.equalsIgnoreCase("true")) {
			return true;
		}
		// get current point type to evaluate
		List<String> vars = getVariableFromPointCriteria(criteria);
		if (vars.isEmpty()) {
			logger.warn("Point type criteria malformed, empty point type found");
			return false;
		}
		for (String var : vars) {
			if (!isPointConcept(user, var)) {
				logger.warn("User " + user.getPlayerId()
						+ " don't have point concept with name " + var);
				return false;
			}
		}
		String originalName = "";
		String newName = "";
		String newCriteria = "";
		// gather data
		for (String var : vars) {
			// because javascriptengine don't like variable with blank
			// space in declaration, change them with anotherChar
			originalName = new String(var);
			newName = StringUtils.replaceChars(var, " ", "_");
			newCriteria = StringUtils.replace(criteria, originalName, newName);
			double score = getScoreFromConcept(user, var);
			// evaluate criteria
			engine.put(newName, score);
		}
		// evaluate criteria
		try {
			Object result = engine.eval(newCriteria);
			if (result instanceof Boolean) {
				return (Boolean) result;
			}
			return false;
		} catch (ScriptException e) {
			logger.error(e.getMessage(), e);
		}

		return false;
	}

	private boolean badgeMatch(Content user) {
		String criteria = challenge.getSelectionCriteriaBadges();
		logger.debug("criteria to evaluate: " + criteria);
		if (user.getState() != null
				&& user.getState().getBadgeCollectionConcept() != null
				&& !user.getState().getBadgeCollectionConcept().isEmpty()) {
			// find right badge collection
			for (BadgeCollectionConcept bc : user.getState()
					.getBadgeCollectionConcept()) {
				if (bc.getName().equalsIgnoreCase(challenge.getGoalType())) {
					if (criteria.contains("size")) {
						String value = getValueFromCriteria(criteria);
						if (!value.isEmpty()) {
							engine.put("size", bc.getBadgeEarned().size());
							try {
								Object result = engine.eval(criteria);
								if (result instanceof Boolean) {
									return (Boolean) result;
								}
								return false;
							} catch (ScriptException e) {
								logger.error(e.getMessage(), e);
							}
						}
					}
				}
			}
		}
		return false;
	}

	private String getValueFromCriteria(String criteria) {
		if (criteria == null) {
			return "";
		}
		for (String co : comparisonOperator) {
			String[] elements = criteria.split(co);
			if (elements != null && elements.length == 2) {
				return StringUtils.stripStart(elements[1], null);
			}
		}
		return "";
	}

	private double getScoreFromConcept(Content user, String var) {
		for (PointConcept pc : user.getState().getPointConcept()) {
			if (pc.getName().equalsIgnoreCase(var)) {
				return pc.getScore();
			}
		}
		return 0;
	}

	private boolean isPointConcept(Content user, String pointType) {
		if (user.getState() != null
				&& user.getState().getPointConcept() != null) {
			boolean found = false;
			for (PointConcept pc : user.getState().getPointConcept()) {
				if (pc.getName().equalsIgnoreCase(pointType)) {
					found = true;
				}
			}
			return found;
		}
		return false;
	}

	private List<String> getVariableFromPointCriteria(String criteria) {
		List<String> result = new ArrayList<String>();
		for (String separator : operators) {
			String[] expressions = criteria.split(separator);
			if (expressions != null && expressions.length > 1) {
				for (String exp : expressions) {
					for (String operator : comparisonOperator) {
						String[] exps = exp.split(operator);
						if (exps != null && exps.length > 1) {
							String v = StringUtils.stripEnd(exps[0], null);
							v = StringUtils.stripStart(v, null);
							if (!result.contains(v)) {
								result.add(v);
							}
						}
					}
				}
			} else {
				for (String operator : comparisonOperator) {
					String[] exps = expressions[0].split(operator);
					if (exps != null && exps.length > 1) {
						String v = StringUtils.stripEnd(exps[0], null);
						v = StringUtils.stripStart(v, null);
						if (!result.contains(v)) {
							result.add(v);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * @return true if user exist and contains all custom data mentioned in
	 *         criteria
	 */
	private boolean isUserValidCustomData(Content user, String criteria) {
		if (user != null && criteria != null && !criteria.isEmpty()
				&& user.getCustomData() != null
				&& user.getCustomData().getAdditionalProperties() != null) {
			List<String> vars = new ArrayList<String>();
			if (containsAnyOperator(criteria)) {
				vars = getVariablesFromCriteria(criteria);
			} else {
				vars = getVariablesFromSingleCriteria(criteria);
			}

			for (String var : vars) {
				if (criteria.contains("null")) {
					// do nothing
				}
				// if is a single criteria and have a null check, don't try to
				// read additional properties
				// if (isSingleCriteria(criteria)) {
				// if (criteria.contains("null")) {
				// // do nothing
				// } else if (!user.getCustomData().getAdditionalProperties()
				// .containsKey(var)
				// || user.getCustomData().getAdditionalProperties()
				// .get(var) == null) {
				// logger.warn("Custom data not found " + var);
				// return false;
				// }
				// }
				else if (!user.getCustomData().getAdditionalProperties()
						.containsKey(var)
						|| user.getCustomData().getAdditionalProperties()
								.get(var) == null) {
					logger.warn("Custom data not found " + var);
					return false;
				}
			}
			return true;
		}
		logger.warn("user null or not custom data available");
		return false;
	}

	private List<String> getVariablesFromCriteria(String criteria) {
		List<String> result = new ArrayList<String>();
		for (String operator : operators) {
			String[] expressions = criteria.split(operator);
			for (int i = 0; i < expressions.length; i++) {
				// expression in the form: var operator value
				String var = StringUtils.stripStart(expressions[i], null)
						.split(" ")[0];
				if (!result.contains(var)) {
					result.add(var);
				}
			}
		}
		return result;
	}

	private List<String> getVariablesFromSingleCriteria(String expression) {
		List<String> result = new ArrayList<String>();
		// expression in the form: var operator value
		String var = StringUtils.stripStart(expression, null).split(" ")[0];
		if (!result.contains(var)) {
			result.add(var);
		}
		return result;
	}

	private boolean containsAnyOperator(String value) {
		for (String operator : operators) {
			if (value.contains(operator)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSingleCriteria(String value) {
		return StringUtils.countMatches(value, operators[0]) == 0 ? true
				: false;
	}
}
