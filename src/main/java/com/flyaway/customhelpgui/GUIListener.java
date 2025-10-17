package com.flyaway.customhelpgui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.inventory.ClickType;

public class GUIListener implements Listener {

    private final CustomHelpGUI plugin;

    public GUIListener(CustomHelpGUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomHelpGUI.HelpGUIHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        ClickType clickType = event.getClick();

        // Определяем тип клика и получаем соответствующую команду
        String command = null;

        if (clickType == ClickType.LEFT) {
            // Получаем команду для ЛКМ
            command = meta.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "left-click-command"),
                PersistentDataType.STRING
            );
        } else if (clickType == ClickType.RIGHT) {
            // Получаем команду для ПКМ
            command = meta.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "right-click-command"),
                PersistentDataType.STRING
            );
        }

        // Если для этого типа клика команды нет, пробуем получить общую команду
        if (command == null) {
            command = meta.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "command"),
                PersistentDataType.STRING
            );
        }

        // Выполняем команду если она есть
        if (command != null && !command.isEmpty()) {
            executeCommand(player, command);
        }
    }

    private void executeCommand(Player player, String command) {
        command = command.replace("%player%", player.getName());

        if (command.equalsIgnoreCase("close")) {
            player.closeInventory();
            return;
        }

        // Определяем исполнителя команды по префиксу
        boolean isPlayerCommand = command.startsWith("player:");

        if (isPlayerCommand) {
            // Убираем префикс player:
            command = command.substring(7);
        }

        // Убираем слеш в начале если он есть
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // Выполняем команду
        if (isPlayerCommand) {
            // Выполняем от имени игрока
            player.performCommand(command);
        } else {
            // Выполняем от имени консоли (по умолчанию)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}
