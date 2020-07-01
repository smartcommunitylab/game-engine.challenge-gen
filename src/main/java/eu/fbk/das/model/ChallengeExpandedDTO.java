package eu.fbk.das.model;

import it.smartcommunitylab.model.ChallengeAssignmentDTO;
import org.joda.time.DateTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.Period;
import org.threeten.bp.format.DateTimeFormatter;


import java.util.*;

import static eu.fbk.das.GamificationEngineRestFacade.jodaToOffset;
import static eu.fbk.das.rs.utils.Utils.formatDateTime;
import static eu.fbk.das.rs.utils.Utils.p;

public class ChallengeExpandedDTO extends ChallengeAssignmentDTO {

    protected Map<String, Object> info;

    public ChallengeExpandedDTO() {
        super();
        info = new HashMap<>();
        setData(new HashMap<>());
    }

    public void setInfo(String k, Object v) {
        info.put(k, v);
    }

    public void setData(String k, Object v) {
        Map<String, Object> d = (Map<String, Object>) this.getData();
        d.put(k, v);
        setData(d);
    }

    public void delData(String k) {
        Map<String, Object> d = (Map<String, Object>) this.getData();
        d.remove(k);
        setData(d);
    }

    public Object getData(String k) {
        Map<String, Object> d = (Map<String, Object>) this.getData();
        return d.get(k);
    }

    public Vector<Object> getDisplayData() {
        Vector<Object> result = new Vector<>();
        result.add(i(getInfo("player")));
        result.add(i(getInfo("playerLevel")));
        result.add(getInfo("id"));
        result.add(getInfo("experiment"));
        result.add(getModelName());
        result.add(getData("counterName"));
        result.add(m(getData("baseline")));
        result.add(m(getData("target")));
        result.add(m(getInfo("improvement")));
        result.add(m(getData("difficulty")));
        result.add(d(getData("bonusScore")));
        result.add(getState());
        result.add(getPriority());

        result.add(formatDateTime(new DateTime(getStart())));
        result.add(formatDateTime(new DateTime(getEnd())));

        result.add(isHide());

        return result;
    }

    public Object getInfo(String k) {
        return info.get(k);
    }

    private Object d(Object s) {
        return Double.valueOf(String.valueOf(s));
    }

    private Object i(Object s) {
        return Integer.valueOf(String.valueOf(s));
    }

    public Vector<Object> getWriteData() {
        Vector<Object> result = new Vector<>();
        result.addAll(getDisplayData());
        return result;
    }

    private Double m(Object o) {

        try {
            String s = String.valueOf(o);
            if ("null".equals(s))
                return 0.0;
            Double d = Double.valueOf(s);
            d = Math.ceil(d * 100.0) / 100.0;
            return d;
        } catch (IllegalFormatConversionException ex) {
            p(ex.getMessage());
        }

        return -1.0;
    }


    public String printData() {
        return getWriteData().toString().replace("[", "").replace("]", "");
    }

    public void setEnd(Date date) {
        setEnd(new DateTime(date));
    }


    public boolean hasData(String k) {
        Map<String, Object> d = (Map<String, Object>) this.getData();
        return d.containsKey(k);
    }

    public void setStart(DateTime dt) {
        setStart(jodaToOffset(dt));
    }

    private void setEnd(DateTime dt) {
        setEnd(jodaToOffset(dt));
    }

    public void setDates(Object start, Object duration) {
        if (start == null || duration == null) {
            p("NULL START / DURATION!");
            return;
        }

        try {
            OffsetDateTime st = OffsetDateTime.parse((CharSequence) start, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            setStart(st);
            String periodAsIsoFormat = "P" + ((String) duration).toUpperCase();
            Period p = Period.parse(periodAsIsoFormat);
            setEnd(st.plus(p));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStart(Date date) {
        setStart(new DateTime(date));
    }
}
