package com.prismmedia.beeswax.customdatamacro.entity;


import com.fasterxml.jackson.databind.JsonNode;

public class Segments {

    private String id;

    private String name;

    public Segments(String id, String name, String value) {
        this.id = id;
        this.name = name;
        this.value = value;
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
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private String value;


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
}
