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
                "CREATE TABLE IF NOT EXISTS `id_user` (" +
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

            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `home` (" +
                    "   `user`  INT UNSIGNED NOT NULL," +
                    "   `name`  VARCHAR(128) NOT NULL," +
                    "   `world` INT UNSIGNED NOT NULL," +
                    "   `x`     DOUBLE       NOT NULL," +
                    "   `y`     DOUBLE       NOT NULL," +
                    "   `z`     DOUBLE       NOT NULL," +
                    "   `yaw`   FLOAT        NOT NULL," +
                    "   PRIMARY KEY (`user`, `name`)" +
                    ")"
            );
            // PRIMARY KEY=indexの並びが`user`,`name`なので、WHERE `user`=?で絞り込みをするだけなら追加のインデックスは要らない
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean set(int user, String name, int world, double x, double y, double z, float yaw) {
        return 0 != executeUpdate(
            "INSERT INTO `home` VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE" +
                "  `world`=VALUES(`world`), `x`=VALUES(`x`), `y`=VALUES(`y`), `z`=VALUES(`z`), `yaw`=VALUES(`yaw`)",
            s -> {
                s.setInt(1, user);
                s.setString(2, name);
                s.setInt(3, world);
                s.setDouble(4, x);
                s.setDouble(5, y);
                s.setDouble(6, z);
                s.setFloat(7, yaw);
            }
        );
    }
}
