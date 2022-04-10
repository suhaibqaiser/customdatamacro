package com.prismmedia.beeswax.customdatamacro.entity;

public class OpenSheet {

    private String id;

    private String segmentId;

    private String advertiserId;

    private String alternativeId;

    private String feedRowId;

    public OpenSheet() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public Integer getSegmentIdIntValue() {
        try {
            return Integer.parseInt(segmentId);
        } catch (Exception e) {
            return 0;
        }
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public String getAdvertiserId() {
        return advertiserId;
    }

    public Integer getAdvertiserIdIntValue() {
        try {
            return Integer.parseInt(advertiserId);
        } catch (Exception e) {
            return 0;
        }
    }

    public void setAdvertiserId(String advertiserId) {
        this.advertiserId = advertiserId;
    }

    public String getAlternativeId() {
        return alternativeId;
    }

    public void setAlternativeId(String alternativeId) {
        this.alternativeId = alternativeId;
    }

    public String getFeedRowId() {
        return feedRowId;
    }

    public void setFeedRowId(String feedRowId) {
        this.feedRowId = feedRowId;
    }
}
