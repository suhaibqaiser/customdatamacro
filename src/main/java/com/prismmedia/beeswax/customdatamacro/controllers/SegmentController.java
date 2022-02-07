package com.prismmedia.beeswax.customdatamacro.controllers;

import com.beeswax.augment.Augmentor;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import com.prismmedia.beeswax.customdatamacro.service.BeeswaxLoaderService;
import com.prismmedia.beeswax.customdatamacro.service.LookupService;
import com.prismmedia.beeswax.customdatamacro.service.SegmentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/augment")
public class SegmentController {

    @Autowired
    private SegmentRepo segmentRepo;

    @Autowired
    private LookupService lookupService;

    @Autowired
    private BeeswaxLoaderService loaderService;

    public SegmentController() {

    }

    @GetMapping("/test")
    public String test() {
        return "system works";
    }

    @GetMapping("/segments")
    @Consumes("application/json")
    @Produces("application/json")
    public Collection<Segments> fetchBidRequestFromJson() {
        return loaderService.getSegNameMap().values();
    }

    @PostMapping("/bidrequest")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public byte[] processBidRequestFromProto(@RequestBody byte[] body, HttpServletResponse response) throws IOException {
        Augmentor.AugmentorRequest request = Augmentor.AugmentorRequest.parseFrom(body);
        List<Augmentor.AugmentorResponse.Segment> segList = lookupService.parseSegmentsFromProtoText(request.getBidRequest());
        Augmentor.AugmentorResponse.Builder responseBuilder = Augmentor.AugmentorResponse.newBuilder();
        responseBuilder.addAllSegments(segList);
        if(segList.isEmpty()) {
            response.setStatus(204);
        }
        return responseBuilder.build().toByteArray();


    }
}
