package com.prismmedia.beeswax.customdatamacro.entity;


import com.fasterxml.jackson.databind.JsonNode;

public class Segments {

    private Integer id;

    private String key;

    private String name;

    private String value;

    private Advertiser advertiser;

    private String feedRowId;

    public Segments(Integer id, String key, String name, String value, Advertiser advertiser) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.value = value;
        this.advertiser = advertiser;
    }

    public Segments() {
    }

    public Segments(final JsonNode segment) {
        if(segment.get("id") != null) {
            this.id = Integer.parseInt(segment.get("id").textValue());
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
        if(value == null) {
            value = "";
        }
        this.value = value;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Advertiser getAdvertiser() {
        return advertiser;
    }

    public void setAdvertiser(Advertiser advertiser) {
        this.advertiser = advertiser;
    }

    public String getFeedRowId() {
        return feedRowId;
    }

    public void setFeedRowId(String feedRowId) {
        this.feedRowId = feedRowId;
    }
}
