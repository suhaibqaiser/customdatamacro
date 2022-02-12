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
import java.util.HashMap;
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
        if(loaderService.getSegKeyMap() != null) {
            return loaderService.getSegKeyMap().values();
        } else {
            return null;
        }

    }

    @PostMapping("/resetlogs")
    @Consumes("application/text")
    @Produces("application/text")
    public String resetLogs(@RequestHeader("start-log") Boolean startLog, @RequestHeader("log-limit") Integer logLimit,
                            @RequestHeader("ip-address") String ipAddr) {
        try {
            lookupService.resetLog(startLog, logLimit, ipAddr);
            return "It worked";
        } catch (Exception e) {
            e.printStackTrace();
            return "did not worked";
        }
    }

    @PostMapping("/bidrequest")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public byte[] processBidRequestFromProto(@RequestHeader("beeswax-auth-secret") String headerSecret, @RequestBody byte[] body, HttpServletResponse response) throws IOException {
        if(headerSecret == null || headerSecret.isEmpty() || !headerSecret.contentEquals("98E4B46F8DE8EFD61EC76F88BFFE4BC9BA93D45C")) {
            response.setStatus(401);
            return null;
        }

        Augmentor.AugmentorRequest request = Augmentor.AugmentorRequest.parseFrom(body);

        Augmentor.AugmentorResponse macroResponse = lookupService.parseSegmentsFromProtoText(request.getBidRequest());

        if(macroResponse == null || macroResponse.getSegmentsList().isEmpty()) {
            response.setStatus(204);
            return null;
        } else {
            return macroResponse.toByteArray();
        }



    }
}
