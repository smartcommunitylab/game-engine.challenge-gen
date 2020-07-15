package eu.fbk.das.model;

import it.smartcommunitylab.model.GroupChallengeDTO;
import java.util.HashMap;
import java.util.Map;

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
}
