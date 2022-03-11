package com.leon.detonator.Base;

import org.json.JSONException;
import org.json.JSONObject;

public interface BaseJSONBean {
    JSONObject toJSON() throws JSONException;

    void fromJSON(JSONObject jsonObject) throws JSONException;
}
