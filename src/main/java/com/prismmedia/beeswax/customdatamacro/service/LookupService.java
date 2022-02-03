package com.prismmedia.beeswax.customdatamacro.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
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

    public List<Segments> parseSegments(final String bidRequest) throws IOException {
        List<Segments> segmentArray = null;
        if(bidRequest != null && !bidRequest.isEmpty()) {

            ObjectMapper mapper = new ObjectMapper();
            JsonParser parser = mapper.createParser(bidRequest);
            JsonNode bidRequestParent = parser.readValueAsTree();
            if(bidRequestParent != null && !bidRequestParent.isEmpty()) {
                try {
                    ArrayNode segments = bidRequestParent.get("user").get("data").get(0).withArray("segment");
                    ObjectReader reader = mapper.readerFor(new TypeReference<List<Segments>>() {
                    });
                    segmentArray = reader.readValue(segments);

                } catch(Exception e) {
                    segmentArray = new ArrayList<Segments>();
                }
            }
        }
        return segmentArray;
    }
}
