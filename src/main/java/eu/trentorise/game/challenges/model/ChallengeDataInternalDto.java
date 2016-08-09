package eu.trentorise.game.challenges.model;

public class ChallengeDataInternalDto {

	private String playerId;
	private String gameId;
	private ChallengeDataDTO dto;

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public ChallengeDataDTO getDto() {
		return dto;
	}

	public void setDto(ChallengeDataDTO dto) {
		this.dto = dto;
	}

}
