package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "name",
        "badgeEarned"
})
public class BadgeCollectionConcept {

    @JsonProperty("name")
    private String name;
    @JsonProperty("badgeEarned")
    private List<String> badgeEarned = new ArrayList<String>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The name
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * @param name The name
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The badgeEarned
     */
    @JsonProperty("badgeEarned")
    public List<String> getBadgeEarned() {
        return badgeEarned;
    }

    /**
     * @param badgeEarned The badgeEarned
     */
    @JsonProperty("badgeEarned")
    public void setBadgeEarned(List<String> badgeEarned) {
        this.badgeEarned = badgeEarned;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
