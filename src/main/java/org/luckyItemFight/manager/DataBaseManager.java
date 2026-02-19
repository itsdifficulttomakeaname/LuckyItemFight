package org.luckyItemFight.manager;

import lombok.SneakyThrows;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static org.luckyItemFight.Main.instance;

public class DataBaseManager {
    private Connection connection;

    public DataBaseManager() {
        try{
            connect();
            createTable();
        } catch (SQLException ex) {
            instance.getLogger().severe("创建/连接数据库时发生了错误!(DataBaseManager::connect | createTable)");
        }
    }

    private void connect() throws SQLException {
        String url = "jdbc:sqlite:" + instance.getDataFolder().getPath() + "/database.db"; // 当前目录下的文件
        connection = DriverManager.getConnection(url);
        instance.getLogger().info("连接到本地数据库!");
    }

    private void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS playerData (
                player TEXT PRIMARY KEY,
                coin INTEGER DEFAULT 0 NOT NULL,
                play INTEGER DEFAULT 0 NOT NULL,
                win INTEGER DEFAULT 0 NOT NULL,
                kill INTEGER DEFAULT 0 NOT NULL
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @SneakyThrows
    public void insert(String playerName) {
        String sql = "INSERT INTO playerData (player) VALUES (?)";
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerName);

            preparedStatement.executeUpdate();
        }
    }

    @SneakyThrows
    public void update(String playerName,String placeHolder,Object val) {
        String sql = "UPDATE playerData SET " + placeHolder + " = ? WHERE player = ?";
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setObject(1, val);
            preparedStatement.setString(2, playerName);

            preparedStatement.executeUpdate();
        }
    }

    @SneakyThrows
    public Object query(String playerName,String placeHolder) {
        String sql = "SELECT " + placeHolder + " FROM playerData WHERE player = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerName);

            try(ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) return resultSet.getObject(placeHolder);
                return null;
            }
        }
    }

    @SneakyThrows
    public Map<String, Object> getPlayer(String playerName) {
        String sql = "SELECT * FROM playerData WHERE player = ?";
        Map<String, Object> result = new HashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    result.put("player", rs.getString("player"));
                    result.put("coin", rs.getInt("coin"));
                    result.put("play", rs.getInt("play"));
                    result.put("win", rs.getInt("win"));
                    result.put("kill", rs.getInt("kill"));
                } else {
                    return null;
                }
            }
        }

        return result;
    }

    @SneakyThrows
    public void close() {
        if (connection != null) {
            connection.close();
        }
    }
}
