package jp.jyn.jumphome.command;

import jp.jyn.jbukkitlib.config.parser.template.variable.StringVariable;
import jp.jyn.jbukkitlib.config.parser.template.variable.TemplateVariable;
import jp.jyn.jumphome.Home;
import jp.jyn.jumphome.config.MainConfig;
import jp.jyn.jumphome.config.MessageConfig;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SetHome implements CommandExecutor, TabCompleter {
    private final MainConfig config;
    private final MessageConfig message;
    private final Home home;

    private final Set<String> blacklist = new HashSet<>();
    private final Function<UUID, Set<String>> getHomeList;

    public SetHome(MainConfig config, MessageConfig message, Home home,
                   Collection<String> blacklist, Function<UUID, Set<String>> getHomeList) {
        this.config = config;
        this.message = message;
        this.home = home;

        this.blacklist.addAll(blacklist);
        this.getHomeList = getHomeList;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageConfig.PLAYER_ONLY);
            return true;
        }

        if (!sender.hasPermission("jumphome.sethome")) {
            sender.sendMessage(message.doNotHavePermission.toString());
            return true;
        }

        String name = Home.DEFAULT_NAME;
        if (args.length != 0) {
            name = args[0];
            if (blacklist.contains(name)) {
                sender.sendMessage(message.unavailableName.toString("name", args[0]));
                return true;
            }
        }

        Player player = (Player) sender;
        if (home.exists(player, name)) {
            // すでに存在するなら上書きして終了
            set(player, name);
            return true;
        }

        // カウント
        int limit = config.limitDefaultFree;
        for (MainConfig.Limit l : config.limitGroup) {
            if (l.free.isPresent() && player.hasPermission(l.permission)) {
                limit = l.free.getAsInt();
                break;
            }
        }
        // TODO: 有料地点を加算

        if (limit > -1 && home.list(player).size() >= limit) { // TODO: count機能？
            // 制限超過
            player.sendMessage(message.overLimit.toString());
            return true;
        }

        set(player, name);
        return true;
    }

    private void set(Player player, String name) {
        Location location = player.getLocation();
        TemplateVariable variable = StringVariable.init()
            .put("name", name)
            .put("world", Objects.requireNonNull(location.getWorld()).getName())
            .put("x", location.getBlockX())
            .put("y", location.getBlockY())
            .put("z", location.getBlockZ())
            .put("yaw", location.getYaw());

        player.sendMessage(
            (home.set(player, name, location) // TODO: 非同期書き込み
                ? message.set
                : message.unknownError
            ).toString(variable)
        );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;
            return getHomeList.apply(player.getUniqueId()).stream()
                .filter(str -> str.startsWith(args[0]))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
