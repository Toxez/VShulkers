package org.txtox8729.vshulkers.commands;

import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.txtox8729.vshulkers.utils.ConfigUtil;

public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String[] args) {
        if (sender instanceof Player player && !player.hasPermission("vshulker.admin") && !player.isOp()) {
            player.sendMessage(ConfigUtil.noPermissionMessage);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            ConfigUtil.plugin.reloadPlugin();
            sender.sendMessage(ConfigUtil.reloadSuccessMessage);
            return true;
        }

        sender.sendMessage((sender instanceof Player player && (player.hasPermission("vshulker.admin") || player.isOp()))
                ? ConfigUtil.usageMessage : ConfigUtil.noPermissionMessage);
        return true;
    }
}