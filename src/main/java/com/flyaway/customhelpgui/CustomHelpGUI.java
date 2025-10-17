package com.flyaway.customhelpgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemFlag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CustomHelpGUI extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private File configFile;
    private final LegacyComponentSerializer textSerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");

    // Добавляем ключи для команд левого и правого клика
    private final NamespacedKey LEFT_CLICK_COMMAND_KEY = new NamespacedKey(this, "left-click-command");
    private final NamespacedKey RIGHT_CLICK_COMMAND_KEY = new NamespacedKey(this, "right-click-command");
    private final NamespacedKey COMMAND_KEY = new NamespacedKey(this, "command");

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder(), "config.yml");
        saveDefaultConfig();
        reloadCustomConfig();

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
        if (command.getName().equalsIgnoreCase("helpgui")) {
            if (!sender.hasPermission("customhelpgui.admin")) {
                sender.sendMessage(colorize("&cУ вас нет прав на использование этой команды!"));
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(colorize("&6CustomHelpGUI Commands:"));
                sender.sendMessage(colorize("&e/helpgui reload &7- Перезагрузить конфиг"));
                sender.sendMessage(colorize("&e/helpgui version &7- Информация о плагине"));
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reload":
                    reloadCustomConfig();
                    sender.sendMessage(colorize("&aКонфигурация плагина перезагружена!"));
                    break;

                case "version":
                case "info":
                    sender.sendMessage(colorize("&6CustomHelpGUI &ev" + getDescription().getVersion()));
                    sender.sendMessage(colorize("&7Автор: Kolobochek"));
                    break;

                default:
                    sender.sendMessage(colorize("&cНеизвестная подкоманда! Используйте /helpgui"));
                    break;
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("helpgui")) {
            if (args.length == 1) {
                if (sender.hasPermission("customhelpgui.admin")) {
                    completions.add("reload");
                    completions.add("version");
                }
            }
        }

        return completions;
    }

    public void reloadCustomConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        getLogger().info("Конфигурация перезагружена!");
    }

    public FileConfiguration getCustomConfig() {
        if (config == null) {
            reloadCustomConfig();
        }
        return config;
    }

    private void openHelpGUI(Player player) {
        Component title = processTextWithGradients(config.getString("gui.title", "&aПомощь"));
        int rows = config.getInt("gui.rows", 6);
        int size = rows * 9;

        Inventory gui = Bukkit.createInventory(new HelpGUIHolder(), size, title);
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
            Component name = processTextWithGradients(config.getString(path + ".name", "Item"));
            List<Component> lore = config.getStringList(path + ".lore").stream()
                    .map(this::processTextWithGradients)
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
            item = createSkull(texture, name, lore, itemId, command, leftClickCommand, rightClickCommand);
        }

        return item;
    }

    private ItemStack createSkull(String texture, Component name, List<Component> lore, String itemId,
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

                // Создаем PlayerProfile с текстурой
                com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "CustomHead");
                com.destroystokyo.paper.profile.ProfileProperty property = new com.destroystokyo.paper.profile.ProfileProperty("textures", texture);
                profile.setProperty(property);
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
        return textSerializer.deserialize(text);
    }

    /**
     * Обрабатывает текст с поддержкой градиентов, HEX цветов и стандартных кодов
     */
    private Component processTextWithGradients(String text) {
        if (text == null) return Component.empty();

        // Сначала обрабатываем градиенты
        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (gradientMatcher.find()) {
            String startHex = gradientMatcher.group(1);
            String endHex = gradientMatcher.group(2);
            String content = gradientMatcher.group(3);

            Component gradientComponent = createGradient(content, startHex, endHex);
            String gradientText = textSerializer.serialize(gradientComponent);
            gradientMatcher.appendReplacement(result, Matcher.quoteReplacement(gradientText));
        }
        gradientMatcher.appendTail(result);

        // Затем применяем стандартные цвета и HEX
        return colorize(result.toString());
    }

    /**
     * Создает градиентный текст
     */
    private Component createGradient(String text, String startHex, String endHex) {
        TextColor startColor = TextColor.fromHexString(startHex);
        TextColor endColor = TextColor.fromHexString(endHex);

        if (startColor == null) startColor = TextColor.color(0xFFFFFF);
        if (endColor == null) endColor = TextColor.color(0xFFFFFF);

        Component result = Component.empty();
        int length = text.length();

        if (length == 0) return result;

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) Math.max(1, length - 1);
            TextColor color = interpolateColor(startColor, endColor, ratio);
            result = result.append(Component.text(text.charAt(i)).color(color));
        }

        return result;
    }

    private TextColor interpolateColor(TextColor start, TextColor end, float ratio) {
        int red = (int) (start.red() + (end.red() - start.red()) * ratio);
        int green = (int) (start.green() + (end.green() - start.green()) * ratio);
        int blue = (int) (start.blue() + (end.blue() - start.blue()) * ratio);

        // Ограничиваем значения 0-255
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return TextColor.color(red, green, blue);
    }

    public static class HelpGUIHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
