package org.txtox8729.vshulkers.listeners;

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

import java.util.Map;
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
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        playerBossBars.values().forEach(BossBar::removeAll);
        playerBossBars.clear();
        shulkerCache.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (ConfigUtil.shulkerModeNotificationType.equals("ACTIONBAR")) {
                player.sendActionBar("");
            }
        }
    }

    private void startShulkerModeCheckTask() {
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!ConfigUtil.shulkerModeEnabled) {
                    playerBossBars.values().forEach(BossBar::removeAll);
                    playerBossBars.clear();
                    shulkerCache.clear();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (ConfigUtil.shulkerModeNotificationType.equals("ACTIONBAR")) {
                            player.sendActionBar("");
                        }
                    }
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("vshulker.mode.bypass")) {
                        shulkerCache.remove(player.getUniqueId());
                        removeNotification(player);
                        continue;
                    }

                    Boolean hasShulker = shulkerCache.get(player.getUniqueId());
                    if (hasShulker == null || hasShulker) {
                        hasShulker = hasShulkerBox(player);
                        shulkerCache.put(player.getUniqueId(), hasShulker);
                    }

                    if (hasShulker) {
                        showNotification(player);
                    } else {
                        removeNotification(player);
                    }
                }
            }
        };
        checkTask.runTaskTimer(plugin, 0L, 20L);
    }

    private boolean hasShulkerBox(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isShulkerBox(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShulkerBox(ItemStack item) {
        return item != null && item.getType().name().endsWith("_SHULKER_BOX");
    }

    private void showNotification(Player player) {
        if (ConfigUtil.shulkerModeNotificationType.equals("BOSSBAR")) {
            showBossBar(player);
        } else if (ConfigUtil.shulkerModeNotificationType.equals("ACTIONBAR")) {
            showActionBar(player);
        }
    }

    private void removeNotification(Player player) {
        if (ConfigUtil.shulkerModeNotificationType.equals("BOSSBAR")) {
            removeBossBar(player);
        } else if (ConfigUtil.shulkerModeNotificationType.equals("ACTIONBAR")) {
            player.sendActionBar("");
        }
    }

    private void showBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bossBar = playerBossBars.computeIfAbsent(uuid, k ->
                Bukkit.createBossBar(ConfigUtil.shulkerModeBossBarMessage, ConfigUtil.shulkerModeBossBarColor, ConfigUtil.shulkerModeBossBarStyle)
        );
        bossBar.setProgress(1.0);

        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }
    }

    private void removeBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bossBar = playerBossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    private void showActionBar(Player player) {
        player.sendActionBar(ConfigUtil.shulkerModeBossBarMessage);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !ConfigUtil.shulkerModeEnabled || player.hasPermission("vshulker.mode.bypass")) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                shulkerCache.put(player.getUniqueId(), hasShulkerBox(player));
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !ConfigUtil.shulkerModeEnabled || player.hasPermission("vshulker.mode.bypass")) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                shulkerCache.put(player.getUniqueId(), hasShulkerBox(player));
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        if (!ConfigUtil.shulkerModeEnabled || event.getPlayer().hasPermission("vshulker.mode.bypass")) {
            return;
        }
        if (isShulkerBox(event.getItem().getItemStack())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    shulkerCache.put(event.getPlayer().getUniqueId(), hasShulkerBox(event.getPlayer()));
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!ConfigUtil.shulkerModeEnabled || event.getPlayer().hasPermission("vshulker.mode.bypass")) {
            return;
        }
        if (isShulkerBox(event.getItemDrop().getItemStack())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    shulkerCache.put(event.getPlayer().getUniqueId(), hasShulkerBox(event.getPlayer()));
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!ConfigUtil.shulkerModeEnabled || player.hasPermission("vshulker.mode.bypass")) {
            shulkerCache.remove(player.getUniqueId());
            removeNotification(player);
            return;
        }

        ItemStack[] inventoryContents = player.getInventory().getContents();
        int droppedCount = 0;

        for (int i = 0; i < inventoryContents.length; i++) {
            ItemStack item = inventoryContents[i];
            if (isShulkerBox(item)) {
                player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                player.getInventory().setItem(i, null);
                droppedCount++;
            }
        }

        if (droppedCount > 0) {
            player.updateInventory();
        }

        shulkerCache.remove(player.getUniqueId());
        removeNotification(player);
    }
}