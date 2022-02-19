package com.prismmedia.beeswax.customdatamacro.service;

import com.beeswax.augment.Augmentor;
import com.beeswax.bid.Request;
import com.beeswax.openrtb.Openrtb;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.protobuf.InvalidProtocolBufferException;
import com.prismmedia.beeswax.customdatamacro.entity.Advertiser;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class LookupService {

    @Autowired
    private BeeswaxLoaderService loaderService;

    private static Boolean enableLogs = false;

    private static String ipAddress = "";

    private static Integer logCount = 0;

    private static Integer logLimit = 5;

    private Segments macroSegment = null;

    @Autowired
    private SegmentRepo segmentRepo;

    public LookupService() {
    }

    public void resetLog(Boolean startLog, Integer logSize, String ipAddr) {
        enableLogs = startLog;
        logCount = 0;
        ipAddress = ipAddr;
        logLimit = logSize;
    }

    public void logEntry(Openrtb.BidRequest bidRequest, String prefix) {
        if(bidRequest.getDevice() != null) {
            if(bidRequest.getDevice().getIp().equalsIgnoreCase(ipAddress)) {
                System.out.println(prefix);
                System.out.println(bidRequest.toString());
                System.out.println("=====");
            }
        }
    }

    public Request.BidAgentResponse parseSegmentsFromCustomBid(Openrtb.BidRequest bidRequest) throws InvalidProtocolBufferException {
        Request.BidAgentResponse.Builder responseBuilder = Request.BidAgentResponse.newBuilder();
        Request.BidAgentResponse.Bid.Builder bidBuilder = Request.BidAgentResponse.Bid.newBuilder();

        Request.BidAgentResponse.Creative.Builder creativeBuilder = Request.BidAgentResponse.Creative.newBuilder();
        Request.BidAgentResponse.Creative.Macro.Builder macroBuilder = Request.BidAgentResponse.Creative.Macro.newBuilder();
        Openrtb.BidRequest.User bidRequestUser = bidRequest.getUser();
        macroSegment = new Segments(0, "", "", "", new Advertiser(0, ""));

        if(enableLogs) {
            logEntry(bidRequest, "Cusotm Bidding Agent logs");
        }
        Boolean foundValue = false;
        if(bidRequestUser.getDataCount() != 0) {
            for(Openrtb.BidRequest.Data dataItem : bidRequestUser.getDataList()) {
                List<Openrtb.BidRequest.Data.Segment> protoSegArray = dataItem.getSegmentList();
                if(protoSegArray != null) {
                    for (Openrtb.BidRequest.Data.Segment segItem : protoSegArray) {
                        Segments segEntity = lookupSegmentFromDB(segItem);
                        if (segEntity != null && segEntity.getId() != null) {
                            if (macroSegment.getId() < segEntity.getId() && segEntity.getName().contains("Product view")) {
                                macroSegment = segEntity;
                                if(enableLogs && bidRequest.getDevice().getIp().equalsIgnoreCase(ipAddress)) {
                                    System.out.println("*** Found entry for agent ".concat(segEntity.getKey()).concat(" with auction id").concat(bidRequest.getExt().getAuctionidStr()));
                                }
                                foundValue = true;
                                break;
                            }
                        }
                    }
                }
                if(foundValue) {
                    break;
                }
            }
            if(foundValue) {
                try {
                    if(macroSegment.getValue() != null && !macroSegment.getValue().isEmpty()) {
                        macroBuilder.setName(macroSegment.getAdvertiser().getName().replace(" ", ""));
                        macroBuilder.setValue(macroSegment.getValue());
                        creativeBuilder.addDynamicMacros(macroBuilder.build());
                        if(enableLogs && bidRequest.getDevice().getIp().equalsIgnoreCase(ipAddress)) {
                            System.out.println("*** Dynamic Macro ".concat(macroBuilder.build().toString()));
                        }
                    }
                    if(macroSegment.getFeedRowId() != null && !macroSegment.getFeedRowId().isEmpty()) {
                        macroBuilder = Request.BidAgentResponse.Creative.Macro.newBuilder();
                        macroBuilder.setName(macroSegment.getAdvertiser().getName().replace(" ", "").concat("FeedRowID"));
                        macroBuilder.setValue(macroSegment.getFeedRowId());
                        creativeBuilder.addDynamicMacros(macroBuilder.build());
                        if(enableLogs && bidRequest.getDevice().getIp().equalsIgnoreCase(ipAddress)) {
                            System.out.println("*** Dynamic Macro FeedRow ".concat(macroBuilder.build().toString()));
                        }
                    }
                    creativeBuilder.setId(310);
                    bidBuilder.setCreative(creativeBuilder.build());
                    bidBuilder.setBidPriceMicros(5000);
                    bidBuilder.setLineItemId(128);
                    responseBuilder.addBids(bidBuilder.build());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        return responseBuilder.build();

    }

    public Augmentor.AugmentorResponse parseSegmentsFromProtoText(Openrtb.BidRequest bidRequest) throws InvalidProtocolBufferException {
        Augmentor.AugmentorResponse.Builder responseBuilder = Augmentor.AugmentorResponse.newBuilder();
        List<Augmentor.AugmentorResponse.Segment> segList = new ArrayList<Augmentor.AugmentorResponse.Segment>();
        Augmentor.AugmentorResponse.Macro.Builder macroBuilder = Augmentor.AugmentorResponse.Macro.newBuilder();
        Openrtb.BidRequest.User bidRequestUser = bidRequest.getUser();
        macroSegment = new Segments(0, "", "", "", new Advertiser(0, ""));

        if(enableLogs) {
            logEntry(bidRequest, "augmenter");
        }

        if(bidRequestUser.getDataCount() != 0) {
            List<Openrtb.BidRequest.Data.Segment> protoSegArray = bidRequestUser.getData(0).getSegmentList();
            if(protoSegArray != null) {
                for(Openrtb.BidRequest.Data.Segment segItem : protoSegArray) {
                    Augmentor.AugmentorResponse.Segment.Builder segBuilder = Augmentor.AugmentorResponse.Segment.newBuilder();
                    Segments segEntity = lookupSegmentFromDB(segItem);
                    if(segEntity != null) {
                        if(macroSegment.getId() < segEntity.getId()) {
                            macroSegment = segEntity;
                        }
                        segBuilder.setId(segEntity.getKey());
                        segBuilder.setValue(segEntity.getValue());
                        System.out.println("*** Found entry for augmenter".concat(segItem.getName()).concat(" with auction id").concat(bidRequest.getId()));
                        segList.add(segBuilder.build());
                    }
                }
                if(segList.isEmpty()) {
                    System.out.println(" ### No entry found for bid request ".concat(bidRequest.getId()));
                    segList.add(getEmptySegment());
                } else {
                    if(macroSegment.getValue() != null) {
                        macroBuilder.setName(macroSegment.getAdvertiser().getName());
                        macroBuilder.setValue(macroSegment.getValue());
                        responseBuilder.addDynamicMacros(macroBuilder.build());
                    }
                    if(macroSegment.getFeedRowId() != null) {
                        macroBuilder = Augmentor.AugmentorResponse.Macro.newBuilder();
                        macroBuilder.setName(macroSegment.getAdvertiser().getName().concat("FeedRowID"));
                        macroBuilder.setValue(macroSegment.getFeedRowId());
                    }

                }
                responseBuilder.addAllSegments(segList);
                responseBuilder.addDynamicMacros(macroBuilder.build());
            }
        }
        return responseBuilder.build();

    }

    public Segments lookupSegmentFromDB(final Openrtb.BidRequest.Data.Segment segment) {
        Segments returnSegment = null;
        if(segment != null && loaderService.getSegKeyMap().containsKey(segment.getId())) {
            returnSegment = loaderService.getSegKeyMap().get(segment.getId());
        }
        return returnSegment;
    }

    public Augmentor.AugmentorResponse.Segment getEmptySegment() {
        Augmentor.AugmentorResponse.Segment.Builder segBuilder = Augmentor.AugmentorResponse.Segment.newBuilder();
        segBuilder.setId("0");
        segBuilder.setValue("0");
        return segBuilder.build();
    }

    @Deprecated
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

    private List<Segments> parseSegmentNodes(final ObjectMapper mapper, @NotNull final JsonNode segments) throws IOException {
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
