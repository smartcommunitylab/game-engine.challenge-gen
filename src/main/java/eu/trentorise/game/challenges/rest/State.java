package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({"BadgeCollectionConcept", "PointConcept"})
public class State extends HashMap<String, List<Object>> {

    private static final long serialVersionUID = -338854568693735588L;

    private ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("BadgeCollectionConcept")
    private List<eu.trentorise.game.challenges.rest.BadgeCollectionConcept> BadgeCollectionConcept =
            new ArrayList<eu.trentorise.game.challenges.rest.BadgeCollectionConcept>();
    @JsonProperty("PointConcept")
    private List<eu.trentorise.game.challenges.rest.PointConcept> PointConcept =
            new ArrayList<eu.trentorise.game.challenges.rest.PointConcept>();
    @JsonProperty("ChallengeConcept")
    private List<eu.trentorise.game.challenges.rest.ChallengeConcept> ChallengeConcept =
            new ArrayList<eu.trentorise.game.challenges.rest.ChallengeConcept>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The BadgeCollectionConcept
     */
    @JsonProperty("BadgeCollectionConcept")
    public List<eu.trentorise.game.challenges.rest.BadgeCollectionConcept> getBadgeCollectionConcept() {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class,
                eu.trentorise.game.challenges.rest.BadgeCollectionConcept.class);
        return mapper.convertValue(get("BadgeCollectionConcept"), type);
    }

    /**
     *
     * @param BadgeCollectionConcept The BadgeCollectionConcept
     */
    // @JsonProperty("BadgeCollectionConcept")
    // public void setBadgeCollectionConcept(
    // List<eu.trentorise.game.challenges.rest.BadgeCollectionConcept> BadgeCollectionConcept) {
    // this.BadgeCollectionConcept = BadgeCollectionConcept;
    // }

    /**
     * @return The PointConcept
     */
    @JsonProperty("PointConcept")
    public List<eu.trentorise.game.challenges.rest.PointConcept> getPointConcept() {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class,
                eu.trentorise.game.challenges.rest.PointConcept.class);
        return mapper.convertValue(get("PointConcept"), type);
    }

    /**
     * @param PointConcept The PointConcept
     */
    // @JsonProperty("PointConcept")
    // public void setPointConcept(
    // List<eu.trentorise.game.challenges.rest.PointConcept> PointConcept) {
    // this.PointConcept = PointConcept;
    // }
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @JsonProperty("ChallengeConcept")
    public List<eu.trentorise.game.challenges.rest.ChallengeConcept> getChallengeConcept() {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class,
                eu.trentorise.game.challenges.rest.ChallengeConcept.class);
        return mapper.convertValue(get("ChallengeConcept"), type);
    }

    // @JsonProperty("ChallengeConcept")
    // public void setChallengeConcept(
    // List<eu.trentorise.game.challenges.rest.ChallengeConcept> challengeConcept) {
    // ChallengeConcept = challengeConcept;
    // }

}
