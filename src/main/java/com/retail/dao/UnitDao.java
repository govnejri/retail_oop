package com.retail.dao;

import com.retail.model.Unit;

import java.sql.*;
import java.util.List;
import java.util.Optional;


public class UnitDao extends AbstractDao<Unit, Integer> {

    @Override
    protected String getTableName() {
        return "units";
    }

    @Override
    protected Unit mapRow(ResultSet rs) throws SQLException {
        Unit unit = new Unit();
        unit.setId(rs.getInt("id"));
        unit.setName(rs.getString("name"));
        unit.setShortName(rs.getString("short_name"));
        unit.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return unit;
    }

    @Override
    public Unit save(Unit unit) throws SQLException {
        String sql = """
            INSERT INTO units (name, short_name)
            VALUES (?, ?)
            RETURNING id
            """;
        
        Integer id = executeInsertAndGetKey(sql,
                unit.getName(),
                unit.getShortName());
        
        unit.setId(id);
        return unit;
    }

    @Override
    public void update(Unit unit) throws SQLException {
        String sql = """
            UPDATE units SET
                name = ?,
                short_name = ?
            WHERE id = ?
            """;
        
        executeUpdate(sql,
                unit.getName(),
                unit.getShortName(),
                unit.getId());
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM units WHERE id = ?";
        executeUpdate(sql, id);
    }

    
    public Optional<Unit> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM units WHERE name = ?";
        return executeQuerySingle(sql, name);
    }

    @Override
    public List<Unit> findAll() throws SQLException {
        String sql = "SELECT * FROM units ORDER BY name";
        return executeQuery(sql);
    }
}
