package jp.jyn.jumphome.db.driver;

import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.jumphome.db.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQL extends Database {
    public MySQL(HikariDataSource hikari) {
        super(hikari);

        try (Connection c = hikari.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_uuid` (" +
                    "   `id`   INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT," +
                    "   `uuid` BINARY(16)   NOT NULL UNIQUE  KEY" +
                    ")"
            );

            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_world` (" +
                    "   `id`    INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT," +
                    "   `world` VARCHAR(128) NOT NULL UNIQUE  KEY" + // VARCHAR(255) in utf8mb4 = over 767 bytes
                    ")"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
