package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "weekly"
})
public class Periods {

    @JsonProperty("weekly")
    private Weekly weekly;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The weekly
     */
    @JsonProperty("weekly")
    public Weekly getWeekly() {
        return weekly;
    }

    /**
     * @param weekly The weekly
     */
    @JsonProperty("weekly")
    public void setWeekly(Weekly weekly) {
        this.weekly = weekly;
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
