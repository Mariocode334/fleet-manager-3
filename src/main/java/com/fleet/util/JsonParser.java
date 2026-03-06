package com.fleet.util;

import com.fleet.model.DroneState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class JsonParser {
    private static JsonParser instance;
    private final Gson gson;

    private JsonParser() {
        this.gson = new GsonBuilder().create();
    }

    public static synchronized JsonParser getInstance() {
        if (instance == null) {
            instance = new JsonParser();
        }
        return instance;
    }

    public DroneState parseMessage(String json) throws JsonSyntaxException {
        return gson.fromJson(json, DroneState.class);
    }

    public String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public boolean isValidJson(String json) {
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
}
