package jp.jyn.jumphome.db.driver;

import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.jumphome.db.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLite extends Database {
    public SQLite(HikariDataSource hikari) {
        super(hikari);

        // create table
        try (Connection c = hikari.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_uuid` (" +
                    "   `id`   INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "   `uuid` BLOB    NOT NULL UNIQUE" +
                    ")"
            );

            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_world` (" +
                    "   `id`    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "   `world` TEXT    NOT NULL UNIQUE" +
                    ")"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
