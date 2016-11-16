package eu.trentorise.game.challenges.util;

public class ChallengeRuleRow {

	private String modelName;
	private String goalType;
	private Object target;
	private String pointType;
	private Double bonus;
	private String name;
	private String baselineVar;
	private String selectionCriteriaPoints;
	private String selectionCriteriaBadges;

	public String getGoalType() {
		return goalType;
	}

	public void setGoalType(String goalType) {
		this.goalType = goalType;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public String getPointType() {
		return pointType;
	}

	public void setPointType(String pointType) {
		this.pointType = pointType;
	}

	public void setBonus(Double bonus) {
		this.bonus = bonus;
	}

	public Double getBonus() {
		return bonus;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getBaselineVar() {
		return baselineVar;
	}

	public void setBaselineVar(String baselineVar) {
		this.baselineVar = baselineVar;
	}

	public void setSelectionCriteriaPoints(String selectionCriteriaPoints) {
		this.selectionCriteriaPoints = selectionCriteriaPoints;
	}

	public String getSelectionCriteriaPoints() {
		return selectionCriteriaPoints;
	}

	public void setSelectionCriteriaBadges(String selectionCriteriaBadges) {
		this.selectionCriteriaBadges = selectionCriteriaBadges;
	}

	public String getSelectionCriteriaBadges() {
		return selectionCriteriaBadges;
	}

	@Override
	public String toString() {
		return "[name=" + this.name + ",type=" + this.modelName + "]";
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
}
