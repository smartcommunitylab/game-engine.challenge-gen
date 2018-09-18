package eu.trentorise.game.challenges.rest;

import java.util.Map;

/**
 * Simple Dto mean to be used for inserting rules
 */
public class RuleDto {

    private String id;
    private String content;
    private String name;
    private Map<String, Map<String, Object>> customData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCustomData(Map<String, Map<String, Object>> customData) {
        this.customData = customData;
    }

    public Map<String, Map<String, Object>> getCustomData() {
        return customData;
    }

}
