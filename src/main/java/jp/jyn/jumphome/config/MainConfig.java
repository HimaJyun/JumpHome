package jp.jyn.jumphome.config;

import jp.jyn.jbukkitlib.util.PackagePrivate;
import jp.jyn.jumphome.JumpHome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Properties;

public class MainConfig {
    public final boolean versionCheck;

    public final DatabaseConfig database;

    @PackagePrivate
    MainConfig(FileConfiguration config) {
        versionCheck = config.getBoolean("versionCheck");

        database = new DatabaseConfig(config.getConfigurationSection("database"));
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
