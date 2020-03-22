package jp.jyn.jumphome.config;

import jp.jyn.jbukkitlib.config.YamlLoader;
import jp.jyn.jumphome.JumpHome;
import org.bukkit.plugin.Plugin;

public class ConfigLoader {
    private final YamlLoader mainLoader;
    private MainConfig mainConfig;

    public ConfigLoader() {
        Plugin plugin = JumpHome.getInstance();
        this.mainLoader = new YamlLoader(plugin, "config.yml");
    }

    public void reloadConfig() {
        mainLoader.saveDefaultConfig();
        if (mainConfig != null) {
            mainLoader.reloadConfig();
        }

        mainConfig = new MainConfig(mainLoader.getConfig());
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }
}
