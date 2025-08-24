package org.txtox8729.vshulkers.listeners;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.txtox8729.vshulkers.VShulkers;
import org.txtox8729.vshulkers.utils.ConfigUtil;
import org.txtox8729.vshulkers.utils.ShulkerUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SHListeners implements Listener {

    private final Map<UUID, Inventory> openShulkers = new HashMap<>();
    private final Map<UUID, ShulkerInfo> shulkerInfoMap = new HashMap<>();
    private static final NamespacedKey SHULKER_OPEN_KEY = new NamespacedKey(VShulkers.getPlugin(VShulkers.class), "shulker-open");

    @Getter
    @AllArgsConstructor
    private static class ShulkerInfo {
        private final ItemStack item;
        private final int slot;
    }

    private Optional<Essentials> getEssentials() {
        return Optional.ofNullable(VShulkers.getPlugin(VShulkers.class).getEssentials());
    }

    private boolean isVanished(@NonNull Player player) {
        return getEssentials().map(ess -> ess.getUser(player.getUniqueId())).map(User::isVanished).orElse(false);
    }

    private boolean isGodMode(@NonNull Player player) {
        return getEssentials().map(ess -> ess.getUser(player.getUniqueId())).map(User::isGodModeEnabled).orElse(false);
    }

    @EventHandler
    public void onPickupAttempt(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();

        if ((ConfigUtil.disableAutoPickupInVanish && isVanished(player)) ||
                (ConfigUtil.disableAutoPickupInGodMode && isGodMode(player)) ||
                isShulkerOpen(player)) {
            event.setCancelled(true);
            return;
        }

        if (player.getInventory().firstEmpty() == -1 && tryAddToShulker(player, item)) {
            event.setCancelled(true);
            event.getItem().remove();
            playPickupSound(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        if (isShulkerOpen(player) && clickedInventory.getType() == InventoryType.SHULKER_BOX) {
            scheduleUpdate(player, clickedInventory);
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && isShulkerOpen(player)) {
            Inventory destination = (clickedInventory == player.getInventory()) ? event.getView().getTopInventory() : player.getInventory();
            if (destination.getType() == InventoryType.SHULKER_BOX) {
                scheduleUpdate(player, destination);
            }
        }

        if (event.getClick().isRightClick() && clickedInventory.getType() == InventoryType.PLAYER && ShulkerUtils.isShulkerBox(event.getCurrentItem())) {
            if ((ConfigUtil.disableShulkerOpenInVanish && isVanished(player)) ||
                    (ConfigUtil.disableShulkerOpenInGodMode && isGodMode(player))) {
                player.sendMessage(ConfigUtil.disableShulkerOpenInVanish ? ConfigUtil.vanishShulkerOpenDeniedMessage : ConfigUtil.godShulkerOpenDeniedMessage);
                event.setCancelled(true);
                return;
            }

            if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                event.setCancelled(true);
                return;
            }

            openShulker(player, event.getCurrentItem(), event.getSlot());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (isShulkerOpen(player) && event.getInventory().getType() == InventoryType.SHULKER_BOX) {
            scheduleUpdate(player, event.getInventory());
        }
    }

    private boolean tryAddToShulker(@NonNull Player player, @NonNull ItemStack item) {
        if (ConfigUtil.shulkerAutoAllItems && !ConfigUtil.allowedItems.contains(item.getType().toString())) {
            return false;
        }

        return java.util.Arrays.stream(player.getInventory().getContents())
                .filter(ShulkerUtils::isShulkerBox)
                .anyMatch(shulker -> {
                    BlockStateMeta meta = (BlockStateMeta) shulker.getItemMeta();
                    if (meta == null) return false;
                    ShulkerBox box = (ShulkerBox) meta.getBlockState();
                    if (box.getInventory().addItem(item).isEmpty()) {
                        meta.setBlockState(box);
                        shulker.setItemMeta(meta);
                        player.updateInventory();
                        return true;
                    }
                    return false;
                });
    }

    private void playPickupSound(@NonNull Player player) {
        if (ConfigUtil.pickupSoundEnabled && ConfigUtil.pickupSound != null) {
            player.playSound(player.getLocation(), ConfigUtil.pickupSound, SoundCategory.PLAYERS, ConfigUtil.pickupSoundVolume, ConfigUtil.pickupSoundPitch);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!ShulkerUtils.isShulkerBox(item)) return;

        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        boolean shulkerOpenEnabled = ConfigUtil.shulkerOpenEnabled;
        boolean shouldOpen = event.getAction() == Action.RIGHT_CLICK_AIR &&
                ((isCreative && !player.isSneaking()) || (!isCreative && (shulkerOpenEnabled ? !player.isSneaking() : player.isSneaking())));

        if (shouldOpen) {
            if ((ConfigUtil.disableShulkerOpenInVanish && isVanished(player)) ||
                    (ConfigUtil.disableShulkerOpenInGodMode && isGodMode(player))) {
                player.sendMessage(ConfigUtil.disableShulkerOpenInVanish ? ConfigUtil.vanishShulkerOpenDeniedMessage : ConfigUtil.godShulkerOpenDeniedMessage);
                event.setCancelled(true);
                return;
            }

            if (!isCreative && player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                event.setCancelled(true);
                return;
            }

            player.setMetadata("shulkerOpening", new FixedMetadataValue(VShulkers.getPlugin(VShulkers.class), true));
            openShulker(player, item, player.getInventory().getHeldItemSlot());
            event.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("shulkerOpening", VShulkers.getPlugin(VShulkers.class));
                }
            }.runTaskLater(VShulkers.getPlugin(VShulkers.class), 2L);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        Inventory inv = openShulkers.remove(uuid);
        ShulkerInfo info = shulkerInfoMap.remove(uuid);
        player.removeMetadata(SHULKER_OPEN_KEY.getKey(), VShulkers.getPlugin(VShulkers.class));

        if (inv != null && info != null && ShulkerUtils.isShulkerBox(info.getItem())) {
            ShulkerUtils.updateShulkerContents(inv, info.getItem());
            if (info.getSlot() >= 0 && info.getSlot() < player.getInventory().getSize()) {
                player.getInventory().setItem(info.getSlot(), info.getItem());
            }

            if (!player.hasPermission("vshulker.limit")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            ShulkerUtils.checkAndDropExcessShulkers(player);
                        }
                    }
                }.runTaskLater(VShulkers.getPlugin(VShulkers.class), 5L);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        closeShulkerOnEvent(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        closeShulkerOnEvent(event.getEntity());
    }

    private void closeShulkerOnEvent(@NonNull Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = openShulkers.remove(uuid);
        ShulkerInfo info = shulkerInfoMap.remove(uuid);
        if (inv != null && info != null) {
            ShulkerUtils.updateShulkerContents(inv, info.getItem());
        }
    }

    private boolean isShulkerOpen(@NonNull Player player) {
        return player.hasMetadata(SHULKER_OPEN_KEY.getKey());
    }

    private void openShulker(@NonNull Player player, ItemStack item, int slot) {
        if (!ShulkerUtils.isShulkerBox(item) || isShulkerOpen(player)) return;

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null || !(meta.getBlockState() instanceof ShulkerBox box)) return;

        UUID uuid = player.getUniqueId();
        openShulkers.put(uuid, box.getInventory());
        shulkerInfoMap.put(uuid, new ShulkerInfo(item, slot));
        player.setMetadata(SHULKER_OPEN_KEY.getKey(), new FixedMetadataValue(VShulkers.getPlugin(VShulkers.class), true));
        player.openInventory(box.getInventory());
    }

    private void scheduleUpdate(@NonNull Player player, @NonNull Inventory shulkerInventory) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = player.getUniqueId();
                ShulkerInfo info = shulkerInfoMap.get(uuid);
                if (info != null && ShulkerUtils.isShulkerBox(info.getItem())) {
                    ShulkerUtils.updateShulkerContents(shulkerInventory, info.getItem());
                    if (info.getSlot() >= 0 && info.getSlot() < player.getInventory().getSize()) {
                        player.getInventory().setItem(info.getSlot(), info.getItem());
                    }
                }
            }
        }.runTaskLater(VShulkers.getPlugin(VShulkers.class), 1L);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (ShulkerUtils.isShulkerBox(event.getItemDrop().getItemStack()) && (isShulkerOpen(player) || player.hasMetadata("shulkerOpening"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isShulkerOpen(player) || player.hasMetadata("shulkerOpening")) {
            player.sendMessage(ConfigUtil.shulkerCommandDenied);
            event.setCancelled(true);
        }
    }
}