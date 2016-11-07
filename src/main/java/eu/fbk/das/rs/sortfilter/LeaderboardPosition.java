package eu.fbk.das.rs.sortfilter;

/**
 * LeaderboardPosition class used for leaderboard creation
 */
public class LeaderboardPosition implements Comparable<LeaderboardPosition> {

	private Integer score;
	private String playerId;
	private Integer index;

	public LeaderboardPosition(Integer score, String playerId) {
		this.score = score;
		this.playerId = playerId;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	@Override
	public int compareTo(LeaderboardPosition o) {
		if (this.score == o.getScore()) {
			return 0;
		} else if (this.getScore() > o.getScore()) {
			return -1;
		}
		return 1;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

}
