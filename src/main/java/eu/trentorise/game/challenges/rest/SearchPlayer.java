package eu.trentorise.game.challenges.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple Dto mean to be used for inserting rules
 */
public class SearchPlayer extends RestParam {

    private Map<String, Object> rawQuery;
    private String query;
    private Map<String, Object> projection;
    private List<String> include;

    public SearchPlayer() {

        rawQuery = new HashMap<String, Object>();
        rawQuery.put("query", query);
        projection = new HashMap<String, Object>();
        include = new ArrayList<>();
        projection.put("include", include);
        include.add("playerId");
        rawQuery.put("projection", projection);

        data = new HashMap<String, Object>();
        data.put("rawQuery", rawQuery);
    }

}
