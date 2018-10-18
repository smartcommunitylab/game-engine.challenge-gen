package eu.trentorise.game.challenges.model;

import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.rs.Utils.f;
import static eu.fbk.das.rs.Utils.formatDateTime;

public class ChallengeDataDTO {

    private String modelName;
    private String instanceName;
    private Date start;
    private Date end;

    private Map<String, Object> data = new HashMap<>();
    // private Map<String, Object> info = new HashMap<>();

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

    public void setData(String name, Object value) {
        this.data.put(name, value);
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

    public void setStart(DateTime start) {
        setStart(start.toDate());
    }

    public void setEnd(DateTime end) {
        setEnd(end.toDate());
    }


    public void addInfo(String s, Object v) {

        // info.put(s, v);
    }

    public Object getInfo(String s) {
        return null; // info.get(s);
    }

    public Vector<Object> getDisplayData() {
        Vector<Object> result = new Vector<>();
        result.add(getInfo("player"));
        result.add(getInfo("playerLevel"));
        result.add(getModelName());
        result.add(getData().get("counterName"));
        result.add(m(getData().get("baseline")));
        result.add(m(getData().get("target")));
        result.add(m( getData().get("difficulty")));
        result.add(getData().get("bonusScore"));
        result.add(getState());
        result.add(getPriority());

        result.add(formatDateTime(new DateTime(getStart())));
        result.add(formatDateTime(new DateTime(getEnd())));
        
        return result;
    }

    public Vector<Object> getWriteData() {
        Vector<Object> result = getDisplayData();
        result.add(instanceName);
        return result;
    }

    private String m(Object o) {
        try {
            String s = f("%.2f", o);
            return s.replace("nu", "");
        } catch (IllegalFormatConversionException ignored) {}

        return f("%d", o);
    }
}
