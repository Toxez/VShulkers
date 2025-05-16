package org.txtox8729.vshulkers.listeners;

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

import java.util.*;
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
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.hasPermission("vshulker.limit") && !isShulkerOpen(player)) {
                        checkAndScheduleDrop(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    private void checkAndScheduleDrop(Player player) {
        ItemStack[] inventoryContents = player.getInventory().getContents();
        List<Integer> slotsToClear = new ArrayList<>();
        int shulkerCount = 0;

        for (int i = 0; i < inventoryContents.length; i++) {
            ItemStack item = inventoryContents[i];
            if (isShulkerBox(item)) {
                if (++shulkerCount > ConfigUtil.shulkerLimit) {
                    slotsToClear.add(i);
                }
            }
        }

        if (!slotsToClear.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;

                    int droppedCount = 0;
                    for (int slot : slotsToClear) {
                        ItemStack item = player.getInventory().getItem(slot);
                        if (item != null && isShulkerBox(item)) {
                            player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                            player.getInventory().setItem(slot, null);
                            droppedCount++;
                        }
                    }

                    if (droppedCount > 0) {
                        player.updateInventory();
                        sendMessageWithCooldown(player, ConfigUtil.limitShulkerDroppedMessage
                                .replace("%limit%", String.valueOf(ConfigUtil.shulkerLimit))
                                .replace("%dropped%", String.valueOf(droppedCount)), true);
                    }
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    private boolean isShulkerBox(ItemStack item) {
        return item != null && item.getType().name().contains("SHULKER_BOX");
    }

    private void sendMessageWithCooldown(Player player, String message, boolean forceMessage) {
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

    @EventHandler
    public void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("vshulker.limit") && isShulkerBox(event.getItem().getItemStack())) {
            if (countShulkerBoxes(player.getInventory().getContents()) >= ConfigUtil.shulkerLimit) {
                event.setCancelled(true);
                sendMessageWithCooldown(player, ConfigUtil.limitShulkerReachedMessage
                        .replace("%limit%", String.valueOf(ConfigUtil.shulkerLimit)), false);
            }
        }
    }

    private int countShulkerBoxes(ItemStack[] contents) {
        int count = 0;
        for (ItemStack item : contents) {
            if (isShulkerBox(item)) {
                count++;
            }
        }
        return count;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastMessageTimeMap.remove(event.getPlayer().getUniqueId());
    }

    private boolean isShulkerOpen(Player p) {
        return p.hasMetadata(SHULKER_OPEN_KEY.getKey());
    }
}