package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({"start", "period", "identifier", "instances"})
public class Weekly {

    @JsonProperty("start")
    private Long start;
    @JsonProperty("period")
    private Long period;
    @JsonProperty("identifier")
    private String identifier;
    @JsonProperty("instances")
    private List<Object> instances = new ArrayList<Object>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The start
     */
    @JsonProperty("start")
    public Long getStart() {
        return start;
    }

    /**
     * @param start The start
     */
    @JsonProperty("start")
    public void setStart(Long start) {
        this.start = start;
    }

    /**
     * @return The period
     */
    @JsonProperty("period")
    public Long getPeriod() {
        return period;
    }

    /**
     * @param period The period
     */
    @JsonProperty("period")
    public void setPeriod(Long period) {
        this.period = period;
    }

    /**
     * @return The identifier
     */
    @JsonProperty("identifier")
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier The identifier
     */
    @JsonProperty("identifier")
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return The instances
     */
    @JsonProperty("instances")
    public List<Object> getInstances() {
        return instances;
    }

    /**
     * @param instances The instances
     */
    @JsonProperty("instances")
    public void setInstances(List<Object> instances) {
        this.instances = instances;
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
