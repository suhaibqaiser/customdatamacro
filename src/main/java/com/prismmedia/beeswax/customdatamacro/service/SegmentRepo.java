package com.prismmedia.beeswax.customdatamacro.service;

import com.prismmedia.beeswax.customdatamacro.entity.Segments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class SegmentRepo {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Segments> getSegments() {
        return jdbcTemplate.query("SELECT * FROM SEGMENTS", new RowMapper<Segments>() {
            @Override
            public Segments mapRow(ResultSet resultSet, int i) throws SQLException {
                Segments segmentDto = new Segments();
                segmentDto.setId(resultSet.getInt("idsegments"));
                segmentDto.setName(resultSet.getString("name"));
                return segmentDto;
            }
        });
    }
}
