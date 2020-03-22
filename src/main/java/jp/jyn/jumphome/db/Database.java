package jp.jyn.jumphome.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.jumphome.JumpHome;
import jp.jyn.jumphome.config.MainConfig;
import jp.jyn.jumphome.db.driver.MySQL;
import jp.jyn.jumphome.db.driver.SQLite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public abstract class Database {
    protected final HikariDataSource hikari;

    protected Database(HikariDataSource hikari) {
        this.hikari = hikari;

        // checking database version
        try (Connection c = hikari.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `meta` (`key` TEXT, `value` TEXT)");

            try (ResultSet r = s.executeQuery("SELECT `value` FROM `meta` WHERE `key`='version'")) {
                if (r.next()) {
                    if (!r.getString(1).equals("1")) {
                        throw new RuntimeException("Database version error (Database cannot be downgraded.)");
                    }
                } else {
                    s.executeUpdate("INSERT INTO `meta` VALUES ('version','1')");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Database connect(MainConfig.DatabaseConfig config) {
        HikariConfig hc = new HikariConfig();

        hc.setJdbcUrl(config.url);
        hc.setPoolName("jumphome-hikari");
        hc.setAutoCommit(true);
        hc.setConnectionInitSql(config.init);
        hc.setDataSourceProperties(config.properties);

        if (config.maximumPoolSize > 0) {
            hc.setMaximumPoolSize(config.maximumPoolSize);
        }
        if (config.minimumIdle > 0) {
            hc.setMinimumIdle(config.minimumIdle);
        }
        if (config.maxLifetime > 0) {
            hc.setMaxLifetime(config.maxLifetime);
        }
        if (config.connectionTimeout > 0) {
            hc.setConnectionTimeout(config.connectionTimeout);
        }
        if (config.idleTimeout > 0) {
            hc.setIdleTimeout(config.idleTimeout);
        }

        Logger logger = JumpHome.getInstance().getLogger();
        if (config.url.startsWith("jdbc:sqlite:")) {
            logger.info("Use SQLite");

            return new SQLite(new HikariDataSource(hc));
        } else if (config.url.startsWith("jdbc:mysql:")) {
            logger.info("Use MySQL");
            hc.setUsername(config.username);
            hc.setPassword(config.password);

            return new MySQL(new HikariDataSource(hc));
        } else {
            throw new IllegalArgumentException("Unknown jdbc");
        }
    }

    public void close() {
        if (hikari != null) {
            hikari.close();
        }
    }
}
