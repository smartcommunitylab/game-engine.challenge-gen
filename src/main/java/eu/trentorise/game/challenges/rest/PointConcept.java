package eu.trentorise.game.challenges.rest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "name", "score", "instances", "id" })
public class PointConcept {

	@JsonProperty("name")
	private String name;
	@JsonProperty("score")
	private Double score;
	@JsonDeserialize(as = LinkedHashMap.class)
	private Map<String, Period> periods = new LinkedHashMap<String, Period>();
	@JsonProperty("id")
	private String id;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	/**
	 * 
	 * @return The name
	 */
	@JsonProperty("name")
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param name
	 *            The name
	 */
	@JsonProperty("name")
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return The score
	 */
	@JsonProperty("score")
	public Double getScore() {
		return score;
	}

	/**
	 * 
	 * @param score
	 *            The score
	 */
	@JsonProperty("score")
	public void setScore(Double score) {
		this.score = score;
	}

	/**
	 * 
	 * @return The id
	 */
	@JsonProperty("id")
	public String getId() {
		return id;
	}

	/**
	 * 
	 * @param id
	 *            The id
	 */
	@JsonProperty("id")
	public void setId(String id) {
		this.id = id;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}

	@JsonProperty("periods")
	public Map<String, Period> getPeriods() {
		return periods;
	}

	@JsonProperty("periods")
	public void setPeriods(Map<String, Period> periods) {
		this.periods = periods;
	}

	public Double getPeriodPreviousScore(String periodIdentifier) {
		LinkedList<PeriodInstance> instances = periods.get(periodIdentifier)
				.getInstances();
		return periods.containsKey(periodIdentifier) ? instances.get(
				instances.size() - 1).getScore() : 0d;
	}

	public Double getPeriodCurrentScore(String periodIdentifier) {
		return periods.containsKey(periodIdentifier) ? periods.get(
				periodIdentifier).getCurrentScore() : 0d;
	}

}
