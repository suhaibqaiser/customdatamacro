package com.prismmedia.beeswax.customdatamacro.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/augment")
public class SegmentController {

    public SegmentController() {

    }

    @GetMapping("/segments")
    public String getSegments() {
        return "Here are new segments";
    }
}
