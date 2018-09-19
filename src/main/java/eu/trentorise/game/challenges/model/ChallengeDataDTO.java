package eu.trentorise.game.challenges.model;

import java.util.Date;
import java.util.Map;

import static eu.fbk.das.rs.Utils.f;

public class ChallengeDataDTO {

    private String modelName;
    private String instanceName;
    private Map<String, Object> data;
    private Date start;
    private Date end;

    // can be either PROPOSED, ASSIGNED, ACTIVE, COMPLETED, FAILED (default value is assigned)
    private String state;
    // origin of the challenge (recommendation system)
    private String origin;
    // if no challenge is chosen by the user, it will be given the one with highest priority
    private String priority;

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public String toString() {
        return f("%s - %s - %s - %s - %s", modelName, data.get("bonusScore"), data.get("counterName"), data.get("target"), start.toString());
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

}
