package eu.fbk.das.model;

import it.smartcommunitylab.model.ChallengeAssignmentDTO;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import static eu.fbk.das.GamificationEngineRestFacade.jodaToOffset;

public class ChallengeExpandedDTO extends ChallengeAssignmentDTO {

    protected Map<String, Object> info;

    public ChallengeExpandedDTO() {
        info = new HashMap<>();
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

    public void setStart(DateTime dt) {
        setStart(jodaToOffset(dt));
    }
}
