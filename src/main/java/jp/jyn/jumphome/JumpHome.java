package jp.jyn.jumphome;

import jp.jyn.jumphome.command.Spawn;
import jp.jyn.jumphome.config.ConfigLoader;
import jp.jyn.jumphome.config.MainConfig;
import jp.jyn.jumphome.config.MessageConfig;
import jp.jyn.jumphome.db.Database;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class JumpHome extends JavaPlugin {
    private static JumpHome instance = null;
    private Home home;

    // LIFO
    private final Deque<Runnable> destructor = new ArrayDeque<>();

    @Override
    public void onEnable() {
        instance = this;

        // Config
        ConfigLoader config = new ConfigLoader();
        config.reloadConfig();
        MainConfig mainConfig = config.getMainConfig();
        MessageConfig messageConfig = config.getMessageConfig();

        // Database
        Database database = Database.connect(mainConfig.database);
        destructor.add(database::close);

        // Home
        home = new Home(database);
        destructor.add(() -> home = null);

        // Command: spawn
        PluginCommand commandSpawn = Objects.requireNonNull(getCommand("spawn"));
        Spawn spawn = new Spawn(mainConfig, messageConfig);
        commandSpawn.setExecutor(spawn);
        destructor.add(() -> commandSpawn.setExecutor(this));
        commandSpawn.setTabCompleter(spawn);
        destructor.add(() -> commandSpawn.setTabCompleter(this));
    }

    @Override
    public void onDisable() {
        while (!destructor.isEmpty()) {
            destructor.removeFirst().run();
        }
    }

    /**
     * Get JumpHome instance
     *
     * @return JumpHome instance
     */
    public static JumpHome getInstance() {
        return instance;
    }

    /**
     * Get Home instance
     *
     * @return Home instance
     */
    public Home getHome() {
        return home;
    }
}
