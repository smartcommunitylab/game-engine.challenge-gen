package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "levelName",
        "levelValue",
        "pointConcept",
        "startLevelScore",
        "endLevelScore",
        "toNextLevel"
})
public class PlayerLevel {

    @JsonProperty("levelName")
    private String levelName;
    @JsonProperty("levelValue")
    private String levelValue;
    @JsonProperty("pointConcept")
    private String pointConcept;
    @JsonProperty("startLevelScore")
    private Double startLevelScore;
    @JsonProperty("endLevelScore")
    private Double endLevelScore;
    @JsonProperty("toNextLevel")
    private Double toNextLevel;

    @JsonProperty("levelName")
    public String getLevelName() {
        return levelName;
    }

    @JsonProperty("levelName")
    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    @JsonProperty("levelValue")
    public String getLevelValue() {
        return levelValue;
    }

    @JsonProperty("levelValue")
    public void setLevelValue(String levelValue) {
        this.levelValue = levelValue;
    }

    @JsonProperty("pointConcept")
    public String getPointConcept() {
        return pointConcept;
    }

    @JsonProperty("pointConcept")
    public void setPointConcept(String pointConcept) {
        this.pointConcept = pointConcept;
    }

    @JsonProperty("startLevelScore")
    public Double getStartLevelScore() {
        return startLevelScore;
    }

    @JsonProperty("startLevelScore")
    public void setStartLevelScore(Double startLevelScore) {
        this.startLevelScore = startLevelScore;
    }

    @JsonProperty("endLevelScore")
    public Double getEndLevelScore() {
        return endLevelScore;
    }

    @JsonProperty("endLevelScore")
    public void setEndLevelScore(Double endLevelScore) {
        this.endLevelScore = endLevelScore;
    }

    @JsonProperty("toNextLevel")
    public Double getToNextLevel() {
        return toNextLevel;
    }

    @JsonProperty("toNextLevel")
    public void setToNextLevel(Double toNextLevel) {
        this.toNextLevel = toNextLevel;
    }

}