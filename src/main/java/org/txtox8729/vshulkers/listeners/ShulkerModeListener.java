package org.txtox8729.vshulkers.listeners;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.txtox8729.vshulkers.utils.ConfigUtil;
import org.txtox8729.vshulkers.utils.ShulkerUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShulkerModeListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> shulkerCache = new ConcurrentHashMap<>();
    private BukkitRunnable checkTask;

    public ShulkerModeListener(JavaPlugin plugin) {
        this.plugin = plugin;
        startShulkerModeCheckTask();
    }

    public void cleanup() {
        Optional.ofNullable(checkTask).ifPresent(BukkitRunnable::cancel);
        playerBossBars.values().forEach(BossBar::removeAll);
        playerBossBars.clear();
        shulkerCache.clear();
        Bukkit.getOnlinePlayers().forEach(player -> {
            if ("ACTIONBAR".equals(ConfigUtil.shulkerModeNotificationType)) {
                player.sendActionBar("");
            }
        });
    }

    private void startShulkerModeCheckTask() {
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!ConfigUtil.shulkerModeEnabled) {
                    cleanup();
                    return;
                }

                Bukkit.getOnlinePlayers().stream()
                        .filter(player -> !player.hasPermission("vshulker.mode.bypass"))
                        .forEach(player -> {
                            boolean hasShulker = shulkerCache.computeIfAbsent(player.getUniqueId(), k -> ShulkerUtils.hasShulkerBox(player));
                            if (hasShulker) {
                                showNotification(player);
                            } else {
                                removeNotification(player);
                            }
                        });
            }
        };
        checkTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void showNotification(@NonNull Player player) {
        switch (ConfigUtil.shulkerModeNotificationType) {
            case "BOSSBAR" -> showBossBar(player);
            case "ACTIONBAR" -> player.sendActionBar(ConfigUtil.shulkerModeBossBarMessage);
        }
    }

    private void removeNotification(@NonNull Player player) {
        switch (ConfigUtil.shulkerModeNotificationType) {
            case "BOSSBAR" -> removeBossBar(player);
            case "ACTIONBAR" -> player.sendActionBar("");
        }
    }

    private void showBossBar(@NonNull Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bossBar = playerBossBars.computeIfAbsent(uuid, k ->
                Bukkit.createBossBar(ConfigUtil.shulkerModeBossBarMessage, ConfigUtil.shulkerModeBossBarColor, ConfigUtil.shulkerModeBossBarStyle)
        );
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
    }

    private void removeBossBar(@NonNull Player player) {
        Optional.ofNullable(playerBossBars.remove(player.getUniqueId())).ifPresent(bossBar -> bossBar.removePlayer(player));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        updateCacheOnInventoryEvent(event.getWhoClicked());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        updateCacheOnInventoryEvent(event.getWhoClicked());
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        if (ShulkerUtils.isShulkerBox(event.getItem().getItemStack())) {
            updateCacheOnInventoryEvent(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (ShulkerUtils.isShulkerBox(event.getItemDrop().getItemStack())) {
            updateCacheOnInventoryEvent(event.getPlayer());
        }
    }

    private void updateCacheOnInventoryEvent(org.bukkit.entity.HumanEntity whoClicked) {
        if (whoClicked instanceof Player player && ConfigUtil.shulkerModeEnabled && !player.hasPermission("vshulker.mode.bypass")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    shulkerCache.put(player.getUniqueId(), ShulkerUtils.hasShulkerBox(player));
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        shulkerCache.remove(player.getUniqueId());
        removeNotification(player);

        if (ConfigUtil.shulkerModeEnabled && !player.hasPermission("vshulker.mode.bypass")) {
            ShulkerUtils.checkAndDropExcessShulkers(player, Integer.MAX_VALUE);
        }
    }
}