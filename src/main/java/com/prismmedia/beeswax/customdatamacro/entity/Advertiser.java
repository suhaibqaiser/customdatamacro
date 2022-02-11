package com.prismmedia.beeswax.customdatamacro.entity;

public class Advertiser {

    private Integer id;

    private String name;


    public Advertiser(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Advertiser() {
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
}
