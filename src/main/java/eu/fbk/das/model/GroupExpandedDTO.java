package eu.fbk.das.model;

import static eu.fbk.das.GamificationEngineRestFacade.getDates;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import eu.fbk.das.utils.Pair;
import it.smartcommunitylab.model.ext.GroupChallengeDTO;

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

    public void setDates(Object start, Object duration) {
        Pair<Date, Date> p = getDates(start, duration);
        setStart(p.getFirst());
        setEnd(p.getSecond());
    }
}
