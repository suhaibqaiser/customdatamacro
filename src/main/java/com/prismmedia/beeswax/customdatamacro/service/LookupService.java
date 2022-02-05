package com.prismmedia.beeswax.customdatamacro.service;

import com.beeswax.augment.Augmentor;
import com.beeswax.openrtb.Openrtb;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.protobuf.*;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.Segment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class LookupService {

    private static List<Segments> segArray = null;

    @Autowired
    private SegmentRepo segmentRepo;

    public LookupService() {
    }

    public List<Segments> getSegArray() {
        if(segArray == null) {
            segArray = segmentRepo.getSegments();
        }
        return segArray;
    }

    public List<Augmentor.AugmentorResponse.Segment> parseSegmentsFromProtoText(Openrtb.BidRequest bidRequest) throws InvalidProtocolBufferException {
       List<Augmentor.AugmentorResponse.Segment> segList = new ArrayList<Augmentor.AugmentorResponse.Segment>();
        Openrtb.BidRequest.User bidRequestUser = bidRequest.getUser();
        if(bidRequestUser.getDataCount() != 0) {
            List<Openrtb.BidRequest.Data.Segment> protoSegArray = bidRequestUser.getData(0).getSegmentList();
            if(protoSegArray != null) {
                for(Openrtb.BidRequest.Data.Segment segItem : protoSegArray) {
                    Augmentor.AugmentorResponse.Segment.Builder segBuilder = Augmentor.AugmentorResponse.Segment.newBuilder();
                    segBuilder.setId(segItem.getId());
                    segBuilder.setValue(segItem.getValue());
                    segList.add(segBuilder.build());
                }
            }
        }
        return segList;


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
