package eu.trentorise.game.challenges.util;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		// TODO
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
	String pointType = getVariableFromPointCriteria(criteria);
	if (pointType.isEmpty()) {
	    logger.warn("Point type criteria malformed, empty point type found");
	    return false;
	}
	if (user.getState() != null
		&& user.getState().getPointConcept() != null) {
	    String originalName = new String(pointType);
	    String newName = "";
	    String newCriteria = "";
	    for (PointConcept pc : user.getState().getPointConcept()) {
		if (pc.getName().equalsIgnoreCase(pointType)) {
		    // because javascriptengine don't like variable with blank
		    // space in declaration, change them with anotherChar
		    newName = StringUtils.replaceChars(pointType, " ", "_");
		    newCriteria = StringUtils.replace(criteria, originalName,
			    newName);
		    // evaluate criteria
		    engine.put(newName, pc.getScore());
		    try {
			Object result = engine.eval(newCriteria);
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
	return false;
    }

    private String getVariableFromPointCriteria(String criteria) {
	for (String operator : comparisonOperator) {
	    String[] expressions = criteria.split(operator);
	    if (expressions != null && expressions.length > 1) {
		for (int i = 0; i < expressions.length; i++) {
		    // expression in the form: var operator value
		    if (expressions[i] != null && !expressions[i].isEmpty()) {
			return StringUtils.stripEnd(expressions[i], null);
		    }
		}
	    }
	}
	return "";
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
		} else if (!user.getCustomData().getAdditionalProperties()
			.containsKey(var)) {
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
}
