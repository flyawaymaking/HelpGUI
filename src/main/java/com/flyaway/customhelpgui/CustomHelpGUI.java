package com.flyaway.customhelpgui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomHelpGUI extends JavaPlugin implements Listener {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration config;

    private final NamespacedKey LEFT_CLICK_COMMAND_KEY = new NamespacedKey(this, "left-click-command");
    private final NamespacedKey RIGHT_CLICK_COMMAND_KEY = new NamespacedKey(this, "right-click-command");
    private final NamespacedKey COMMAND_KEY = new NamespacedKey(this, "command");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        getLogger().info("CustomHelpGUI enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomHelpGUI disabled!");
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        if (message.equals("/help") || message.startsWith("/help ")) {
            event.setCancelled(true);
            openHelpGUI(event.getPlayer());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("helpgui")) return false;

        if (!sender.hasPermission("customhelpgui.admin")) {
            sender.sendMessage(colorize(config.getString("messages.no_permission", "<red>У вас нет прав на использование этой команды!")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(colorize(config.getString("messages.command_help", "<gold>CustomHelpGUI Commands...")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                config = getConfig();
                sender.sendMessage(colorize(config.getString("messages.config_reloaded", "<green>Конфигурация плагина перезагружена!")));
                break;

            case "version":
            case "info":
                sender.sendMessage(colorize("<gold>CustomHelpGUI <yellow>v" + getPluginMeta().getVersion()));
                sender.sendMessage(colorize("<gray>Author: golovin12"));
                break;

            default:
                sender.sendMessage(colorize(config.getString("messages.unknown_subcommand", "<red>Неизвестная подкоманда! Используйте /helpgui")));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("helpgui") && args.length == 1 && sender.hasPermission("customhelpgui.admin")) {
            completions.add("reload");
            completions.add("version");
        }
        return completions;
    }

    private void openHelpGUI(Player player) {
        Component title = colorize(config.getString("gui.title", "<green>Помощь"));
        int rows = config.getInt("gui.rows", 6);
        Inventory gui = Bukkit.createInventory(new HelpGUIHolder(), rows * 9, title);
        setupGUIItems(gui, player);

        player.openInventory(gui);
    }

    private void setupGUIItems(Inventory gui, Player player) {
        if (!config.contains("items")) {
            getLogger().warning("No items found in config!");
            return;
        }

        for (String itemKey : config.getConfigurationSection("items").getKeys(false)) {
            String path = "items." + itemKey;

            int slot = config.getInt(path + ".slot", 0);
            String materialName = config.getString(path + ".material", "STONE");
            Component name = colorize(config.getString(path + ".name", "Item"));
            List<Component> lore = config.getStringList(path + ".lore").stream()
                    .map(this::colorize)
                    .collect(Collectors.toList());

            // Получаем команды для разных типов кликов
            String command = config.getString(path + ".command", "");
            String leftClickCommand = config.getString(path + ".left-click-command", "");
            String rightClickCommand = config.getString(path + ".right-click-command", "");

            ItemStack item = createItem(materialName, name, lore, itemKey, command, leftClickCommand, rightClickCommand);

            if (item != null && slot >= 0 && slot < gui.getSize()) {
                gui.setItem(slot, item);
            }
        }
    }

    private ItemStack createItem(String materialName, Component name, List<Component> lore, String itemId,
                                 String command, String leftClickCommand, String rightClickCommand) {
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) {
            getLogger().warning("Invalid material: " + materialName);
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            // Сохраняем все команды в PersistentDataContainer
            if (!command.isEmpty()) {
                meta.getPersistentDataContainer().set(COMMAND_KEY, PersistentDataType.STRING, command);
            }
            if (!leftClickCommand.isEmpty()) {
                meta.getPersistentDataContainer().set(LEFT_CLICK_COMMAND_KEY, PersistentDataType.STRING, leftClickCommand);
            }
            if (!rightClickCommand.isEmpty()) {
                meta.getPersistentDataContainer().set(RIGHT_CLICK_COMMAND_KEY, PersistentDataType.STRING, rightClickCommand);
            }

            item.setItemMeta(meta);
        }

        if (material == Material.PLAYER_HEAD && config.contains("items." + itemId + ".texture")) {
            String texture = config.getString("items." + itemId + ".texture");
            item = createSkull(texture, name, lore, command, leftClickCommand, rightClickCommand);
        }

        return item;
    }

    private ItemStack createSkull(String texture, Component name, List<Component> lore,
                                  String command, String leftClickCommand, String rightClickCommand) {
        try {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.displayName(name);
                meta.lore(lore);

                // Сохраняем все команды в PersistentDataContainer для головы
                if (!command.isEmpty()) {
                    meta.getPersistentDataContainer().set(COMMAND_KEY, PersistentDataType.STRING, command);
                }
                if (!leftClickCommand.isEmpty()) {
                    meta.getPersistentDataContainer().set(LEFT_CLICK_COMMAND_KEY, PersistentDataType.STRING, leftClickCommand);
                }
                if (!rightClickCommand.isEmpty()) {
                    meta.getPersistentDataContainer().set(RIGHT_CLICK_COMMAND_KEY, PersistentDataType.STRING, rightClickCommand);
                }

                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "CustomHead");
                profile.setProperty(new ProfileProperty("textures", texture));
                meta.setPlayerProfile(profile);

                head.setItemMeta(meta);
            }

            return head;

        } catch (Exception e) {
            // Если возникла ошибка, создаем обычную голову
            getLogger().warning("Ошибка при создании головы с текстурой: " + e.getMessage());

            // Fallback - создаем обычную голову
            ItemStack fallbackHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta fallbackMeta = fallbackHead.getItemMeta();
            if (fallbackMeta != null) {
                fallbackMeta.displayName(name);
                fallbackMeta.lore(lore);

                // Сохраняем команды
                if (!command.isEmpty()) {
                    fallbackMeta.getPersistentDataContainer().set(COMMAND_KEY, PersistentDataType.STRING, command);
                }
                if (!leftClickCommand.isEmpty()) {
                    fallbackMeta.getPersistentDataContainer().set(LEFT_CLICK_COMMAND_KEY, PersistentDataType.STRING, leftClickCommand);
                }
                if (!rightClickCommand.isEmpty()) {
                    fallbackMeta.getPersistentDataContainer().set(RIGHT_CLICK_COMMAND_KEY, PersistentDataType.STRING, rightClickCommand);
                }

                fallbackHead.setItemMeta(fallbackMeta);
            }

            return fallbackHead;
        }
    }

    private Component colorize(String text) {
        return miniMessage.deserialize(text);
    }

    public static class HelpGUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
