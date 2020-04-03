package jp.jyn.jumphome.db.driver;

import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.jumphome.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLite extends Database {
    public SQLite(HikariDataSource hikari) {
        super(hikari);

        // create table
        try (Connection c = hikari.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_user` (" +
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

            s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `home` (" +
                    "   `user`  INTEGER NOT NULL," +
                    "   `name`  TEXT    NOT NULL," +
                    "   `world` INTEGER NOT NULL," +
                    "   `x`     REAL    NOT NULL," +
                    "   `y`     REAL    NOT NULL," +
                    "   `z`     REAL    NOT NULL," +
                    "   `yaw`   REAL    NOT NULL," +
                    "   PRIMARY KEY (`user`, `name`)" +
                    ")"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean set(int user, String name, int world, double x, double y, double z, float yaw) {
        try (Connection c = hikari.getConnection();
             PreparedStatement s = c.prepareStatement(
                 "INSERT OR REPLACE INTO `home` VALUES(?,?,?,?,?,?,?)"
             )) {
            s.setInt(1, user);
            s.setString(2, name);
            s.setInt(3, world);
            s.setDouble(4, x);
            s.setDouble(5, y);
            s.setDouble(6, z);
            s.setFloat(7, yaw);
            return s.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
