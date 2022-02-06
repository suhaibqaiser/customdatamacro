package com.prismmedia.beeswax.customdatamacro.service;

import com.beeswax.augment.Augmentor;
import com.beeswax.openrtb.Openrtb;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.protobuf.InvalidProtocolBufferException;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LookupService {

    private static ConcurrentHashMap<String, Segments> segNameMap = null;

    private static ConcurrentHashMap<String, Segments> segValueMap = null;

    @Autowired
    private SegmentRepo segmentRepo;

    public LookupService() {
    }

    public ConcurrentHashMap<String, Segments> getSegValueMap() {
        if(segValueMap == null) {
            segValueMap = segmentRepo.fetchSegmentsValueMap();
        }
        return segValueMap;
    }

    public ConcurrentHashMap<String, Segments> getSegNameMap() {
        if(segNameMap == null) {
            segNameMap = segmentRepo.fetchSegmentsNameMap();
        }
        return segNameMap;
    }

    public List<Augmentor.AugmentorResponse.Segment> parseSegmentsFromProtoText(Openrtb.BidRequest bidRequest) throws InvalidProtocolBufferException {
       List<Augmentor.AugmentorResponse.Segment> segList = new ArrayList<Augmentor.AugmentorResponse.Segment>();
        Openrtb.BidRequest.User bidRequestUser = bidRequest.getUser();
        if(bidRequestUser.getDataCount() != 0) {
            List<Openrtb.BidRequest.Data.Segment> protoSegArray = bidRequestUser.getData(0).getSegmentList();
            if(protoSegArray != null) {
                for(Openrtb.BidRequest.Data.Segment segItem : protoSegArray) {
                    Augmentor.AugmentorResponse.Segment.Builder segBuilder = Augmentor.AugmentorResponse.Segment.newBuilder();
                    Segments segEntity = lookupSegmentFromDB(segItem);
                    if(segEntity != null) {
                        segBuilder.setId(segEntity.getKey());
                        segBuilder.setValue(segEntity.getValue());
                        segList.add(segBuilder.build());
                    }
                }
                if(segList.isEmpty()) {
                    segList.add(getEmptySegment());
                }
            }
        }
        return segList;

    }

    public Segments lookupSegmentFromDB(final Openrtb.BidRequest.Data.Segment segment) {
        Segments returnSegment = null;
        if(segment != null && getSegNameMap().containsKey(segment.getName())) {
            returnSegment = getSegNameMap().get(segment.getName());
        } else if(segment != null && getSegValueMap().containsKey(segment.getValue())) {
            returnSegment = getSegValueMap().get(segment.getValue());
        }
        return returnSegment;
    }

    public Augmentor.AugmentorResponse.Segment getEmptySegment() {
        Augmentor.AugmentorResponse.Segment.Builder segBuilder = Augmentor.AugmentorResponse.Segment.newBuilder();
        segBuilder.setId("0");
        segBuilder.setValue("0");
        return segBuilder.build();
    }

    public List<Segments> parseSegmentsFromJson(final String bidRequest) throws IOException {
        List<Segments> segmentArray = null;

        if(bidRequest != null && !bidRequest.isEmpty()) {

            ObjectMapper mapper = new ObjectMapper();
            JsonParser parser = mapper.createParser(bidRequest);
            JsonNode bidRequestParent = parser.readValueAsTree();
            if(bidRequestParent != null && !bidRequestParent.isEmpty()) {
                try {
                    JsonNode user = bidRequestParent.get("user");
                    if(user != null && user.get("data").isArray()) {
                        for (JsonNode dataItem : user.withArray("data")) {
                            if (dataItem.get("segment").isArray()) {
                                segmentArray = parseSegmentNodes(mapper, dataItem.get("segment"));
                            }
                        }
                    } else {
                        JsonNode segment = user.get("data").get("segment");
                        segmentArray = parseSegmentNodes(mapper, segment);
                    }
                } catch(Exception e) {
                    segmentArray = new ArrayList<Segments>();
                    e.printStackTrace();
                }
            }
        }
        return segmentArray;
    }

    private List<Segments> parseSegmentNodes(final ObjectMapper mapper, final JsonNode segments) throws IOException {
        List<Segments> segmentArray = null;
        if (segments.isArray()) {
            ObjectReader reader = mapper.readerFor(new TypeReference<List<Segments>>() {
            });
            segmentArray = reader.readValue(segments);
        } else {
            segmentArray = new ArrayList<Segments>();
            segmentArray.add(new Segments(segments));
        }
        return segmentArray;
    }

}
