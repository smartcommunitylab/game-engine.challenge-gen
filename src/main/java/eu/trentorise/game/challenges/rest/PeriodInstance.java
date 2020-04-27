package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({"score", "start", "end", "index"})
public class PeriodInstance {

    @JsonProperty("score")
    private Double score = 0d;
    @JsonProperty("start")
    private long start;
    @JsonProperty("end")
    private long end;
    @JsonProperty("index")
    private long index;

    public PeriodInstance() {
    }

    public PeriodInstance(Double score, long start, long end) {
        this.score = score;
        this.start = start;
        this.end = end;
    }

    public PeriodInstance(Map<String, Object> jsonProps) {
        Object scoreField = jsonProps.get("score");
        Object startField = jsonProps.get("start");
        Object endField = jsonProps.get("end");
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
    }

    public Double increaseScore(Double value) {
        score = score + value;
        return score;
    }

    @JsonProperty("score")
    public Double getScore() {
        return score;
    }

    @JsonProperty("score")
    public void setScore(Double score) {
        this.score = score;
    }

    @JsonProperty("start")
    public long getStart() {
        return start;
    }

    @JsonProperty("start")
    public void setStart(long start) {
        this.start = start;
    }

    @JsonProperty("end")
    public long getEnd() {
        return end;
    }

    @JsonProperty("end")
    public void setEnd(long end) {
        this.end = end;
    }

    @Override
    public String toString() {
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return String.format("[start: %s, end: %s, score: %s",
                formatter.format(new Date(start)),
                formatter.format(new Date(end)), score);
    }

    @JsonProperty("index")
    public long getIndex() {
        return index;
    }

    @JsonProperty("index")
    public void setIndex(long index) {
        this.index = index;
    }
}
