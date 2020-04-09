package jp.jyn.jumphome.config;

import jp.jyn.jbukkitlib.config.parser.template.StringParser;
import jp.jyn.jbukkitlib.config.parser.template.TemplateParser;
import jp.jyn.jbukkitlib.util.PackagePrivate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.function.Function;

public class MessageConfig {
    private final static String PREFIX = "[JumpHome] ";
    public final static String HEADER = "========== JumpHome ==========";
    public final static String PLAYER_ONLY = PREFIX + ChatColor.RED + "This command can only be run by players.";

    public final TemplateParser doNotHavePermission;
    /**
     * world
     */
    public final TemplateParser worldDoesNotExist;
    public final TemplateParser unknownError;

    /**
     * name,world,x,y,z,yaw
     */
    public final TemplateParser set;
    public final TemplateParser overLimit;
    /**
     * name
     */
    public final TemplateParser unavailableName;

    @PackagePrivate
    MessageConfig(ConfigurationSection config) {
        Function<String, TemplateParser> parse = key -> StringParser.parse(PREFIX + config.getString(key));

        doNotHavePermission = parse.apply("doNotHavePermission");
        worldDoesNotExist = parse.apply("worldDoesNotExist");
        unknownError = parse.apply("unknownError");

        set = parse.apply("set");
        overLimit = parse.apply("overLimit");
        unavailableName = parse.apply("unavailableName");
    }
}
