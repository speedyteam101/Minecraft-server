package com.speedyteam101.demonslayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Chest menu for choosing a breathing style or Blood Demon Art. */
public final class StyleMenu implements InventoryHolder {
    private final Inventory inventory;
    private final Map<Integer, Style> slots = new HashMap<>();

    private StyleMenu(DemonSlayerPlugin plugin, Player player) {
        boolean demon = plugin.slayers().isDemon(player);
        inventory = Bukkit.createInventory(this, 27,
                Component.text(demon ? "Choose a Blood Demon Art" : "Choose a Breathing Style"));
        Style current = plugin.slayers().style(player);
        int slot = 0;
        for (Style s : Style.values()) {
            if (s.isDemonArt() != demon) {
                continue;
            }
            String error = plugin.slayers().canLearn(player, s);
            ItemStack icon = new ItemStack(error == null ? s.icon() : Material.BARRIER);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(s.displayName(), s.color()).decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, s == current));
            List<Component> lore = new ArrayList<>();
            if (s == current) {
                lore.add(line("Your current style", NamedTextColor.GREEN));
            }
            if (error != null) {
                lore.add(line(error, NamedTextColor.RED));
            }
            for (Form f : plugin.forms().of(s)) {
                lore.add(line("• " + f.name() + " (" + f.cost() + ")", NamedTextColor.GRAY));
            }
            meta.lore(lore);
            icon.setItemMeta(meta);
            inventory.setItem(slot, icon);
            slots.put(slot, s);
            slot++;
        }
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    public static void open(DemonSlayerPlugin plugin, Player player) {
        player.openInventory(new StyleMenu(plugin, player).getInventory());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Handles clicks in the menu. */
    public static final class Clicks implements Listener {
        private final DemonSlayerPlugin plugin;

        public Clicks(DemonSlayerPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder() instanceof StyleMenu menu)) {
                return;
            }
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= menu.inventory.getSize()) {
                return;
            }
            Style s = menu.slots.get(e.getRawSlot());
            if (s != null && plugin.slayers().learn(p, s)) {
                p.closeInventory();
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof StyleMenu) {
                e.setCancelled(true);
            }
        }
    }
}
