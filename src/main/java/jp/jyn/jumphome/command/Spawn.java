package jp.jyn.jumphome.command;

import jp.jyn.jumphome.config.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Spawn implements CommandExecutor, TabExecutor {
    private final MainConfig config;

    public Spawn(MainConfig config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player."); // TODO: どこかで定数化する
            return true;
        }

        // 引数がない->default->jumphome.spawn
        // 引数がある->jumphome.spawn.otherも必要(->もしくはjumphome.spawn.other.[世界名]で個別許可？)
        if (!sender.hasPermission("jumphome.spawn")) {
            // TODO: エラーメッセージ
            return true;
        }

        Player player = (Player) sender;
        World moveTo;
        if (args.length == 0) {
            moveTo = config.spawnCurrent
                ? player.getWorld()
                : Objects.requireNonNull(Bukkit.getWorld(config.spawnMoveTo), "config.yml(spawn.moveTo)");
        } else {
            if (!player.hasPermission("jumphome.spawn.other")) {
                // TODO: エラーメッセージ
                return true;
            }

            moveTo = Bukkit.getWorld(args[0]);
            if (moveTo == null) {
                sender.sendMessage("World not found"); // TODO: 設定可能なメッセージ
                return true;
            }
        }

        // TODO: 安全テレポート
        player.teleport(moveTo.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // other持ってない相手に他の世界を提示しても仕方ない
        if (args.length == 1 && sender.hasPermission("jumphome.spawn.other")) {
            return Bukkit.getWorlds().stream().map(World::getName)
                .filter(str -> str.startsWith(args[0]))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
