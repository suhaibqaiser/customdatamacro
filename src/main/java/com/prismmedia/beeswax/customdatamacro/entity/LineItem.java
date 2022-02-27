package com.prismmedia.beeswax.customdatamacro.entity;

import java.util.ArrayList;
import java.util.List;

public class LineItem {

    private Integer id;

    private Integer advertiserId;

    private Boolean isActive;

    private Float budget;

    private String cpmBid;

    private List<Creative> creativeList;

    private Integer targetExpressionId;

    private String biddingStrategy;

    public String getBiddingStrategy() {
        return biddingStrategy;
    }

    public void setBiddingStrategy(String biddingStrategy) {
        this.biddingStrategy = biddingStrategy;
    }

    public Integer getTargetExpressionId() {
        return targetExpressionId;
    }

    public String getCpmBid() {
        return cpmBid;
    }

    public void setCpmBid(String cpmBid) {
        this.cpmBid = cpmBid;
    }
    public void setTargetExpressionId(Integer targetExpressionId) {
        this.targetExpressionId = targetExpressionId;
    }

    public LineItem() {
        creativeList = new ArrayList<Creative>();
    }

    public List<Creative> getCreativeList() {
        return creativeList;
    }

    public void setCreativeList(List<Creative> creativeList) {
        this.creativeList = creativeList;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAdvertiserId() {
        return advertiserId;
    }

    public void setAdvertiserId(Integer advertiserId) {
        this.advertiserId = advertiserId;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Float getBudget() {
        return budget;
    }

    public void setBudget(Float budget) {
        this.budget = budget;
    }

    public long getCpmBidLong() {
        try {
            return Long.parseLong(cpmBid);
        } catch (Exception e) {
            return 0;
        }
    }
}
