package eu.fbk.das.rs.challenges;

import java.util.Set;

public class Challenge {
	private Set<String> playerSet;
	private String challengeTyp;
	private String strategy;
	private Set<String> pointConcepts;
	private Reward reward;

	public Set<String> getPlayerSet() {
		return playerSet;
	}

	public void setPlayerSet(Set<String> playerSet) {
		this.playerSet = playerSet;
	}

	public String getChallengeTyp() {
		return challengeTyp;
	}

	public void setChallengeTyp(String challengeTyp) {
		this.challengeTyp = challengeTyp;
	}

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	public Set<String> getPointConcepts() {
		return pointConcepts;
	}

	public void setPointConcepts(Set<String> pointConcepts) {
		this.pointConcepts = pointConcepts;
	}

	public static class Reward {
		private String scoreName;
		private String type;
		private double value;
		private double maxValue;

		public String getScoreName() {
			return scoreName;
		}

		public void setScoreName(String scoreName) {
			this.scoreName = scoreName;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public double getValue() {
			return value;
		}

		public void setValue(double value) {
			this.value = value;
		}

		public double getMaxValue() {
			return maxValue;
		}

		public void setMaxValue(double maxValue) {
			this.maxValue = maxValue;
		}

		@Override
		public String toString() {
			return "Reward [scoreName=" + scoreName + ", type=" + type + ", value=" + value + ", maxValue=" + maxValue
					+ "]";
		}

	}

	public Reward getReward() {
		return reward;
	}

	public void setReward(Reward reward) {
		this.reward = reward;
	}

	@Override
	public String toString() {
		return "Challenge [playerSet=" + playerSet + ", challengeTyp=" + challengeTyp + ", strategy=" + strategy
				+ ", pointConcepts=" + pointConcepts + ", reward=" + reward + "]";
	}
}
