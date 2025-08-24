package org.txtox8729.vshulkers.commands;

import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class VTabCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, @NonNull String[] args) {
        if (args.length == 1 && (sender instanceof Player player ? player.hasPermission("vshulker.admin") || player.isOp() : true)) {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}