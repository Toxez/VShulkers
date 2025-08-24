package org.txtox8729.vshulkers.utils;

import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShulkerUtils {

    private static final Set<Material> SHULKER_MATERIALS = getShulkerMaterials();

    public static Set<Material> getShulkerMaterials() {
        return Stream.of(Material.values())
                .filter(material -> material.name().endsWith("SHULKER_BOX"))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isShulkerBox(ItemStack item) {
        return item != null && SHULKER_MATERIALS.contains(item.getType()) &&
                item.getItemMeta() instanceof BlockStateMeta meta &&
                meta.getBlockState() instanceof ShulkerBox;
    }

    public static boolean hasShulkerBox(@NonNull Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .anyMatch(ShulkerUtils::isShulkerBox);
    }

    public static int countShulkerBoxes(ItemStack[] contents) {
        return (int) Arrays.stream(contents).filter(ShulkerUtils::isShulkerBox).count();
    }

    public static boolean isShulkerInContainer(@NonNull Inventory inventory) {
        return Arrays.stream(inventory.getContents()).anyMatch(ShulkerUtils::isShulkerBox);
    }

    public static void updateShulkerContents(@NonNull Inventory inv, @NonNull ItemStack shulkerItem) {
        if (!isShulkerBox(shulkerItem)) return;

        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        if (meta == null) return;

        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        box.getInventory().setContents(inv.getContents());
        meta.setBlockState(box);
        shulkerItem.setItemMeta(meta);
    }

    public static void checkAndDropExcessShulkers(@NonNull Player player) {
        checkAndDropExcessShulkers(player, ConfigUtil.shulkerLimit);
    }

    public static void checkAndDropExcessShulkers(@NonNull Player player, int limit) {
        ItemStack[] contents = player.getInventory().getContents();
        int shulkerCount = countShulkerBoxes(contents);
        if (shulkerCount <= limit) return;

        int dropped = 0;
        for (int i = 0; i < contents.length; i++) {
            if (dropped >= shulkerCount - limit) break;
            if (isShulkerBox(contents[i])) {
                player.getWorld().dropItemNaturally(player.getLocation(), contents[i].clone());
                player.getInventory().setItem(i, null);
                dropped++;
            }
        }

        if (dropped > 0) {
            player.updateInventory();
            player.sendMessage(ConfigUtil.limitShulkerDroppedMessage
                    .replace("%limit%", String.valueOf(limit))
                    .replace("%dropped%", String.valueOf(dropped)));
        }
    }
}