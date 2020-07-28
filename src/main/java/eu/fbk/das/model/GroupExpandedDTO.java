package eu.fbk.das.model;

import eu.fbk.das.utils.Pair;
import it.smartcommunitylab.model.GroupChallengeDTO;

import org.threeten.bp.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static eu.fbk.das.GamificationEngineRestFacade.dateToOffset;
import static eu.fbk.das.GamificationEngineRestFacade.getDates;

public class GroupExpandedDTO extends GroupChallengeDTO {

    private final Map<String, Object> info;

    public GroupExpandedDTO() {
        super();
        info = new HashMap<>();
    }

    public void setInfo(String k, Object v) {
        info.put(k, v);
    }

    public Object getInfo(String k) {
        return info.get(k);
    }

    public void setStart(Date dt) {
        setStart(dateToOffset(dt));
    }

    public void setEnd(Date dt) {
        setEnd(dateToOffset(dt));
    }

    public void setDates(Object start, Object duration) {
        Pair<Date, Date> p = getDates(start, duration);
        setStart(p.getFirst());
        setEnd(p.getSecond());
    }
}
