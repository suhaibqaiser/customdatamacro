package com.prismmedia.beeswax.customdatamacro.entity;

public class Deal {

    private Integer id;

    private String inventorySourceKey;

    private String cpmOverride;

    private String dealIdentifier;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getInventorySourceKey() {
        return inventorySourceKey;
    }

    public void setInventorySourceKey(String inventorySourceKey) {
        this.inventorySourceKey = inventorySourceKey;
    }

    public String getCpmOverride() {
        return cpmOverride;
    }

    public void setCpmOverride(String cpmOverride) {
        this.cpmOverride = cpmOverride;
    }

    public String getDealIdentifier() {
        return dealIdentifier;
    }

    public void setDealIdentifier(String dealIdentifier) {
        this.dealIdentifier = dealIdentifier;
    }

    public long getCpmBidLong() {
        try {
            return Double.valueOf(Double.parseDouble(cpmOverride) * 1000).longValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
