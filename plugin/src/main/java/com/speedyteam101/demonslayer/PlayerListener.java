package com.speedyteam101.demonslayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Technique input, special items, demon hunger and the starter kit. */
public final class PlayerListener implements Listener {
    private final DemonSlayerPlugin plugin;

    public PlayerListener(DemonSlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /** Right-click with a Nichirin blade: use a form; sneak + right-click: switch form. */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = e.getPlayer();
        if (!Items.isNichirin(p.getInventory().getItemInMainHand()) || plugin.slayers().isDemon(p)) {
            return;
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
                && e.getClickedBlock().getType().isInteractable() && !p.isSneaking()) {
            return; // let players open doors and chests while holding the blade
        }
        if (p.isSneaking()) {
            plugin.slayers().cycle(p);
        } else {
            plugin.slayers().cast(p);
        }
    }

    /** Swap-hands key (F): demons use their Blood Demon Art, slayers can use it as a second cast key. */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        boolean demon = plugin.slayers().isDemon(p);
        if (plugin.slayers().style(p) == null || (!demon && !Items.isNichirin(p.getInventory().getItemInMainHand()))) {
            return;
        }
        e.setCancelled(true);
        if (p.isSneaking()) {
            plugin.slayers().cycle(p);
        } else {
            plugin.slayers().cast(p);
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        String id = Items.id(e.getItem());
        if (id == null) {
            return;
        }
        Player p = e.getPlayer();
        if (Items.DEMON_BLOOD.equals(id)) {
            if (plugin.slayers().isDemon(p)) {
                p.sendMessage(Component.text("You are already a demon.", NamedTextColor.RED));
                e.setCancelled(true);
                return;
            }
            plugin.slayers().setDemon(p, true);
        } else if (Items.CURE.equals(id)) {
            if (!plugin.slayers().isDemon(p)) {
                p.sendMessage(Component.text("You are human. The cure does nothing.", NamedTextColor.GRAY));
                e.setCancelled(true);
                return;
            }
            plugin.slayers().setDemon(p, false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        // Demons don't need food.
        if (e.getEntity() instanceof Player p && plugin.slayers().isDemon(p) && e.getFoodLevel() < p.getFoodLevel()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        p.discoverRecipes(Items.recipeKeys());
        if (p.getPersistentDataContainer().has(Keys.JOINED, PersistentDataType.BYTE)) {
            return;
        }
        p.getPersistentDataContainer().set(Keys.JOINED, PersistentDataType.BYTE, (byte) 1);
        if (plugin.getConfig().getBoolean("starter-kit", true)) {
            for (ItemStack item : new ItemStack[]{Items.nichirin(null), Items.uniform()}) {
                p.getInventory().addItem(item).values().forEach(left -> p.getWorld().dropItem(p.getLocation(), left));
            }
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) {
                return;
            }
            Demons.crow(p, "Welcome to the Demon Slayer Corps, " + p.getName() + "!");
            p.sendMessage(Component.text("1. Choose your breathing style: ", NamedTextColor.GRAY)
                    .append(Component.text("/slayer menu", NamedTextColor.YELLOW)));
            p.sendMessage(Component.text("2. Right-click with your Nichirin blade to use a form, sneak + right-click to switch.", NamedTextColor.GRAY));
            p.sendMessage(Component.text("3. Hunt demons at night. They only die to a Nichirin blade or sunlight.", NamedTextColor.GRAY));
            p.sendMessage(Component.text("4. Earn merit to rise from Mizunoto to Hashira: ", NamedTextColor.GRAY)
                    .append(Component.text("/slayer info", NamedTextColor.YELLOW)));
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.slayers().forget(e.getPlayer());
        plugin.combat().forget(e.getPlayer().getUniqueId());
    }
}
