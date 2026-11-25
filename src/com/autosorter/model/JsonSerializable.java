package com.autosorter.model;

import java.util.Map;

/** Contract for any object that can be rendered as a JSON object (polymorphic serialisation). */
public interface JsonSerializable {
    Map<String, Object> toJson();
}
