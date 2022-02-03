package com.prismmedia.beeswax.customdatamacro.service;

import com.beeswax.openrtb.Openrtb;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import com.prismmedia.beeswax.customdatamacro.util.ProtoJsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
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

    public List<Segments> parseSegmentsFromProtoText(final String bidRequestProtoText) {
        Openrtb.BidRequest bidRequest = ProtoJsonUtil.parseFromProtoBuf(bidRequestProtoText);
        List<Openrtb.BidRequest.Data.Segment> protoSegArray = bidRequest.getUser().getData(0).getSegmentList();
        List<Segments> segList = new ArrayList<Segments>();
        for(Openrtb.BidRequest.Data.Segment segItem : protoSegArray) {
            Segments seg = new Segments();
            seg.setId(segItem.getId());
            seg.setName(segItem.getName());
            seg.setValue(segItem.getValue());
            segList.add(seg);
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
