package com.prismmedia.beeswax.customdatamacro.controllers;

import com.beeswax.augment.Augmentor;
import com.beeswax.openrtb.Openrtb;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import com.prismmedia.beeswax.customdatamacro.service.LookupService;
import com.prismmedia.beeswax.customdatamacro.service.SegmentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/augment")
public class SegmentController {

    @Autowired
    private SegmentRepo segmentRepo;

    @Autowired
    private LookupService lookupService;

    public SegmentController() {

    }

    @GetMapping("/segments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Segments> getSegments() {
        return segmentRepo.getSegments();
    }

    @PostMapping("/bidrequestjson")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Segments> processBidRequestFromJson(@RequestBody String bidRequest) throws IOException {
        return lookupService.parseSegmentsFromJson(bidRequest);

    }

    @PostMapping("/bidrequestproto")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public byte[] processBidRequestFromProto(@RequestBody byte[] body) throws IOException {
        Augmentor.AugmentorRequest request = Augmentor.AugmentorRequest.parseFrom(body);
        //System.out.println(bidRequest.toString());
        List<Augmentor.AugmentorResponse.Segment> segList = lookupService.parseSegmentsFromProtoText(request.getBidRequest());
        Augmentor.AugmentorResponse.Builder responseBuilder = Augmentor.AugmentorResponse.newBuilder();
        responseBuilder.addAllSegments(segList);
        return responseBuilder.build().toByteArray();


    }
}
