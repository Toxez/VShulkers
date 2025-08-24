package org.txtox8729.vshulkers.utils;

import lombok.NonNull;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.txtox8729.vshulkers.VShulkers;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigUtil {
    public static VShulkers plugin;

    public static Sound pickupSound;
    public static float pickupSoundPitch;
    public static float pickupSoundVolume;
    public static boolean pickupSoundEnabled;

    public static Sound denySound;
    public static float denySoundPitch;
    public static float denySoundVolume;
    public static boolean denySoundEnabled;

    public static boolean shulkerOpenEnabled;
    public static String noPermissionMessage;
    public static String reloadSuccessMessage;
    public static String usageMessage;
    public static String noShulkerInContainerMessage;
    public static String limitShulkerReachedMessage;
    public static String limitShulkerDroppedMessage;
    public static String shulkerCommandDenied;
    public static int shulkerLimit;
    public static boolean shulkerLimitEnabled;
    public static List<String> allowedItems;
    public static boolean shulkerAutoAllItems;

    public static boolean disableShulkerOpenInVanish;
    public static boolean disableAutoPickupInVanish;
    public static boolean disableShulkerOpenInGodMode;
    public static boolean disableAutoPickupInGodMode;

    public static String vanishShulkerOpenDeniedMessage;
    public static String godShulkerOpenDeniedMessage;

    public static boolean shulkerModeEnabled;
    public static String shulkerModeBossBarMessage;
    public static BarColor shulkerModeBossBarColor;
    public static BarStyle shulkerModeBossBarStyle;
    public static String shulkerModeNotificationType;
    public static List<InventoryType> bannedContainers;

    public static void init(@NonNull VShulkers pluginInstance) {
        plugin = pluginInstance;
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        initPickupSound(config);
        initDenySound(config);

        shulkerOpenEnabled = config.getBoolean("shulkerOpen.enabled", true);

        noPermissionMessage = HexUtil.translate(config.getString("messages.no-permission-message", "&7[&#D21919✘&7] &7У вас &#D21919нет прав &7на выполнение этой команды!"));
        reloadSuccessMessage = HexUtil.translate(config.getString("messages.reload-success-message", "&7[ CD32✔&7] &7Конфигурация CD32успешно &7перезагружена!"));
        usageMessage = HexUtil.translate(config.getString("messages.usage-message", "&7[&#DBA544★&7] &fИспользование: &#DBA544/vshulker reload"));
        noShulkerInContainerMessage = HexUtil.translate(config.getString("messages.no-shulker-in-container", "&7[&#D21919✘&7] &7Вы &#D21919не можете &7положить сюда шалкер!"));
        limitShulkerReachedMessage = HexUtil.translate(config.getString("messages.limit-shulker-reached", "&7[&#D21919✘&7] &7Вы &#D21919не можете &7хранить более &6%limit% &7шалкеров в инвентаре!"));
        limitShulkerDroppedMessage = HexUtil.translate(config.getString("messages.limit-shulker-dropped", "&7[&#D21919✘&7] &7У вас было выброшено &6%dropped% &7шалкеров, так как лимит &6%limit% &7был превышен!"));
        shulkerCommandDenied = HexUtil.translate(config.getString("messages.shulker-command-denied", "&7[&#DB4444✘&7] &7Вы &#DB4444не можете &7использовать команды с открытым шалкером!"));

        shulkerLimit = config.getInt("settings.limit-shulker-boxes", 3);
        shulkerLimitEnabled = config.getBoolean("settings.shulker-limit-enabled", true);
        allowedItems = config.getStringList("shulker-auto");
        shulkerAutoAllItems = config.getBoolean("shulker-auto-all-items", true);

        disableShulkerOpenInVanish = config.getBoolean("essentials-support.disable-shulker-open-in-vanish", true);
        disableAutoPickupInVanish = config.getBoolean("essentials-support.disable-auto-pickup-in-vanish", true);
        disableShulkerOpenInGodMode = config.getBoolean("essentials-support.disable-shulker-open-in-god-mode", true);
        disableAutoPickupInGodMode = config.getBoolean("essentials-support.disable-auto-pickup-in-god-mode", true);

        vanishShulkerOpenDeniedMessage = HexUtil.translate(config.getString("messages.vanish-shulker-open-denied", "&7[&#D21919✘&7] &7Вы &#D21919не можете &7открыть шалкер в ванише!"));
        godShulkerOpenDeniedMessage = HexUtil.translate(config.getString("messages.god-shulker-open-denied", "&7[&#D21919✘&7] &7Вы &#D21919не можете &7открыть шалкер в режиме бога!"));

        shulkerModeEnabled = config.getBoolean("shulkermode.enabled", true);
        shulkerModeBossBarMessage = HexUtil.translate(config.getString("shulkermode.bossbar-message", "&7[&#D21919✘&7] &cРежим шалкера активен!"));
        shulkerModeBossBarColor = initBarColor(config.getString("shulkermode.bossbar-color", "RED"));
        shulkerModeBossBarStyle = initBarStyle(config.getString("shulkermode.bossbar-style", "SOLID"));
        shulkerModeNotificationType = initNotificationType(config.getString("shulkermode.notification-type", "BOSSBAR"));

        bannedContainers = config.getStringList("banned-containers").stream()
                .map(String::toUpperCase)
                .map(ConfigUtil::parseInventoryType)
                .filter(type -> type != null)
                .collect(Collectors.toList());

        if (bannedContainers.isEmpty()) {
            bannedContainers.addAll(List.of(InventoryType.SHULKER_BOX, InventoryType.ENDER_CHEST));
        }
    }

    private static void initPickupSound(@NonNull FileConfiguration config) {
        ConfigurationSection sec = config.getConfigurationSection("pickupSound");
        String soundName = sec != null ? sec.getString("sound", "ENTITY_ITEM_PICKUP") : "ENTITY_ITEM_PICKUP";
        pickupSound = parseSound(soundName.toUpperCase(), "ENTITY_ITEM_PICKUP");
        pickupSoundPitch = (float) (sec != null ? sec.getDouble("pitch", 1.2) : 1.2);
        pickupSoundVolume = (float) (sec != null ? sec.getDouble("volume", 0.4) : 0.4);
        pickupSoundEnabled = sec != null ? sec.getBoolean("enabled", true) : true;
    }

    private static void initDenySound(@NonNull FileConfiguration config) {
        ConfigurationSection sec = config.getConfigurationSection("denySound");
        String soundName = sec != null ? sec.getString("sound", "BLOCK_NOTE_BLOCK_BASS") : "BLOCK_NOTE_BLOCK_BASS";
        denySound = parseSound(soundName.toUpperCase(), "BLOCK_NOTE_BLOCK_BASS");
        denySoundPitch = (float) (sec != null ? sec.getDouble("pitch", 0.5) : 0.5);
        denySoundVolume = (float) (sec != null ? sec.getDouble("volume", 1.0) : 1.0);
        denySoundEnabled = sec != null ? sec.getBoolean("enabled", true) : true;
    }

    private static Sound parseSound(@NonNull String name, @NonNull String defaultName) {
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Звук '" + name + "' не найден");
            return Sound.valueOf(defaultName);
        }
    }

    private static BarColor initBarColor(@NonNull String color) {
        try {
            return BarColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Цвет боссбара '" + color + "' не найден");
            return BarColor.RED;
        }
    }

    private static BarStyle initBarStyle(@NonNull String style) {
        try {
            return BarStyle.valueOf(style.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Стиль боссбара '" + style + "' не найден");
            return BarStyle.SOLID;
        }
    }

    private static String initNotificationType(@NonNull String type) {
        String upper = type.toUpperCase();
        if (!"BOSSBAR".equals(upper) && !"ACTIONBAR".equals(upper)) {
            plugin.getLogger().warning("Настройка '" + type + "' не поддерживается");
            return "BOSSBAR";
        }
        return upper;
    }

    private static InventoryType parseInventoryType(@NonNull String name) {
        try {
            return InventoryType.valueOf(name);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Инвентарь '" + name + "' не найден");
            return null;
        }
    }
}