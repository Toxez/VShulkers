package org.txtox8729.vshulkers.listeners;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.txtox8729.vshulkers.VShulkers;
import org.txtox8729.vshulkers.utils.ConfigUtil;
import org.txtox8729.vshulkers.utils.ShulkerUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LimitListener implements Listener {

    private final Map<UUID, Long> lastMessageTimeMap = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;
    private static final NamespacedKey SHULKER_OPEN_KEY = new NamespacedKey(VShulkers.getPlugin(VShulkers.class), "shulker-open");

    public LimitListener(JavaPlugin plugin) {
        this.plugin = plugin;
        startShulkerCheckTask();
    }

    private void startShulkerCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ConfigUtil.shulkerLimitEnabled) return;
                Bukkit.getOnlinePlayers().stream()
                        .filter(player -> !player.hasPermission("vshulker.limit") && !isShulkerOpen(player))
                        .forEach(ShulkerUtils::checkAndDropExcessShulkers);
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    @EventHandler
    public void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        if (!ConfigUtil.shulkerLimitEnabled) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("vshulker.limit") && ShulkerUtils.isShulkerBox(event.getItem().getItemStack())) {
            if (ShulkerUtils.countShulkerBoxes(player.getInventory().getContents()) >= ConfigUtil.shulkerLimit) {
                event.setCancelled(true);
                sendMessageWithCooldown(player, ConfigUtil.limitShulkerReachedMessage
                        .replace("%limit%", String.valueOf(ConfigUtil.shulkerLimit)), false);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastMessageTimeMap.remove(event.getPlayer().getUniqueId());
    }

    private boolean isShulkerOpen(@NonNull Player player) {
        return player.hasMetadata(SHULKER_OPEN_KEY.getKey());
    }

    private void sendMessageWithCooldown(@NonNull Player player, @NonNull String message, boolean forceMessage) {
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        if (forceMessage || currentTime - lastMessageTimeMap.getOrDefault(playerId, 0L) >= 1000L) {
            lastMessageTimeMap.put(playerId, currentTime);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.sendMessage(message);
                    }
                }
            }.runTask(plugin);
        }
    }
}