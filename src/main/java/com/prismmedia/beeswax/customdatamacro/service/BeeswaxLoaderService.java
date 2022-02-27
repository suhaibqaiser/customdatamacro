package com.prismmedia.beeswax.customdatamacro.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prismmedia.beeswax.customdatamacro.entity.Advertiser;
import com.prismmedia.beeswax.customdatamacro.entity.Creative;
import com.prismmedia.beeswax.customdatamacro.entity.LineItem;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sound.sampled.Line;
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

    private static ConcurrentHashMap<String, Segments> segKeyMap = null;

    private ObjectMapper mapper = new ObjectMapper();

    private RestTemplateWithCookies restTemplate = new RestTemplateWithCookies();

    @Autowired
    private SegmentRepo segRepo;

    @Scheduled(cron = "0 0/10 * * * ?")
    public void task() {
        System.out.println("Scheduler (cron expression) task with duration : " + sdf.format(new Date()));
        try {
            loadSegmentTree();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadBiddingStrategy() throws IOException {
        authenticate();
    }

    public void loadSegmentTree() {
        ConcurrentHashMap<String, Segments> keyMap = new ConcurrentHashMap<String, Segments>();
        ConcurrentHashMap<Integer, Advertiser> advertiserMap = null;
        ConcurrentHashMap<String, String> feedRowMap = null;
        Integer rowCount = 1;
        try {
            System.out.println("Loading from Beeswax System start time = " + sdf.format(new Date()));
            authenticate();
            advertiserMap = loadAdvertisersFromBeeswax();
            feedRowMap = loadFeedRowIdMap();
            String beeswaxUrl = "https://prism.api.beeswax.com/rest/v2/ref/segment-tree";

            boolean next = true;

            //segRepo.deleteAllFromSegments();

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
                            segments.setAdvertiser(advertiserMap.get(itemNode.get("advertiser_id").asInt()));
                            segments.setFeedRowId(feedRowMap.get(segments.getId().toString()));
                            //segRepo.save(segments);
                            keyMap.put(segments.getKey(), segments);
                            rowCount++;
                        }

                    }
                }
            }
            if(!keyMap.isEmpty()) {
                segKeyMap = keyMap;
                System.out.println("Populating Line Items ... this may take time");
                populateActiveLineItems();
                System.out.println("Replaced values of Segment Maps in memory from Beeswax system");
            }
            System.out.println("Parsing completed with total count " +  rowCount + " time = " + sdf.format(new Date()));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error occurred in parsing of Segments from Beeswax ...");
        }


    }

    public void authenticate() throws JsonProcessingException {
        String authUrl = "https://prism.api.beeswax.com/rest/authenticate";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "application/json");
        HashMap<String, String> authBody = new HashMap<String, String>();
        authBody.put("email", "george.groves@prismmedia.io");
        authBody.put("password", "x&f$pgFH43@gDJqs");
        String body = mapper.writeValueAsString(authBody);
        HttpEntity<String> httpEntity = new HttpEntity <String> (body, httpHeaders);
        restTemplate.postForObject(authUrl, httpEntity, String.class);
    }

    public void populateActiveLineItems() throws IOException {
        final String prismTestStrategy = "PRISM_TEST_STRATEGY";
        String biddingStrategyUrl = "https://prism.api.beeswax.com/rest/line_item?active=1";
        String response = restTemplate.getForObject(biddingStrategyUrl, String.class);
        JsonNode node = mapper.createParser(response).readValueAsTree();
        ArrayNode results = node.withArray("payload");
        if(results != null && !results.isEmpty()) {
            for(JsonNode itemNode : results) {
                LineItem lineItem = new LineItem();
                lineItem.setId(itemNode.get("line_item_id").intValue());
                lineItem.setBudget(itemNode.get("line_item_budget").floatValue());
                lineItem.setActive(itemNode.get("active").asBoolean());
                lineItem.setTargetExpressionId(itemNode.get("targeting_expression_id").intValue());
                lineItem.setBiddingStrategy(itemNode.get("bidding").get("bidding_strategy").textValue());
                if(prismTestStrategy.equalsIgnoreCase(lineItem.getBiddingStrategy()) && lineItem.getActive()) {
                    populateCreatives(lineItem);
                    List<String> segItemList = getSegmentIds(lineItem.getTargetExpressionId().toString());
                    System.out.println("Line Item: " + lineItem.getId().toString() + " - " + segItemList);
                    if(segItemList != null && !segItemList.isEmpty()) {
                        for(String segId : segItemList) {
                            Segments segment = segKeyMap.get(segId);
                            if(segment != null) {
                                segment.getLineItemList().add(lineItem);
                            }
                        }
                    }

                }
            }

        }
    }



    public void populateCreatives(final LineItem lineItem) throws IOException {
        String biddingStrategyUrl = "https://prism.api.beeswax.com/rest/creative_line_item?line_item_id=" + lineItem.getId().toString();
        String response = restTemplate.getForObject(biddingStrategyUrl, String.class);
        JsonNode node = mapper.createParser(response).readValueAsTree();
        ArrayNode results = node.withArray("payload");
        if(results != null && !results.isEmpty()) {
            for (JsonNode itemNode : results) {
                if(itemNode.get("active").booleanValue()) {
                    Integer creativeId = itemNode.get("creative_id").intValue();
                    Creative creative = getCreative(creativeId.toString());
                    if(creative != null) {
                        lineItem.getCreativeList().add(creative);
                    }
                }
            }
        }
    }

    public Creative getCreative(final String creativeId) throws IOException {
        Creative creative = null;
        String biddingStrategyUrl = "https://prism.api.beeswax.com/rest/creative?creative_id=" + creativeId;
        System.out.println("Looking up Creative with url: " + biddingStrategyUrl);
        String response = restTemplate.getForObject(biddingStrategyUrl, String.class);
        JsonNode node = mapper.createParser(response).readValueAsTree();
        ArrayNode results = node.withArray("payload");
        if(results != null && !results.isEmpty()) {
            for (JsonNode itemNode : results) {
                if(itemNode.get("active").booleanValue()) {
                    creative = new Creative();
                    creative.setId(itemNode.get("creative_id").intValue());
                    creative.setName(itemNode.get("creative_name").toString());
                    creative.setHeight(itemNode.get("height").intValue());
                    creative.setWidth(itemNode.get("width").intValue());
                    creative.setType(itemNode.get("creative_type").intValue());
                    break;
                }
            }
        }
        return creative;
    }

    public List<String> getSegmentIds(final String targetExpressionId) throws IOException {
        List<String> segIdList = new ArrayList<String>();
        try {

            String biddingStrategyUrl = "https://prism.api.beeswax.com/rest/v2/targeting-expressions?id=" + targetExpressionId;
            String response = restTemplate.getForObject(biddingStrategyUrl, String.class);
            JsonNode node = mapper.createParser(response).readValueAsTree();
            ArrayNode results = node.withArray("results");

            if(results != null && !results.isEmpty()) {

                for(JsonNode itemNode : results) {
                    if(itemNode.findPath("modules") != null && itemNode.findPath("modules").findPath("user") != null) {
                        ArrayNode segments = itemNode.get("modules").get("user").get("all").get("segment").withArray("any");
                        if(segments != null) {
                            for(JsonNode segItem : segments) {
                                segIdList.add(segItem.get("value").textValue());
                            }
                        }
                    }

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return segIdList;
    }

    public ConcurrentHashMap<Integer, Advertiser> loadAdvertisersFromBeeswax() throws IOException {
        ConcurrentHashMap<Integer, Advertiser> advertiserMap = new ConcurrentHashMap<Integer, Advertiser>();
        String beeswaxUrl = "https://prism.api.beeswax.com/rest/advertiser";
        System.out.println("Making Advertiser REST call to Beeswax = " + beeswaxUrl);
        String response = restTemplate.getForObject(beeswaxUrl, String.class);
        JsonNode node = mapper.createParser(response).readValueAsTree();
        ArrayNode results = node.withArray("payload");
        if(results != null && !results.isEmpty()) {
            for(JsonNode itemNode : results) {
                Advertiser advertiser = new Advertiser();
                advertiser.setId(itemNode.get("advertiser_id").intValue());
                advertiser.setName(itemNode.get("advertiser_name").textValue());
                advertiserMap.put(advertiser.getId(), advertiser);
            }
        }
        return advertiserMap;
    }

    public ConcurrentHashMap<String, String> loadFeedRowIdMap() throws IOException {
        ConcurrentHashMap<String, String> feedMap = new ConcurrentHashMap<String, String>();
        String openSheetUrl = "https://opensheet.elk.sh/1t-bWAQdSeiUJA37gjcyFuXEgyyY1GSw8tRsP8EdatGE/Sheet1";
        System.out.println("Making Advertiser REST call to OpenSheet = " + openSheetUrl);
        String response = restTemplate.getForObject(openSheetUrl, String.class);
        ArrayNode results = mapper.createParser(response).readValueAsTree();
        if(results != null && !results.isEmpty()) {
            for(JsonNode itemNode : results) {
                feedMap.put(itemNode.get("segment_id").textValue(), itemNode.get("feedRowID").textValue());
            }
        }
        return feedMap;
    }

    public ConcurrentHashMap<String, Segments> getSegKeyMap() {
        if(segKeyMap == null) {
            synchronized (BeeswaxLoaderService.class) {
                if(segKeyMap == null) {
                    loadSegmentTree();
                }
            }

        }
        return segKeyMap;
    }

    @Deprecated
    public void populateActiveLineItemsFromInputStrategy() throws IOException {
        String biddingStrategyUrl = "https://prism.api.beeswax.com/rest/line_item?bidding_strategy=PRISM_TEST_STRATEGY";
        String response = restTemplate.getForObject(biddingStrategyUrl, String.class);
        JsonNode node = mapper.createParser(response).readValueAsTree();
        ArrayNode results = node.withArray("payload");
        if(results != null && !results.isEmpty()) {
            for(JsonNode itemNode : results) {
                LineItem lineItem = new LineItem();
                lineItem.setId(itemNode.get("line_item_id").intValue());
                lineItem.setBudget(itemNode.get("line_item_budget").floatValue());
                lineItem.setActive(itemNode.get("active").asBoolean());
                lineItem.setTargetExpressionId(itemNode.get("targeting_expression_id").intValue());
                if(lineItem.getActive()) {
                    populateCreatives(lineItem);
                    List<String> segItemList = getSegmentIds(lineItem.getTargetExpressionId().toString());
                    System.out.println("Line Item: " + lineItem.getId().toString() + " - " + segItemList);
                    if(segItemList != null && !segItemList.isEmpty()) {
                        for(String segId : segItemList) {
                            Segments segment = segKeyMap.get(segId);
                            if(segment != null) {
                                segment.getLineItemList().add(lineItem);
                            }
                        }
                    }

                }
            }

        }
    }
}
