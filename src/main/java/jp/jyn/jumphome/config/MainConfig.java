package jp.jyn.jumphome.config;

import jp.jyn.jbukkitlib.config.parser.ExpressionParser;
import jp.jyn.jbukkitlib.util.PackagePrivate;
import jp.jyn.jumphome.JumpHome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Properties;

public class MainConfig {
    public final boolean versionCheck;

    public final boolean spawnCurrent;
    public final String spawnMoveTo;

    public final boolean limitEconomyEnable;
    public final ExpressionParser limitEconomyCost;
    public final int limitDefaultFree;
    public final int limitDefaultPaid;
    public final List<Limit> limitGroup;

    public final DatabaseConfig database;

    @PackagePrivate
    MainConfig(FileConfiguration config) {
        versionCheck = config.getBoolean("versionCheck");

        spawnCurrent = config.getBoolean("spawn.current");
        spawnMoveTo = Objects.requireNonNull(config.getString("spawn.moveTo"), "config.yml(spawn.moveTo)");

        limitEconomyEnable = config.getBoolean("limit.economy.enable");
        limitEconomyCost = ExpressionParser.parse(Objects.requireNonNull(
            config.getString("limit.economy.cost"),
            "config.yml(limit.economy.cost)")
        );
        limitDefaultFree = config.getInt("limit.default.free", 1);
        limitDefaultPaid = config.getInt("limit.default.paid", -1);
        {
            List<Limit> tmp = new ArrayList<>();
            if (config.contains("limit.group", true)) {
                ConfigurationSection s = Objects.requireNonNull(config.getConfigurationSection("limit.group"));
                for (String key : s.getKeys(false)) {
                    tmp.add(new Limit(key, s.getConfigurationSection(key)));
                }
            }
            limitGroup = Collections.unmodifiableList(tmp);
        }

        database = new DatabaseConfig(Objects.requireNonNull(
            config.getConfigurationSection("database"), "config.yml(database)")
        );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Limit {
        public final String permission;
        public final OptionalInt free;
        public final OptionalInt paid;

        private Limit(String permission, ConfigurationSection s) {
            this.permission = Objects.requireNonNull(permission, "config.yml(limit.group)");
            Objects.requireNonNull(s, "config.yml(limit.group)");

            this.free = s.contains("free", true) ? OptionalInt.of(s.getInt("free")) : OptionalInt.empty();
            this.paid = s.contains("paid", true) ? OptionalInt.of(s.getInt("paid")) : OptionalInt.empty();
        }
    }

    public static class DatabaseConfig {
        public final String url;
        public final String username;
        public final String password;
        public final String init;
        public final Properties properties = new Properties();

        public final int maximumPoolSize;
        public final int minimumIdle;
        public final long maxLifetime;
        public final long connectionTimeout;
        public final long idleTimeout;

        @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
        private DatabaseConfig(ConfigurationSection config) {
            String type = config.getString("type", "");
            ConfigurationSection db = config.getConfigurationSection(type);

            if (type.equalsIgnoreCase("sqlite")) {
                File file = new File(JumpHome.getInstance().getDataFolder(), db.getString("file"));
                file.getParentFile().mkdirs();
                url = "jdbc:sqlite:" + file.getPath();
            } else if (type.equalsIgnoreCase("mysql")) {
                url = String.format(
                    "jdbc:mysql://%s/%s",
                    db.getString("host"),
                    db.getString("name")
                );
            } else {
                throw new IllegalArgumentException("Invalid value: database.type(config.yml)");
            }
            username = db.getString("username");
            password = db.getString("password");
            init = db.getString("init", "/* JumpHome */SELECT 1");

            if (db.contains("properties")) {
                ConfigurationSection section = db.getConfigurationSection("properties");
                for (String key : section.getKeys(false)) {
                    properties.put(key, section.getString(key));
                }
            }

            maximumPoolSize = config.getInt("connectionPool.maximumPoolSize");
            minimumIdle = config.getInt("connectionPool.minimumIdle");
            maxLifetime = config.getLong("connectionPool.maxLifetime");
            connectionTimeout = config.getLong("connectionPool.connectionTimeout");
            idleTimeout = config.getLong("connectionPool.idleTimeout");
        }
    }
}
