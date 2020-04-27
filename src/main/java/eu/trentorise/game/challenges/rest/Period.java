package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class Period {
    private Date start;
    private long period;
    private String identifier;
    private LinkedList<PeriodInstance> instances = new LinkedList<>();

    long executionMoment = System.currentTimeMillis();

    public Period() {
    }

    public Period(String identifier) {
        this.identifier = identifier;
    }

    public Period(String identifier, Date start, long period) {
        this.start = start;
        this.period = period;
        this.identifier = identifier;
    }

    public Period(Map<String, Object> jsonProps) {
        start = new Date((long) jsonProps.get("start"));
        Object periodField = jsonProps.get("period");
        if (periodField != null) {
            if (periodField instanceof Long) {
                period = (Long) periodField;
            }
            if (periodField instanceof Integer) {
                period = Integer.valueOf((Integer) periodField).longValue();
            }
        }
        identifier = (String) jsonProps.get("identifier");
        List<Map<String, Object>> tempInstances = (List<Map<String, Object>>) jsonProps
                .get("instances");
        if (tempInstances != null) {
            for (Map<String, Object> tempInstance : tempInstances) {
                instances.add(new PeriodInstance(tempInstance));
            }
        }

    }

    public String getIdentifier() {
        return identifier;
    }

    private PeriodInstance getCurrentInstance() {
        return retrieveInstance(executionMoment);
    }

    @JsonIgnore
    public Double getCurrentScore() {
        try {
            return getCurrentInstance().getScore();
        } catch (IllegalArgumentException e) {
            return 0d;
        }
    }

    public Double increaseScore(Double value, long moment) {
        try {
            PeriodInstance instance = retrieveInstance(moment);
            return instance.increaseScore(value);
        } catch (IllegalArgumentException e) {
            return 0d;
        }
    }

    private PeriodInstance retrieveInstance(long moment) {
        if (moment < start.getTime()) {
            throw new IllegalArgumentException(
                    "moment is previous than startDate of period");
        }
        PeriodInstance instance = null;
        long startInstance = -1;
        long endInstance = -1;
        if (instances.isEmpty() || instances.getLast().getEnd() < moment) {
            startInstance = instances.isEmpty() ? start.getTime() : instances
                    .getLast().getEnd() + 1;
            endInstance = instances.isEmpty() ? startInstance + period
                    : instances.getLast().getEnd() + period;
            instance = new PeriodInstance(0d, startInstance, endInstance);
            instances.add(instance);

            while (endInstance < moment) {
                startInstance = endInstance + 1;
                endInstance = endInstance + period;
                instance = new PeriodInstance(0d, startInstance, endInstance);
                instances.add(instance);
            }
        } else {

            for (Iterator<PeriodInstance> iter = instances.descendingIterator(); iter
                    .hasNext(); ) {
                PeriodInstance instanceTemp = iter.next();
                if (moment > instanceTemp.getStart()
                        && moment < instanceTemp.getEnd()) {
                    instance = instanceTemp;
                    break;
                }
            }
        }

        return instance;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public LinkedList<PeriodInstance> getInstances() {
        return instances;
    }

    public void setInstances(LinkedList<PeriodInstance> instances) {
        this.instances = instances;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}