package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import javax.annotation.Generated;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({"name", "score", "instances", "id"})
public class PointConcept {

    @JsonProperty("name")
    private String name;
    @JsonProperty("score")
    private Double score;
    @JsonDeserialize(as = LinkedHashMap.class)
    private Map<String, PeriodInternal> periods = new LinkedHashMap<String, PeriodInternal>();
    @JsonProperty("id")
    private String id;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    @JsonIgnore
    long executionMoment = System.currentTimeMillis();

    public PointConcept(String name, long moment) {
        this.name = name;
        executionMoment = moment;
    }

    public PointConcept(String name) {
        this.name = name;
    }

    @JsonCreator
    public PointConcept(Map<String, Object> jsonProps) {
        Object idField = jsonProps.get("id");
        id = (idField != null) ? String.valueOf(idField) : null;
        name = (String) jsonProps.get("name");
        Object scoreField = jsonProps.get("score");
        // fix: in some case PointConcept JSON representation contains 0 value
        // in score field
        // and so it is cast to Integer
        if (scoreField != null) {
            if (scoreField instanceof Double) {
                score = (Double) scoreField;
            }
            if (scoreField instanceof Integer) {
                score = ((Integer) scoreField).doubleValue();
            }
        }
        Map<String, Object> temp = (Map<String, Object>) jsonProps.get("periods");
        if (temp != null) {
            Set<Entry<String, Object>> entries = temp.entrySet();
            for (Entry<String, Object> entry : entries) {
                periods.put(entry.getKey(),
                        new PeriodInternal((Map<String, Object>) entry.getValue()));
            }
        }
    }

    public Period getPeriod(String periodName) {
        return periods.get(periodName);
    }

    /*
     * Actually I must have this methods to permit Jackson to correctly serialize the inner class
     */
    public Map<String, PeriodInternal> getPeriods() {
        return periods;
    }

    /*
     * Actually I must have this methods to permit Jackson to correctly serialize the inner class
     */
    public void setPeriods(Map<String, PeriodInternal> periods) {
        this.periods = periods;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        increasePeriodicPoints(score - this.score);
        this.score = score;
    }

    public Double increment(Double score) {
        increasePeriodicPoints(score);
        this.score += score;
        return this.score;
    }

    private void increasePeriodicPoints(Double score) {
        for (PeriodInternal p : periods.values()) {
            p.increaseScore(score, executionMoment);
        }
    }

    public void addPeriod(String identifier, Date start, long period) {
        PeriodInternal p = new PeriodInternal(identifier, start, period);
        if (!periods.containsKey(identifier)) {
            periods.put(identifier, p);
        }
    }

    public void deletePeriod(String identifier) {
        periods.remove(identifier);
    }

    public Double getPeriodCurrentScore(int periodIndex) {
        return new ArrayList<>(periods.values()).get(periodIndex).getCurrentScore();
    }

    public Double getPeriodCurrentScore(String periodIdentifier) {
        return periods.containsKey(periodIdentifier)
                ? periods.get(periodIdentifier).getCurrentScore() : 0d;
    }

    public PeriodInstance getPeriodCurrentInstance(int periodIndex) {
        return new ArrayList<>(periods.values()).get(periodIndex).getCurrentInstance();
    }

    public PeriodInstance getPeriodCurrentInstance(String periodIdentifier) {
        return periods.containsKey(periodIdentifier)
                ? periods.get(periodIdentifier).getCurrentInstance() : null;
    }

    public Double getPeriodPreviousScore(String periodIdentifier) {
        return getPeriodScore(periodIdentifier, 1);
    }

    public PeriodInstance getPeriodPreviousInstance(String periodIdentifier) {
        return getPeriodInstance(periodIdentifier, 1);
    }

    public Double getPeriodScore(String periodIdentifier, long moment) {
        if (!periods.containsKey(periodIdentifier))
            return 0d;

        PeriodInternal p = periods.get(periodIdentifier);
        LocalDateTime momentDate = new LocalDateTime(moment);
        if (p.start.after(momentDate.toDate()))
            return 0d;

        return p.getScore(moment);
    }

    public PeriodInstance getPeriodInstance(String periodIdentifier, long moment) {
        return periods.containsKey(periodIdentifier)
                ? periods.get(periodIdentifier).retrieveInstance(moment) : null;
    }

    public Double getPeriodScore(String periodIdentifier, int instanceIndex) {
        Double result = 0d;
        PeriodInternal p = periods.get(periodIdentifier);
        if (p != null) {
            PeriodInstance instance = getPeriodInstance(periodIdentifier, instanceIndex);
            result = instance != null ? instance.getScore() : 0d;
        }

        return result;
    }

    public PeriodInstance getPeriodInstance(String periodIdentifier, int instanceIndex) {
        PeriodInstance result = null;
        PeriodInternal p = periods.get(periodIdentifier);
        if (p != null) {
            LocalDateTime dateCursor = new LocalDateTime(p.start);
            dateCursor =
                    dateCursor.withPeriodAdded(new org.joda.time.Period(p.period), instanceIndex);
            result = getPeriodInstance(periodIdentifier, dateCursor.toDate().getTime());
        }

        return result;
    }

    public interface Period {
        public Date getStart();

        public long getPeriod();

        public String getIdentifier();
    }

    public interface PeriodInstance {
        public Double getScore();

        public long getStart();

        public long getEnd();

        public int getIndex();
    }

    public class PeriodInternal implements Period {
        private Date start;
        private long period;
        private String identifier;
        private int capacity;

        /*
         * JsonDeserialize is used by convertValue method of ObjectMapper field of PlayerState. In
         * constructor of playerState the StatePersistence object is deserialized.
         *
         * convertValue method invoked PointConcept constructor annotated with JsonCreator.
         *
         * Improve this flow. Use PointConceptTest.persistPeriod() as example
         */
        @JsonDeserialize(keyUsing = LocalDateTimeDeserializer.class)
        // @JsonSerialize(keyUsing = LocalDateTimeSerializer.class)
        private TreeMap<LocalDateTime, PeriodInstanceImpl> instances = new TreeMap<>();

        public PeriodInternal(String identifier, Date start, long period) {
            this.start = start;
            this.period = period;
            this.identifier = identifier;
        }

        public PeriodInternal(String identifier, Date start, long period, int capacity) {
            this.start = start;
            this.period = period;
            this.identifier = identifier;
            this.capacity = capacity;
        }

        public PeriodInternal(Map<String, Object> jsonProps) {
            if (jsonProps != null) {
                Object startField = jsonProps.get("start");
                if (startField != null) {
                    start = new Date((long) startField);
                }
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
                Object capacityField = jsonProps.get("capacity");
                if (capacityField != null) {
                    capacity = (int) capacityField;
                }
                Map<String, Map<String, Object>> tempInstances =
                        (Map<String, Map<String, Object>>) jsonProps.get("instances");
                if (tempInstances != null) {
                    Set<Entry<String, Map<String, Object>>> entries = tempInstances.entrySet();
                    for (Entry<String, Map<String, Object>> entry : entries) {
                        instances.put(
                                DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")
                                        .parseLocalDateTime(entry.getKey()),
                                new PeriodInstanceImpl(entry.getValue()));
                    }
                }
            }
        }

        private PeriodInstanceImpl getCurrentInstance() {
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

        public Double getScore(long moment) {
            try {
                return retrieveInstance(moment).getScore();
            } catch (NullPointerException e) {
                return 0d;
            }
        }

        public Double increaseScore(Double value, long moment) {
            try {
                PeriodInstanceImpl instance = retrieveInstance(moment);
                return instance.increaseScore(value);
            } catch (IllegalArgumentException e) {
                return 0d;
            }
        }

        private PeriodInstanceImpl retrieveInstance(long moment) {
            LocalDateTime momentDate = new LocalDateTime(moment);
            if (start.after(momentDate.toDate())) {
                throw new IllegalArgumentException("moment is previous than startDate of period");
            }

            PeriodInstanceImpl instance = null;
            LocalDateTime key = null;
            LocalDateTime lowerBoundDate = instances.floorKey(momentDate);
            if (lowerBoundDate == null) {
                lowerBoundDate = new LocalDateTime(start.getTime());
            }
            org.joda.time.Period jodaPeriod = new org.joda.time.Period(period);
            Interval interval = null;
            do {
                interval = new Interval(lowerBoundDate.toDateTime(),
                        lowerBoundDate.withPeriodAdded(jodaPeriod, 1).toDateTime());
                lowerBoundDate = interval.getEnd().toLocalDateTime();
            } while (!interval.contains(moment));

            instance = instances.get(interval.getStart().toLocalDateTime());
            if (instance == null) {
                instance =
                        new PeriodInstanceImpl(interval.getStartMillis(), interval.getEndMillis());
                instance.setIndex(getInstanceIndex(new LocalDateTime(start.getTime()), jodaPeriod,
                        momentDate));
                key = interval.getStart().toLocalDateTime();
                instances.put(key, instance);
                if (capacity > 0 && instances.size() > capacity) {
                    instances.pollFirstEntry();
                }
            }

            return instance;
        }

        private int getInstanceIndex(LocalDateTime start, org.joda.time.Period period,
                                     LocalDateTime momentDate) {
            int index = -1;
            Interval interval = null;
            LocalDateTime cursorDate = start;
            DateTime moment = momentDate.toDateTime();
            do {
                interval = new Interval(cursorDate.toDateTime(),
                        cursorDate.withPeriodAdded(period, 1).toDateTime());
                cursorDate = interval.getEnd().toLocalDateTime();
                index++;
            } while (!interval.contains(moment));

            return index;
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

        public TreeMap<LocalDateTime, PeriodInstanceImpl> getInstances() {
            return instances;
        }

        public void setInstances(TreeMap<LocalDateTime, PeriodInstanceImpl> instances) {
            this.instances = instances;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }

        // @Override
        // public int getCapacity() {
        // return capacity;
        // }
        //
        // public int setCapacity(int capacity) {
        // return this.capacity = capacity;
        // }

    }

    public class PeriodInstanceImpl implements PeriodInstance {
        private Double score = 0d;
        private long start;
        private long end;
        private int index;

        public PeriodInstanceImpl(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public PeriodInstanceImpl(Map<String, Object> jsonProps) {
            Object scoreField = jsonProps.get("score");
            Object startField = jsonProps.get("start");
            Object endField = jsonProps.get("end");
            Object indexField = jsonProps.get("index");
            if (scoreField != null) {
                if (scoreField instanceof Double) {
                    score = (Double) scoreField;
                }

                if (scoreField instanceof Integer) {
                    score = Integer.valueOf((Integer) scoreField).doubleValue();
                }
            }

            if (startField != null) {
                if (startField instanceof Long) {
                    start = (Long) startField;
                }

                if (startField instanceof Integer) {
                    start = Integer.valueOf((Integer) startField).longValue();
                }
            }

            if (endField != null) {
                if (endField instanceof Long) {
                    end = (Long) endField;
                }

                if (endField instanceof Integer) {
                    end = Integer.valueOf((Integer) endField).longValue();
                }
            }

            if (indexField != null) {
                if (indexField instanceof Integer) {
                    index = Integer.valueOf((Integer) indexField);
                }
            }
        }

        public Double increaseScore(Double value) {
            score = score + value;
            return score;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        @Override
        public String toString() {
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return String.format("[start: %s, end: %s, score: %s]",
                    formatter.format(new Date(start)), formatter.format(new Date(end)), score);
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    /*
     * ExecutionMoment should be a private immutable field. Actually I need public getter/setter to
     * expose the field to some tests with temporary constraints...TO IMPROVE
     */
    public long getExecutionMoment() {
        return executionMoment;
    }

    /*
     * ExecutionMoment should be a private immutable field. Actually I need public getter/setter to
     * expose the field to some tests with temporary constraints...TO IMPROVE
     */
    public void setExecutionMoment(long executionMoment) {
        this.executionMoment = executionMoment;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PointConcept) {
            PointConcept toCompare = (PointConcept) obj;
            return toCompare == this || name.equals(toCompare.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 15).append(name).hashCode();
    }

    @Override
    public String toString() {
        return String.format("{name: %s, score: %s}", name, score);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
