package jp.jyn.jumphome.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.jbukkitlib.sql.SQLTemplate;
import jp.jyn.jbukkitlib.uuid.UUIDBytes;
import jp.jyn.jumphome.JumpHome;
import jp.jyn.jumphome.config.MainConfig;
import jp.jyn.jumphome.db.driver.MySQL;
import jp.jyn.jumphome.db.driver.SQLite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public abstract class Database extends SQLTemplate {
    protected final HikariDataSource hikari;

    protected Database(HikariDataSource hikari) {
        super(hikari::getConnection);
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

    // region id
    public int user2id(UUID uuid) {
        byte[] bytesUUID = UUIDBytes.toBytes(uuid);
        return getId(
            "SELECT `id` FROM `id_user` WHERE `uuid`=?",
            "INSERT INTO `id_user` (`uuid`) VALUES (?)",
            p -> p.setBytes(1, bytesUUID)
        );
    }

    public int world2Id(String world) {
        return getId(
            "SELECT `id` FROM `id_world` WHERE `world`=?",
            "INSERT INTO `id_world` (`world`) VALUES (?)",
            p -> p.setString(1, world)
        );
    }

    public String id2world(int id) {
        return select(
            "SELECT `world` FROM `id_world` WHERE `id`=?",
            p -> p.setInt(1, id),
            r -> r.next() ? r.getString(1) : null
        );
    }

    protected int getId(String select, String insert, PreparedParameter parameter) {
        try (Connection c = hikari.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement s = c.prepareStatement(select)) {
                parameter.set(s);
                try (ResultSet r = s.executeQuery()) {
                    if (r.next()) {
                        return r.getInt(1);
                    }
                }

                // ID発行
                try (PreparedStatement s2 = c.prepareStatement(insert,
                    PreparedStatement.RETURN_GENERATED_KEYS // TODO: これたぶんSQLiteで使えん
                )) {
                    parameter.set(s2);
                    s2.executeUpdate();
                    try (ResultSet r = s2.getGeneratedKeys()) {
                        if (r.next()) {
                            return r.getInt(1);
                        }
                    }
                }

                // IDが出てない
                c.rollback();
                throw new RuntimeException("Unable to issue ID.");
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true); // eq commit()
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    // endregion

    public abstract boolean set(int user, String name, int world, double x, double y, double z, float yaw);

    public RawLocation get(int user, String name) {
        return select(
            "SELECT `world`,`x`,`y`,`z`,`yaw` FROM `home` WHERE `user`=? AND `name`=?",
            p -> {
                p.setInt(1, user);
                p.setString(2, name);
            },
            r -> r.next()
                ? new RawLocation(
                name,
                r.getInt("world"),
                r.getDouble("x"),
                r.getDouble("y"),
                r.getDouble("z"),
                r.getFloat("yaw"))
                : null
        );
    }

    public boolean delete(int user, String name) {
        return 0 != delete(
            "DELETE FROM `home` WHERE `user`=? AND `name`=?",
            s -> {
                s.setInt(1, user);
                s.setString(2, name);
            }
        );
    }

    public List<RawLocation> list(int user) {
        return select(
            "SELECT `name`,`world`,`x`,`y`,`z`,`yaw` FROM `home` WHERE `user`=?",
            p -> p.setInt(1, user),
            r -> {
                // 大抵の場合は初期容量(10)で収まると予想されるので、参照の局所性で有利なArrayListを使う。(LinkedListに性能的なメリットが少ない)
                List<RawLocation> result = new ArrayList<>();
                while (r.next()) {
                    result.add(new RawLocation(
                        r.getString("name"),
                        r.getInt("world"),
                        r.getDouble("x"),
                        r.getDouble("y"),
                        r.getDouble("z"),
                        r.getFloat("yaw")
                    ));
                }
                return result;
            }
        );
    }

    public static class RawLocation {
        public final String name;
        public final int world;
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;

        public RawLocation(String name, int world, double x, double y, double z, float yaw) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }
}
