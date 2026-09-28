package com.speedyteam101.demonslayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Custom items, recognised by a persistent data tag. */
public final class Items {
    public static final String NICHIRIN = "nichirin_blade";
    public static final String UNIFORM = "slayer_uniform";
    public static final String DEMON_BLOOD = "demon_blood";
    public static final String CURE = "tamayo_cure";
    public static final List<String> ALL = List.of(NICHIRIN, UNIFORM, DEMON_BLOOD, CURE);

    private Items() {
    }

    private static Component text(String s, TextColor color) {
        return Component.text(s, color).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack tag(ItemStack item, ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(Keys.ITEM, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    public static String id(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(Keys.ITEM, PersistentDataType.STRING);
    }

    public static boolean isNichirin(ItemStack item) {
        return NICHIRIN.equals(id(item));
    }

    public static ItemStack create(String id) {
        return switch (id) {
            case NICHIRIN -> nichirin(null);
            case UNIFORM -> uniform();
            case DEMON_BLOOD -> demonBlood();
            case CURE -> cure();
            default -> null;
        };
    }

    /** A Nichirin blade; it takes on the colour of the wielder's breathing style. */
    public static ItemStack nichirin(Style style) {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        styleBlade(meta, style);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        return tag(item, meta, NICHIRIN);
    }

    /** Recolours a Nichirin blade's name for a style. */
    public static void recolor(ItemStack item, Style style) {
        if (!isNichirin(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        styleBlade(meta, style);
        item.setItemMeta(meta);
    }

    private static void styleBlade(ItemMeta meta, Style style) {
        boolean colored = style != null && style.bladeColor() != null;
        meta.displayName(colored
                ? text("Nichirin Blade (" + style.bladeColor() + ")", style.color()).decorate(TextDecoration.BOLD)
                : text("Nichirin Blade", NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(text("Forged from Scarlet Crimson Iron Sand,", NamedTextColor.GRAY));
        lore.add(text("which absorbs sunlight all year round.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(text("Right-click: use your breathing form", NamedTextColor.YELLOW));
        lore.add(text("Sneak + right-click: switch form", NamedTextColor.YELLOW));
        lore.add(text("Stops demons regenerating and can kill them.", NamedTextColor.RED));
        meta.lore(lore);
    }

    public static ItemStack uniform() {
        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(25, 25, 35));
        meta.displayName(text("Demon Slayer Uniform", NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD));
        meta.lore(List.of(text("Woven from special fibres that", NamedTextColor.GRAY),
                text("lesser demons' claws can't tear.", NamedTextColor.GRAY)));
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3, true);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
        return tag(item, meta, UNIFORM);
    }

    public static ItemStack demonBlood() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(110, 0, 0));
        meta.displayName(text("Muzan's Blood", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));
        meta.lore(List.of(text("Drink to become a demon.", NamedTextColor.RED),
                text("Demons are strong at night, regenerate,", NamedTextColor.GRAY),
                text("and burn to ash in sunlight.", NamedTextColor.GRAY)));
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        return tag(item, meta, DEMON_BLOOD);
    }

    public static ItemStack cure() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(170, 110, 220));
        meta.displayName(text("Tamayo's Cure", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        meta.lore(List.of(text("Drink to turn a demon back into a human.", NamedTextColor.GRAY)));
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        return tag(item, meta, CURE);
    }

    public static List<NamespacedKey> recipeKeys() {
        return List.of(Keys.RECIPE_NICHIRIN, Keys.RECIPE_UNIFORM, Keys.RECIPE_CURE);
    }

    public static void registerRecipes() {
        ShapedRecipe blade = new ShapedRecipe(Keys.RECIPE_NICHIRIN, nichirin(null));
        blade.shape(" I ", "GIG", " S ");
        blade.setIngredient('I', Material.IRON_BLOCK);
        blade.setIngredient('G', Material.GLOWSTONE_DUST);
        blade.setIngredient('S', Material.STICK);

        ShapedRecipe uniform = new ShapedRecipe(Keys.RECIPE_UNIFORM, uniform());
        uniform.shape("W W", "WWW", "WWW");
        uniform.setIngredient('W', Material.BLACK_WOOL);

        ShapelessRecipe cure = new ShapelessRecipe(Keys.RECIPE_CURE, cure());
        cure.addIngredient(Material.GOLDEN_APPLE);
        cure.addIngredient(Material.ALLIUM);
        cure.addIngredient(Material.LILAC);
        cure.addIngredient(Material.GLISTERING_MELON_SLICE);

        for (Recipe r : List.<Recipe>of(blade, uniform, cure)) {
            NamespacedKey key = ((org.bukkit.Keyed) r).getKey();
            Bukkit.removeRecipe(key);
            Bukkit.addRecipe(r);
        }
    }
}
