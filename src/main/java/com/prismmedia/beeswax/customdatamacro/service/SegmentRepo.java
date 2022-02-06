package com.prismmedia.beeswax.customdatamacro.service;

import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SegmentRepo {

    @Autowired
    private JdbcTemplate jdbcTemplate;



    public List<Segments> getSegments() {
        return  jdbcTemplate.query("SELECT * FROM SEGMENTS", new RowMapper<Segments>() {
            @Override
            public Segments mapRow(ResultSet resultSet, int i) throws SQLException {
                Segments segmentDto = new Segments();
                segmentDto.setId(resultSet.getString("id"));
                segmentDto.setKey(resultSet.getString("key"));
                segmentDto.setName(resultSet.getString("name"));
                segmentDto.setValue(resultSet.getString("value"));
                segmentDto.setAdvertiserId(resultSet.getString("advertiserId"));
                return segmentDto;
            }
        });
    }

    public ConcurrentHashMap<String, Segments> fetchSegmentsNameMap() {
        List<Segments> segList = getSegments();
        ConcurrentHashMap<String, Segments> segMap = new ConcurrentHashMap<String, Segments>();
        for(Segments segItem : segList) {
            segMap.put(segItem.getName(), segItem);
        }
        return segMap;
    }

    public ConcurrentHashMap<String, Segments> fetchSegmentsValueMap() {
        List<Segments> segList = getSegments();
        ConcurrentHashMap<String, Segments> segMap = new ConcurrentHashMap<String, Segments>();
        for(Segments segItem : segList) {
            segMap.put(segItem.getValue(), segItem);
        }
        return segMap;
    }
}
