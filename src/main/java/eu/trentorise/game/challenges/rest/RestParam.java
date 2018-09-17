package eu.trentorise.game.challenges.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

public class RestParam {

    public static GsonBuilder builder = new GsonBuilder();

    protected Map<String, Object> data;

    public String json() {
        Gson gson = builder.create();
        return gson.toJson(data);
    }
}
