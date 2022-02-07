package com.prismmedia.beeswax.customdatamacro.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BeeswaxLoaderService {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static ConcurrentHashMap<String, Segments> segNameMap = null;

    private static ConcurrentHashMap<String, Segments> segValueMap = null;

    @Autowired
    private SegmentRepo segRepo;

    @Scheduled(cron = "*/5 * * * * ?")
    public void task() {
        System.out.println("Scheduler (cron expression) task with duration : " + sdf.format(new Date()));
        try {
            loadSegmentTree();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadSegmentTree() {
        ConcurrentHashMap<String, Segments> nameMap = new ConcurrentHashMap<String, Segments>();
        ConcurrentHashMap<String, Segments> valueMap = new ConcurrentHashMap<String, Segments>();
        ObjectMapper mapper = new ObjectMapper();
        RestTemplateWithCookies restTemplate = new RestTemplateWithCookies();
        Integer rowCount = 1;
        try {
            System.out.println("Loading from Beeswax System start time = " + sdf.format(new Date()));
            String beeswaxUrl = "https://prism.api.beeswax.com/rest/v2/ref/segment-tree";
            String authUrl = "https://prism.api.beeswax.com/rest/authenticate";
            boolean next = true;
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("Content-Type", "application/json");
            HashMap<String, String> authBody = new HashMap<String, String>();
            authBody.put("email", "george.groves@prismmedia.io");
            authBody.put("password", "x&f$pgFH43@gDJqs");
            authBody.put("keep_logged_in", "true");
            String body = mapper.writeValueAsString(authBody);
            HttpEntity<String> httpEntity = new HttpEntity <String> (body, httpHeaders);
            String authResponse = restTemplate.postForObject(authUrl, httpEntity, String.class);
            httpHeaders = new HttpHeaders();
            httpHeaders.set("Content-Type", "application/json");
            httpEntity = new HttpEntity<String>("", httpHeaders);
            segRepo.deleteAllFromSegments();

            while(beeswaxUrl != null && !beeswaxUrl.isEmpty()) {
                System.out.println("Making REST call to Beeswax = " + beeswaxUrl);
                String response = restTemplate.getForObject(beeswaxUrl, String.class);
                JsonNode node = mapper.createParser(response).readValueAsTree();
                beeswaxUrl = node.get("next").textValue();

                ArrayNode results = node.withArray("results");
                if(results != null && !results.isEmpty()) {
                    for(JsonNode itemNode : results) {
                        Segments segments = new Segments();
                        segments.setKey(itemNode.get("key").textValue());
                        String type = itemNode.get("type").textValue();
                        if(type != null && type.equalsIgnoreCase("segment")) {
                            String[] parsedIds = segments.getKey().split("prism-");
                            if(parsedIds.length > 0) {
                                segments.setId(Integer.parseInt(parsedIds[1]));
                            } else {
                                segments.setId(rowCount);
                            }
                            segments.setName(itemNode.get("name").textValue());
                            segments.setValue(itemNode.get("alternative_id").textValue());
                            segments.setAdvertiserId(itemNode.get("advertiser_id").asInt());
                            segRepo.save(segments);
                            nameMap.put(segments.getName(), segments);
                            valueMap.put(segments.getValue(), segments);
                            rowCount++;
                        }

                    }
                }
            }
            if(!valueMap.isEmpty() && !nameMap.isEmpty()) {
                segNameMap = nameMap;
                segValueMap = valueMap;
                System.out.println("Replaced values of Segment Maps in memory from Beeswax system");
            }
            System.out.println("Parsing completed with total count " +  rowCount + " time = " + sdf.format(new Date()));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error occurred in parsing of Segments from Beeswax ...");
        }


    }


    public ConcurrentHashMap<String, Segments> getSegValueMap() {
        if(segValueMap == null) {
            new Thread(() -> {
                loadSegmentTree();
            }).start();
            segValueMap = segRepo.fetchSegmentsValueMap();
        }
        return segValueMap;
    }

    public ConcurrentHashMap<String, Segments> getSegNameMap() {
        if(segNameMap == null) {
            new Thread(() -> {
                loadSegmentTree();
            }).start();
            segNameMap = segRepo.fetchSegmentsNameMap();
        }
        return segNameMap;
    }
}
