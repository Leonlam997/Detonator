package com.leon.detonator.base;

import org.json.JSONException;
import org.json.JSONObject;

public interface BaseJSONBean {
    JSONObject toJSON() throws JSONException;

    void fromJSON(JSONObject jsonObject) throws JSONException;
}
