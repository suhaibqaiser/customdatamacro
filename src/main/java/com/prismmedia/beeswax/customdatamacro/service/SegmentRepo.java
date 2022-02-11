package com.prismmedia.beeswax.customdatamacro.service;

import com.prismmedia.beeswax.customdatamacro.entity.Advertiser;
import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SegmentRepo {

    @Autowired
    private JdbcTemplate jdbcTemplate;



    public List<Segments> getSegments() {
        System.out.println("Loading Segments data from mysql db ...");
        return  jdbcTemplate.query("SELECT * FROM SEGMENTS", new RowMapper<Segments>() {
            @Override
            public Segments mapRow(ResultSet resultSet, int i) throws SQLException {
                Segments segmentDto = new Segments();
                segmentDto.setId(resultSet.getInt("id"));
                segmentDto.setKey(resultSet.getString("key"));
                segmentDto.setName(resultSet.getString("name"));
                segmentDto.setValue(resultSet.getString("value"));
                Advertiser advertiser = new Advertiser(resultSet.getInt("advertiserId"), "");
                segmentDto.setAdvertiser(advertiser);
                return segmentDto;
            }
        });
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteAllFromSegments() {
        try {
            jdbcTemplate.execute("TRUNCATE TABLE SEGMENTS");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int save(final Segments segments) {
        try {
            return jdbcTemplate.update("INSERT INTO SEGMENTS values (?,?,?,?,?,?)", segments.getId(), segments.getKey(), segments.getName(), segments.getValue(), segments.getAdvertiser().getId(), segments.getFeedRowId());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

    }

    public ConcurrentHashMap<String, Segments> fetchSegmentsNameMap() {
        List<Segments> segList = getSegments();
        ConcurrentHashMap<String, Segments> segMap = new ConcurrentHashMap<String, Segments>();
        for(Segments segItem : segList) {
            segMap.put(segItem.getName(), segItem);
        }
        return segMap;
    }

    public ConcurrentHashMap<String, Segments> fetchSegmentsKeyMap() {
        List<Segments> segList = getSegments();
        ConcurrentHashMap<String, Segments> segMap = new ConcurrentHashMap<String, Segments>();
        for(Segments segItem : segList) {
            segMap.put(segItem.getKey(), segItem);
        }
        return segMap;
    }
}
