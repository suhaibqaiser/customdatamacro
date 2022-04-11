package com.prismmedia.beeswax.customdatamacro.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prismmedia.beeswax.customdatamacro.entity.*;
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

    private static ConcurrentHashMap<String, Deal> dealsMap = null;

    private static ConcurrentHashMap<String, OpenSheet> feedRowMap = null;

    private static ConcurrentHashMap<Integer, Advertiser> advertiserMap = null;

    public static ConcurrentHashMap<String, Segments> thirdPartySegMap = null;

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


        Integer rowCount = 1;
        try {
            System.out.println("Loading from Beeswax System start time = " + sdf.format(new Date()));
            authenticate();
            advertiserMap = loadAdvertisersFromBeeswax();
            feedRowMap = loadFeedRowIdMap();
            dealsMap = loadDeals();
            String beeswaxUrl = "https://prism.api.beeswax.com/rest/v2/ref/segment-tree";

            boolean next = true;

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
                            OpenSheet openSheet = feedRowMap.get(segments.getKey());
                            if(openSheet != null) {
                                segments.setName(itemNode.get("name").textValue());
                                segments.setAdvertiser(advertiserMap.get(itemNode.get("advertiser_id").asInt()));
                                segments.setId(openSheet.getSegmentIdIntValue());
                                segments.setValue(openSheet.getAlternativeId());
                                segments.setFeedRowId(openSheet.getFeedRowId());
                                keyMap.put(segments.getKey(), segments);
                                rowCount++;
                            }
                        }
                    }
                }
            }
            if(!keyMap.isEmpty()) {
                segKeyMap = keyMap;
                System.out.println("Populating Line Items ... this may take time");
                loadThirdPartySegments();
                System.out.println("Replaced values of Third Party Segment Maps in memory from Beeswax system");
                populateActiveLineItems();
                System.out.println("Replaced values of Segment Maps in memory from Beeswax system");
            }
            System.out.println("Parsing completed with total count " +  rowCount + " time = " + sdf.format(new Date()));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error occurred in parsing of Segments from Beeswax ...");
        }


    }

    public void loadThirdPartySegments() {
        System.out.println("Loading from Third Party Segments start time = " + sdf.format(new Date()));
        ConcurrentHashMap<String, Segments> thirdPartySegmentsMap = new ConcurrentHashMap<String, Segments>();
        try {
            String beeswaxUrl = "https://prism.api.beeswax.com/rest/v2/third-party/segment-search?search=grapeshot";
            Integer rowCount = 0;
            boolean next = true;

            while(beeswaxUrl != null && !beeswaxUrl.isEmpty()) {
                System.out.println("Making third party segments calls to Beeswax = " + beeswaxUrl);
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
                            OpenSheet openSheet = feedRowMap.get(segments.getKey());
                            if(openSheet != null) {
                                segments.setName(itemNode.get("name").textValue());
                                segments.setId(openSheet.getSegmentIdIntValue());
                                segments.setValue(openSheet.getAlternativeId());
                                segments.setAdvertiser(advertiserMap.get(openSheet.getAdvertiserIdIntValue()));
                                segments.setFeedRowId(openSheet.getFeedRowId());
                                thirdPartySegmentsMap.put(segments.getKey(), segments);
                                rowCount++;
                            }
                        }
                    }
                }
            }
            if(!thirdPartySegmentsMap.isEmpty()) {
                thirdPartySegMap = thirdPartySegmentsMap;
                System.out.println("Third PArty Segments load completed with total count " +
                        rowCount + " time = " + sdf.format(new Date()));
            }

        } catch(Exception e) {
            e.printStackTrace();
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
                    lineItem.setCpmBid(itemNode.get("bidding").get("values").get("bid_cpm").textValue());
                    List<String> segItemList = getSegmentIds(lineItem.getTargetExpressionId().toString());
                    System.out.println("PRISM TEST: Line Item: " + lineItem.getId().toString() + " - " + segItemList);
                    if(segItemList != null && !segItemList.isEmpty()) {
                        populateCreatives(lineItem);
                        for(String segId : segItemList) {
                            Segments segment = segKeyMap.get(segId);
                            if(segment != null) {
                                segment.getLineItemList().add(lineItem);
                            }
                            segment = thirdPartySegMap.get(segId);
                            if(segment != null) {
                                segment.getLineItemList().add(lineItem);
                            }
                        }
                    }
                }
            }

        }
    }

    public ConcurrentHashMap<String, Deal> loadDeals() throws IOException {
        System.out.println("Loading from Beeswax Deals start time = " + sdf.format(new Date()));
        ConcurrentHashMap<String, Deal> dealItemsMap = new ConcurrentHashMap<String, Deal>();
        try {

            String beeswaxUrl = "https://prism.api.beeswax.com/rest/v2/deals";
            Integer rowCount = 0;
            boolean next = true;

            while(beeswaxUrl != null && !beeswaxUrl.isEmpty()) {
                System.out.println("Making deals call to Beeswax = " + beeswaxUrl);
                String response = restTemplate.getForObject(beeswaxUrl, String.class);
                JsonNode node = mapper.createParser(response).readValueAsTree();
                beeswaxUrl = node.get("next").textValue();

                ArrayNode results = node.withArray("results");
                if(results != null && !results.isEmpty()) {
                    for(JsonNode itemNode : results) {
                        Deal deal = new Deal();
                        rowCount++;
                        deal.setId(itemNode.get("id").intValue());
                        deal.setDealIdentifier(itemNode.get("deal_id").textValue());
                        deal.setCpmOverride(itemNode.get("cpm_override").textValue());
                        deal.setInventorySourceKey(itemNode.get("inventory_source_key").textValue());
                        dealItemsMap.put(deal.getDealIdentifier(), deal);
                    }
                }
            }
            System.out.println("Deal load completed with total count " +  rowCount + " time = " + sdf.format(new Date()));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error occurred in parsing of Segments from Beeswax ...");
        }
        return dealItemsMap;
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
                        if(itemNode.get("modules") != null && itemNode.get("modules").get("user") != null) {
                            ArrayNode segments = itemNode.get("modules").get("user").get("all").get("segment").withArray("any");
                            if(segments != null) {
                                for(JsonNode segItem : segments) {
                                    segIdList.add(segItem.get("value").textValue());
                                }
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

    public ConcurrentHashMap<String, OpenSheet> loadFeedRowIdMap() throws IOException {
        ConcurrentHashMap<String, OpenSheet> feedMap = new ConcurrentHashMap<String, OpenSheet>();
        String openSheetUrl = "https://opensheet.elk.sh/1t-bWAQdSeiUJA37gjcyFuXEgyyY1GSw8tRsP8EdatGE/Sheet1";
        System.out.println("Making Advertiser REST call to OpenSheet = " + openSheetUrl);
        String response = restTemplate.getForObject(openSheetUrl, String.class);
        ArrayNode results = mapper.createParser(response).readValueAsTree();
        if(results != null && !results.isEmpty()) {
            for(JsonNode itemNode : results) {
                OpenSheet openSheet = new OpenSheet();
                openSheet.setId(itemNode.get("id").textValue());
                openSheet.setAdvertiserId(itemNode.get("advertiser_id").textValue());
                openSheet.setFeedRowId(itemNode.get("feedRowID").textValue());
                openSheet.setSegmentId(itemNode.get("segment_id").textValue());
                openSheet.setAlternativeId(itemNode.get("alternative_id").textValue());
                feedMap.put(openSheet.getId(), openSheet);
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

    public ConcurrentHashMap<String, OpenSheet> getFeedRowMap() {
        if(feedRowMap == null) {
            synchronized (BeeswaxLoaderService.class) {
                if(feedRowMap == null) {
                    loadSegmentTree();
                }
            }
        }
        return feedRowMap;
    }

    public ConcurrentHashMap<String, Deal> getDealsMap() {
        if(dealsMap == null) {
            synchronized (BeeswaxLoaderService.class) {
                if(dealsMap == null) {
                    loadSegmentTree();
                }
            }
        }
        return dealsMap;
    }

    public ConcurrentHashMap<String, Segments> getThirdPartySegMap() {
        if(thirdPartySegMap == null) {
            synchronized (BeeswaxLoaderService.class) {
                if(thirdPartySegMap == null) {
                    loadSegmentTree();
                }
            }
        }
        return thirdPartySegMap;
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
