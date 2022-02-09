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

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LookupService {

    @Autowired
    private BeeswaxLoaderService loaderService;

    @Autowired
    private SegmentRepo segmentRepo;

    public LookupService() {
    }

    public List<Augmentor.AugmentorResponse.Segment> parseSegmentsFromProtoText(Openrtb.BidRequest bidRequest) throws InvalidProtocolBufferException {
       List<Augmentor.AugmentorResponse.Segment> segList = new ArrayList<Augmentor.AugmentorResponse.Segment>();
        Openrtb.BidRequest.User bidRequestUser = bidRequest.getUser();
        Segments macroResponse = new Segments();
        if(bidRequestUser.getDataCount() != 0) {
            List<Openrtb.BidRequest.Data.Segment> protoSegArray = bidRequestUser.getData(0).getSegmentList();
            if(protoSegArray != null) {
                for(Openrtb.BidRequest.Data.Segment segItem : protoSegArray) {
                    Augmentor.AugmentorResponse.Segment.Builder segBuilder = Augmentor.AugmentorResponse.Segment.newBuilder();
                    Segments segEntity = lookupSegmentFromDB(segItem);
                    if(segEntity != null) {
                        if(macroResponse.getId() < segEntity.getId()) {
                            macroResponse = segEntity;
                        }
                        segBuilder.setId(segEntity.getKey());
                        segBuilder.setValue(segEntity.getValue());
                        System.out.println("*** Found entry for ".concat(segItem.getName()).concat(" with auction id").concat(bidRequest.getId()));
                        segList.add(segBuilder.build());
                    }
                }
                if(segList.isEmpty()) {
                    System.out.println(" ### No entry found for bid request ".concat(bidRequest.getId()));
                    segList.add(getEmptySegment());
                }
            }
        }

        return segList;

    }

    public Segments lookupSegmentFromDB(final Openrtb.BidRequest.Data.Segment segment) {
        Segments returnSegment = null;
        if(segment != null && loaderService.getSegNameMap().containsKey(segment.getName())) {
            returnSegment = loaderService.getSegNameMap().get(segment.getName());
        } else if(segment != null && loaderService.getSegNameMap().containsKey(segment.getValue())) {
            returnSegment = loaderService.getSegNameMap().get(segment.getValue());
        } else if(segment != null && loaderService.getSegValueMap().containsKey(segment.getName())) {
            returnSegment = loaderService.getSegValueMap().get(segment.getName());
        } else if(segment != null && loaderService.getSegValueMap().containsKey(segment.getValue())) {
            returnSegment = loaderService.getSegValueMap().get(segment.getValue());
        }
        return returnSegment;
    }

    public Augmentor.AugmentorResponse.Segment getEmptySegment() {
        Augmentor.AugmentorResponse.Segment.Builder segBuilder = Augmentor.AugmentorResponse.Segment.newBuilder();
        segBuilder.setId("0");
        segBuilder.setValue("0");
        return segBuilder.build();
    }

    @Deprecated
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

    private List<Segments> parseSegmentNodes(final ObjectMapper mapper, @NotNull final JsonNode segments) throws IOException {
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
