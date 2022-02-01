package com.prismmedia.beeswax.customdatamacro.controllers;

import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import com.prismmedia.beeswax.customdatamacro.service.SegmentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping("/augment")
public class SegmentController {

    @Autowired
    private SegmentRepo segmentRepo;

    public SegmentController() {

    }

    @GetMapping("/segments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Segments> getSegments() {
        return segmentRepo.getSegments();
    }
}
