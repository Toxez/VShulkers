package org.txtox8729.vshulkers;

import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.txtox8729.vshulkers.commands.ReloadCommand;
import org.txtox8729.vshulkers.commands.VTabCompleter;
import org.txtox8729.vshulkers.listeners.ContainerListener;
import org.txtox8729.vshulkers.listeners.LimitListener;
import org.txtox8729.vshulkers.listeners.SHListeners;
import org.txtox8729.vshulkers.listeners.ShulkerModeListener;
import org.txtox8729.vshulkers.utils.ConfigUtil;

import java.util.Optional;

public final class VShulkers extends JavaPlugin {

    private Essentials essentials;
    private ShulkerModeListener shulkerModeListener;

    @Override
    public void onEnable() {
        ConfigUtil.init(this);

        essentials = (Essentials) Optional.ofNullable(Bukkit.getPluginManager().getPlugin("Essentials")).orElse(null);

        Bukkit.getPluginManager().registerEvents(new SHListeners(), this);
        Bukkit.getPluginManager().registerEvents(new ContainerListener(), this);

        shulkerModeListener = new ShulkerModeListener(this);
        Bukkit.getPluginManager().registerEvents(shulkerModeListener, this);

        LimitListener limitListener = new LimitListener(this);
        Bukkit.getPluginManager().registerEvents(limitListener, this);

        getCommand("vshulker").setExecutor(new ReloadCommand());
        getCommand("vshulker").setTabCompleter(new VTabCompleter());

        getLogger().info(ChatColor.GREEN + "Плагин VShulker успешно включен! версия: 1.7");
        getLogger().info(ChatColor.GREEN + "Автор: Tox_8729");
    }

    @Override
    public void onDisable() {
        Optional.ofNullable(shulkerModeListener).ifPresent(ShulkerModeListener::cleanup);
    }

    public void reloadPlugin() {
        reloadConfig();
        ConfigUtil.init(this);
    }

    public Essentials getEssentials() {
        return essentials;
    }
}