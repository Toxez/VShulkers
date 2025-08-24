package org.txtox8729.vshulkers.listeners;

import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.txtox8729.vshulkers.VShulkers;
import org.txtox8729.vshulkers.utils.ConfigUtil;
import org.txtox8729.vshulkers.utils.ShulkerUtils;

import java.util.Set;
import java.util.stream.Stream;

public class ContainerListener implements Listener {

    private static final Set<Material> SHULKER_BOXES = ShulkerUtils.getShulkerMaterials();

    private static final long MESSAGE_COOLDOWN = 1000L;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = event.getView().getTopInventory();
        if (!ConfigUtil.bannedContainers.contains(topInventory.getType())) return;

        if (player.hasPermission("vshulker.bypass")) return;

        if (checkForShulker(event, player) || ShulkerUtils.isShulkerInContainer(topInventory)) {
            event.setCancelled(true);
            sendMessageWithCooldown(player, ConfigUtil.noShulkerInContainerMessage);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = event.getView().getTopInventory();
        if (!ConfigUtil.bannedContainers.contains(topInventory.getType())) return;

        if (player.hasPermission("vshulker.bypass")) return;

        ItemStack cursor = event.getOldCursor();
        if (ShulkerUtils.isShulkerBox(cursor) || ShulkerUtils.isShulkerInContainer(topInventory)) {
            event.setCancelled(true);
            sendMessageWithCooldown(player, ConfigUtil.noShulkerInContainerMessage);
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (ShulkerUtils.isShulkerBox(event.getItem()) &&
                (ConfigUtil.bannedContainers.contains(event.getDestination().getType()) ||
                        ConfigUtil.bannedContainers.contains(event.getSource().getType()) ||
                        ConfigUtil.bannedContainers.contains(event.getInitiator().getType()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (ShulkerUtils.isShulkerBox(event.getItem().getItemStack()) && ConfigUtil.bannedContainers.contains(event.getInventory().getType())) {
            event.setCancelled(true);
            if (event.getInventory().getHolder() instanceof Player player) {
                sendMessageWithCooldown(player, ConfigUtil.noShulkerInContainerMessage);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            player.removeMetadata("shulkerMessageCooldown", VShulkers.getPlugin(VShulkers.class));
        }
    }

    private boolean checkForShulker(@NonNull InventoryClickEvent event, @NonNull Player player) {
        return Stream.of(event.getCursor(), event.getCurrentItem(), player.getInventory().getItemInOffHand())
                .anyMatch(ShulkerUtils::isShulkerBox) ||
                (event.getAction() == InventoryAction.HOTBAR_SWAP && checkHotbarSlot(event.getHotbarButton(), player)) ||
                (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && ShulkerUtils.isShulkerBox(event.getCurrentItem())) ||
                (event.getClick() == ClickType.SWAP_OFFHAND && ShulkerUtils.isShulkerBox(player.getInventory().getItemInOffHand())) ||
                (event.getClick() == ClickType.NUMBER_KEY && checkHotbarSlot(event.getHotbarButton(), player)) ||
                (event.getAction() == InventoryAction.SWAP_WITH_CURSOR && (ShulkerUtils.isShulkerBox(event.getCursor()) || ShulkerUtils.isShulkerBox(event.getCurrentItem())));
    }

    private boolean checkHotbarSlot(int slot, Player player) {
        return slot >= 0 && slot < player.getInventory().getSize() && ShulkerUtils.isShulkerBox(player.getInventory().getItem(slot));
    }

    private void sendMessageWithCooldown(@NonNull Player player, @NonNull String message) {
        long currentTime = System.currentTimeMillis();
        String metaKey = "shulkerMessageCooldown";

        if (player.hasMetadata(metaKey) && currentTime - player.getMetadata(metaKey).get(0).asLong() < MESSAGE_COOLDOWN) {
            return;
        }

        player.sendMessage(message);
        if (ConfigUtil.denySoundEnabled) {
            player.playSound(player.getLocation(), ConfigUtil.denySound, ConfigUtil.denySoundVolume, ConfigUtil.denySoundPitch);
        }
        player.setMetadata(metaKey, new FixedMetadataValue(VShulkers.getPlugin(VShulkers.class), currentTime));
    }
}