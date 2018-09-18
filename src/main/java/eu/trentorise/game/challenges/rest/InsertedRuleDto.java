package eu.trentorise.game.challenges.rest;

/**
 * Simple Dto mean to be used for inserting rules
 */
public class InsertedRuleDto {

    private String id;
    private String content;
    private String name;

    public InsertedRuleDto() {
    }

    public InsertedRuleDto(RuleDto rule) {
        this.id = rule.getId();
        this.content = rule.getContent();
        this.name = rule.getName();
    }

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

}
