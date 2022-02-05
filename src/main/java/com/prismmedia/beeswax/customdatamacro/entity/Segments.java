package com.prismmedia.beeswax.customdatamacro.entity;


import com.fasterxml.jackson.databind.JsonNode;

public class Segments {

    private String id;

    private String key;

    private String name;

    private String value;

    private String advertiserId;

    public Segments(String id, String key, String name, String value, String advertiserId) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.value = value;
        this.advertiserId = advertiserId;
    }

    public Segments() {
    }

    public Segments(final JsonNode segment) {
        if(segment.get("id") != null) {
            this.id = segment.get("id").textValue();
        }
        if(segment.get("name") != null) {
            this.name = segment.get("name").textValue();
        }
        if(segment.get("value") != null) {
            this.value = segment.get("value").textValue();
        }
        if(segment.get("key") != null) {
            this.key = segment.get("key").textValue();
        }
        if(segment.get("advertiserId") != null) {
            this.advertiserId = segment.get("advertiserId").textValue();
        }
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdvertiserId() {
        return advertiserId;
    }

    public void setAdvertiserId(String advertiserId) {
        this.advertiserId = advertiserId;
    }
}
