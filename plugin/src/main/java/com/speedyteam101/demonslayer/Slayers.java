package com.speedyteam101.demonslayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-player state: style, form, merit/rank, demon status and breath. */
public final class Slayers {
    private static final long CAST_COOLDOWN_MS = 600;

    private final DemonSlayerPlugin plugin;
    private final Map<UUID, Double> breath = new HashMap<>();
    private final Map<UUID, Long> nextCast = new HashMap<>();
    /** Form name to flash in the HUD, and until when. */
    private final Map<UUID, String> flash = new HashMap<>();
    private final Map<UUID, Long> flashUntil = new HashMap<>();
    private int ticks;

    public Slayers(DemonSlayerPlugin plugin) {
        this.plugin = plugin;
    }

    // ---- stored data ----

    public Style style(Player p) {
        return Style.parse(p.getPersistentDataContainer().get(Keys.STYLE, PersistentDataType.STRING));
    }

    public int formIndex(Player p) {
        return p.getPersistentDataContainer().getOrDefault(Keys.FORM, PersistentDataType.INTEGER, 0);
    }

    public int merit(Player p) {
        return p.getPersistentDataContainer().getOrDefault(Keys.MERIT, PersistentDataType.INTEGER, 0);
    }

    public Rank rank(Player p) {
        return Rank.of(merit(p));
    }

    public boolean isDemon(Player p) {
        return p.getPersistentDataContainer().getOrDefault(Keys.DEMON, PersistentDataType.BYTE, (byte) 0) == 1;
    }

    public void clearStyle(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.remove(Keys.STYLE);
        pdc.remove(Keys.FORM);
    }

    /** Checks whether the player may learn a style; returns an error message or null. */
    public String canLearn(Player p, Style s) {
        boolean demon = isDemon(p);
        if (s.isDemonArt() && !demon) {
            return "Only demons can use Blood Demon Arts.";
        }
        if (!s.isDemonArt() && demon) {
            return "Demons can't use breathing styles. Choose a Blood Demon Art.";
        }
        if (s.kind() == Style.Kind.SUN && rank(p) != Rank.HASHIRA) {
            return "Sun Breathing can only be learned by a Hashira.";
        }
        return null;
    }

    public boolean learn(Player p, Style s) {
        String error = canLearn(p, s);
        if (error != null) {
            p.sendMessage(Component.text(error, NamedTextColor.RED));
            return false;
        }
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.set(Keys.STYLE, PersistentDataType.STRING, s.name());
        pdc.set(Keys.FORM, PersistentDataType.INTEGER, 0);
        for (ItemStack item : p.getInventory().getContents()) {
            Items.recolor(item, s);
        }
        p.sendMessage(Component.text("You now use ", NamedTextColor.GRAY)
                .append(Component.text(s.displayName(), s.color()).decorate(TextDecoration.BOLD))
                .append(Component.text(". ", NamedTextColor.GRAY))
                .append(Component.text(s.isDemonArt()
                        ? "Press F (swap hands) to use it, sneak + F to switch form."
                        : "Right-click with your Nichirin blade to use it, sneak + right-click to switch form.",
                        NamedTextColor.YELLOW)));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
        return true;
    }

    public void setDemon(Player p, boolean demon) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (demon) {
            pdc.set(Keys.DEMON, PersistentDataType.BYTE, (byte) 1);
        } else {
            pdc.remove(Keys.DEMON);
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
            p.removePotionEffect(PotionEffectType.REGENERATION);
        }
        clearStyle(p);
        if (demon) {
            p.showTitle(Title.title(Component.text("You are a demon", NamedTextColor.DARK_RED),
                    Component.text("Stay out of the sun. Choose a Blood Demon Art: /slayer menu", NamedTextColor.GRAY)));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.4f);
        } else {
            p.showTitle(Title.title(Component.text("You are human again", NamedTextColor.LIGHT_PURPLE),
                    Component.text("Choose a breathing style: /slayer menu", NamedTextColor.GRAY)));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
        }
    }

    public void setMerit(Player p, int merit) {
        Rank before = rank(p);
        p.getPersistentDataContainer().set(Keys.MERIT, PersistentDataType.INTEGER, Math.max(0, merit));
        Rank after = rank(p);
        if (after.ordinal() > before.ordinal()) {
            p.showTitle(Title.title(Component.text("Promoted!", NamedTextColor.GOLD),
                    Component.text("Your rank is now " + after.displayName(), NamedTextColor.YELLOW)));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            Bukkit.broadcast(Component.text(p.getName() + " has been promoted to " + after.displayName() + "!", NamedTextColor.GOLD));
            if (after == Rank.HASHIRA) {
                p.sendMessage(Component.text("As a Hashira you can now learn Sun Breathing: /slayer style sun", NamedTextColor.GOLD));
            }
        }
    }

    public void addMerit(Player p, int amount) {
        setMerit(p, merit(p) + amount);
    }

    // ---- techniques ----

    public double breath(Player p) {
        return breath.getOrDefault(p.getUniqueId(), (double) rank(p).maxBreath());
    }

    public void cast(Player p) {
        Style style = style(p);
        if (style == null) {
            p.sendMessage(Component.text("You haven't chosen a style yet. Use /slayer menu", NamedTextColor.RED));
            return;
        }
        String error = canLearn(p, style);
        if (error != null) {
            p.sendMessage(Component.text(error, NamedTextColor.RED));
            return;
        }
        if (!isDemon(p) && !Items.isNichirin(p.getInventory().getItemInMainHand())) {
            p.sendActionBar(Component.text("Hold a Nichirin blade to use " + style.displayName(), NamedTextColor.RED));
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextCast.getOrDefault(p.getUniqueId(), 0L)) {
            return;
        }
        List<Form> forms = plugin.forms().of(style);
        if (forms.isEmpty()) {
            return;
        }
        Form form = forms.get(Math.floorMod(formIndex(p), forms.size()));
        double b = breath(p);
        if (b < form.cost()) {
            p.sendActionBar(Component.text(isDemon(p) ? "Not enough blood energy!" : "Out of breath! Sneak to use Total Concentration.", NamedTextColor.RED));
            return;
        }
        breath.put(p.getUniqueId(), b - form.cost());
        nextCast.put(p.getUniqueId(), now + CAST_COOLDOWN_MS);
        flash.put(p.getUniqueId(), form.name());
        flashUntil.put(p.getUniqueId(), now + 1500);
        p.getWorld().playSound(p.getLocation(), style.sound(), 0.8f, 1.0f + (float) (Math.random() * 0.2));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.2f);
        form.action().accept(p);
    }

    public void cycle(Player p) {
        Style style = style(p);
        if (style == null) {
            p.sendMessage(Component.text("You haven't chosen a style yet. Use /slayer menu", NamedTextColor.RED));
            return;
        }
        List<Form> forms = plugin.forms().of(style);
        if (forms.isEmpty()) {
            return;
        }
        int next = Math.floorMod(formIndex(p) + 1, forms.size());
        p.getPersistentDataContainer().set(Keys.FORM, PersistentDataType.INTEGER, next);
        flash.put(p.getUniqueId(), forms.get(next).name());
        flashUntil.put(p.getUniqueId(), System.currentTimeMillis() + 1500);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.5f);
        hud(p);
    }

    public void forget(Player p) {
        UUID id = p.getUniqueId();
        breath.remove(id);
        nextCast.remove(id);
        flash.remove(id);
        flashUntil.remove(id);
    }

    // ---- ticking ----

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void tick() {
        ticks++;
        boolean everySecond = ticks % 2 == 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Rank rank = rank(p);
            double regen = 2.5 + rank.ordinal() * 0.25 + (p.isSneaking() ? 6 : 0);
            double b = Math.min(rank.maxBreath(), breath(p) + regen);
            breath.put(p.getUniqueId(), b);
            if (everySecond && isDemon(p)) {
                demonTick(p);
            }
            hud(p);
        }
    }

    private void demonTick(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        World w = p.getWorld();
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, true, false));
        if (!Demons.isDay(w) || w.getEnvironment() != World.Environment.NORMAL) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 0, true, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, true, false));
        }
        if (plugin.getConfig().getBoolean("demons.sunlight-burns", true) && Demons.inSunlight(p)) {
            p.setFireTicks(Math.max(p.getFireTicks(), 60));
            plugin.combat().trueDamage(p, 4);
            p.sendActionBar(Component.text("The sun is burning you! Get into the shade!", NamedTextColor.GOLD));
        }
    }

    private void hud(Player p) {
        Style style = style(p);
        boolean demon = isDemon(p);
        if (style == null || (!demon && !Items.isNichirin(p.getInventory().getItemInMainHand()))) {
            return;
        }
        long now = System.currentTimeMillis();
        String formName;
        if (flashUntil.getOrDefault(p.getUniqueId(), 0L) > now) {
            formName = flash.get(p.getUniqueId());
        } else {
            List<Form> forms = plugin.forms().of(style);
            formName = forms.isEmpty() ? "" : forms.get(Math.floorMod(formIndex(p), forms.size())).name();
        }
        Rank rank = rank(p);
        int max = rank.maxBreath();
        int filled = (int) Math.round(breath(p) / max * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? '|' : '.');
        }
        p.sendActionBar(Component.text(style.displayName(), style.color())
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(Component.text(formName, NamedTextColor.WHITE))
                .append(Component.text("  " + (demon ? "Blood " : "Breath ") + "[", NamedTextColor.GRAY))
                .append(Component.text(bar.toString(), demon ? NamedTextColor.DARK_RED : NamedTextColor.AQUA))
                .append(Component.text("]  " + (demon ? "Demon" : rank.displayName()), NamedTextColor.GRAY)));
    }
}
