package com.speedyteam101.demonslayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.speedyteam101.demonslayer.OnHit.effect;
import static com.speedyteam101.demonslayer.OnHit.freeze;
import static com.speedyteam101.demonslayer.OnHit.knockBack;
import static com.speedyteam101.demonslayer.OnHit.none;
import static com.speedyteam101.demonslayer.OnHit.pull;

/** Demon mobs: spawning, sunlight, regeneration, special attacks, boss bars and rewards. */
public final class Demons implements Listener {
    @FunctionalInterface
    private interface Ability {
        void use(LivingEntity demon, Player target);
    }

    private static final Fx BLOOD = Fx.dust(Color.fromRGB(150, 0, 0), 1.6f);
    private static final Fx THREAD = Fx.dust(Color.fromRGB(170, 0, 0), 1.0f);
    private static final Fx FLESH = Fx.dust(Color.fromRGB(200, 120, 140), 1.8f);
    private static final Fx DREAM = Fx.of(Particle.SPELL_WITCH);
    private static final Fx ICE = Fx.of(Particle.SNOWFLAKE);
    private static final Fx MOON = Fx.dust(Color.fromRGB(150, 40, 180), 1.8f);
    private static final Fx CRIT = Fx.of(Particle.CRIT);
    private static final Fx CLOUD = Fx.of(Particle.CLOUD);
    private static final Fx SMOKE = Fx.of(Particle.SMOKE_LARGE);
    private static final Fx NOTE = Fx.of(Particle.NOTE);
    private static final Fx BALL = Fx.dust(Color.fromRGB(230, 60, 60), 2.0f);
    private static final Fx ASH = Fx.of(Particle.LAVA);

    private final DemonSlayerPlugin plugin;
    private final Random random = new Random();
    private final Set<UUID> tracked = new HashSet<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, Integer> cooldowns = new HashMap<>();
    private final Map<DemonType, List<Ability>> abilities = new EnumMap<>(DemonType.class);
    private int seconds;

    public Demons(DemonSlayerPlugin plugin) {
        this.plugin = plugin;
        registerAbilities();
    }

    // ---- helpers ----

    public static boolean isDay(World world) {
        long t = world.getTime();
        return t < 12300 || t > 23850;
    }

    public static boolean inSunlight(LivingEntity e) {
        World w = e.getWorld();
        return w.getEnvironment() == World.Environment.NORMAL && isDay(w) && !w.hasStorm()
                && e.getEyeLocation().getBlock().getLightFromSky() == 15;
    }

    public DemonType typeOf(Entity e) {
        if (e == null) {
            return null;
        }
        return DemonType.parse(e.getPersistentDataContainer().get(Keys.DEMON_TYPE, PersistentDataType.STRING));
    }

    public boolean isDemonMob(Entity e) {
        return !(e instanceof Player) && typeOf(e) != null;
    }

    private static void attribute(LivingEntity e, Attribute attribute, double value) {
        AttributeInstance a = e.getAttribute(attribute);
        if (a != null) {
            a.setBaseValue(value);
        }
    }

    private static double maxHealth(LivingEntity e) {
        AttributeInstance a = e.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return a == null ? 20 : a.getValue();
    }

    // ---- spawning ----

    public LivingEntity spawn(DemonType type, Location at) {
        Entity entity = at.getWorld().spawnEntity(at, type.entity());
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return null;
        }
        apply(living, type);
        return living;
    }

    /** Turns a living entity into a demon of the given type. */
    public void apply(LivingEntity e, DemonType type) {
        e.getPersistentDataContainer().set(Keys.DEMON_TYPE, PersistentDataType.STRING, type.name());
        attribute(e, Attribute.GENERIC_MAX_HEALTH, type.health());
        e.setHealth(type.health());
        attribute(e, Attribute.GENERIC_ATTACK_DAMAGE, type.damage());
        attribute(e, Attribute.GENERIC_MOVEMENT_SPEED, type.speed());
        attribute(e, Attribute.GENERIC_FOLLOW_RANGE, 40);
        e.customName(type.displayName());
        e.setCustomNameVisible(true);
        if (type.isBoss()) {
            attribute(e, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
            attribute(e, Attribute.GENERIC_ARMOR, 10);
            e.setRemoveWhenFarAway(false);
            e.setPersistent(true);
            e.setCanPickupItems(false);
        }
        EntityEquipment eq = e.getEquipment();
        if (eq != null) {
            switch (type) {
                case RUI, AKAZA, MUZAN -> eq.setItemInMainHand(new ItemStack(Material.AIR));
                case KOKUSHIBO -> eq.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
                default -> {
                }
            }
            if (type.isBoss()) {
                eq.setItemInMainHandDropChance(0f);
                eq.setHelmetDropChance(0f);
            }
        }
        track(e);
    }

    private void track(LivingEntity e) {
        tracked.add(e.getUniqueId());
        DemonType type = typeOf(e);
        if (type != null && type.isBoss() && !bars.containsKey(e.getUniqueId())) {
            BossBar bar = Bukkit.createBossBar(type.fullName(), type.barColor(), BarStyle.SEGMENTED_10);
            bars.put(e.getUniqueId(), bar);
        }
    }

    private void untrack(UUID id) {
        tracked.remove(id);
        cooldowns.remove(id);
        plugin.combat().forget(id);
        BossBar bar = bars.remove(id);
        if (bar != null) {
            bar.removeAll();
        }
    }

    public int bossCount() {
        return bars.size();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }
        if (e.getEntityType() != org.bukkit.entity.EntityType.ZOMBIE && e.getEntityType() != org.bukkit.entity.EntityType.HUSK) {
            return;
        }
        if (e.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        if (random.nextDouble() >= plugin.getConfig().getDouble("demons.natural-spawn-chance", 0.35)) {
            return;
        }
        int roll = random.nextInt(100);
        DemonType type = roll < 60 ? DemonType.LESSER
                : roll < 72 ? DemonType.SWAMP
                : roll < 82 ? DemonType.TEMARI
                : roll < 92 ? DemonType.DRUM
                : DemonType.HAND;
        apply(e.getEntity(), type);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        for (Entity entity : e.getEntities()) {
            if (entity instanceof LivingEntity living && isDemonMob(living)) {
                track(living);
            }
        }
    }

    /** Picks up demons that are already loaded (after a reload). */
    public void scanLoaded() {
        for (World w : Bukkit.getWorlds()) {
            for (LivingEntity e : w.getLivingEntities()) {
                if (isDemonMob(e)) {
                    track(e);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        // Demons don't attack demon players.
        if (e.getTarget() instanceof Player p && isDemonMob(e.getEntity()) && plugin.slayers().isDemon(p)) {
            e.setCancelled(true);
        }
    }

    // ---- ticking ----

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void shutdown() {
        for (BossBar bar : bars.values()) {
            bar.removeAll();
        }
        bars.clear();
    }

    private void tick() {
        seconds++;
        boolean sunlight = plugin.getConfig().getBoolean("demons.sunlight-burns", true);
        for (UUID id : new ArrayList<>(tracked)) {
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof LivingEntity e) || !e.isValid() || e.isDead()) {
                untrack(id);
                continue;
            }
            DemonType type = typeOf(e);
            if (type == null) {
                untrack(id);
                continue;
            }
            double max = maxHealth(e);
            if (sunlight && inSunlight(e)) {
                ASH.spawn(e.getLocation().add(0, 1, 0), 6, 0.4);
                e.setFireTicks(Math.max(e.getFireTicks(), 60));
                plugin.combat().trueDamage(e, max * (type.isBoss() ? 0.08 : 0.25));
                if (e.isDead()) {
                    continue;
                }
            } else if (!plugin.combat().isMarked(e) && e.getHealth() < max) {
                e.setHealth(Math.min(max, e.getHealth() + (type.isBoss() ? max * 0.005 : 1.5)));
            }
            Player target = pickTarget(e);
            if (target != null) {
                int cd = cooldowns.getOrDefault(id, type.cooldown()) - 1;
                if (type.isBoss() && e.getHealth() < max / 2) {
                    cd--; // enraged
                }
                if (cd <= 0) {
                    List<Ability> list = abilities.get(type);
                    if (list != null && !list.isEmpty()) {
                        list.get(random.nextInt(list.size())).use(e, target);
                    }
                    cd = type.cooldown();
                }
                cooldowns.put(id, cd);
            }
            BossBar bar = bars.get(id);
            if (bar != null) {
                updateBar(bar, e, max);
            }
        }
        if (seconds % 60 == 0) {
            bossEvents();
        }
    }

    private void updateBar(BossBar bar, LivingEntity boss, double max) {
        bar.setProgress(Math.max(0, Math.min(1, boss.getHealth() / max)));
        Set<Player> near = new HashSet<>();
        for (Player p : boss.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(boss.getLocation()) < 48 * 48) {
                near.add(p);
            }
        }
        for (Player p : new ArrayList<>(bar.getPlayers())) {
            if (!near.contains(p)) {
                bar.removePlayer(p);
            }
        }
        for (Player p : near) {
            bar.addPlayer(p);
        }
    }

    private Player pickTarget(LivingEntity e) {
        if (e instanceof Mob mob && mob.getTarget() instanceof Player p && valid(p, e, 32)) {
            return p;
        }
        Player best = null;
        double bestDist = 24 * 24;
        for (Player p : e.getWorld().getPlayers()) {
            if (!valid(p, e, 24)) {
                continue;
            }
            double d = p.getLocation().distanceSquared(e.getLocation());
            if (d < bestDist) {
                best = p;
                bestDist = d;
            }
        }
        if (best != null && e instanceof Mob mob) {
            mob.setTarget(best);
        }
        return best;
    }

    private boolean valid(Player p, LivingEntity demon, double range) {
        return p.isValid() && !p.isDead() && p.getWorld().equals(demon.getWorld())
                && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                && !plugin.slayers().isDemon(p)
                && p.getLocation().distanceSquared(demon.getLocation()) <= range * range;
    }

    // ---- boss events ----

    private void bossEvents() {
        if (!plugin.getConfig().getBoolean("boss-events.enabled", true)) {
            return;
        }
        double chance = plugin.getConfig().getDouble("boss-events.chance-per-minute", 0.04);
        for (Player p : Bukkit.getOnlinePlayers()) {
            World w = p.getWorld();
            if (w.getEnvironment() != World.Environment.NORMAL || isDay(w) || plugin.slayers().isDemon(p)
                    || p.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }
            Rank rank = plugin.slayers().rank(p);
            if (rank == Rank.MIZUNOTO || random.nextDouble() >= chance || bossNear(p.getLocation(), 150)) {
                continue;
            }
            DemonType boss = bossFor(rank);
            double angle = random.nextDouble() * Math.PI * 2;
            int x = p.getLocation().getBlockX() + (int) (Math.cos(angle) * 14);
            int z = p.getLocation().getBlockZ() + (int) (Math.sin(angle) * 14);
            Location at = new Location(w, x + 0.5, w.getHighestBlockYAt(x, z) + 1, z + 0.5);
            spawn(boss, at);
            crow(p, boss.fullName() + " has been sighted nearby! Prepare yourself!");
        }
    }

    private DemonType bossFor(Rank rank) {
        int r = rank.ordinal();
        if (r >= Rank.HASHIRA.ordinal()) {
            return random.nextBoolean() ? DemonType.MUZAN : DemonType.KOKUSHIBO;
        }
        if (r >= Rank.KINOE.ordinal()) {
            return DemonType.KOKUSHIBO;
        }
        if (r >= Rank.HINOTO.ordinal()) {
            return random.nextBoolean() ? DemonType.DOMA : DemonType.AKAZA;
        }
        if (r >= Rank.TSUCHINOTO.ordinal()) {
            return DemonType.AKAZA;
        }
        if (r >= Rank.KANOTO.ordinal()) {
            return DemonType.ENMU;
        }
        return DemonType.RUI;
    }

    private boolean bossNear(Location at, double range) {
        for (UUID id : bars.keySet()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.getWorld().equals(at.getWorld()) && e.getLocation().distanceSquared(at) < range * range) {
                return true;
            }
        }
        return false;
    }

    public static void crow(Player p, String message) {
        p.sendMessage(Component.text("[Kasugai Crow] ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Caw! Caw! " + message, NamedTextColor.WHITE)));
        p.playSound(p.getLocation(), Sound.ENTITY_PARROT_AMBIENT, 1f, 0.6f);
    }

    // ---- death and rewards ----

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        DemonType type = typeOf(dead);
        if (type == null || dead instanceof Player) {
            return;
        }
        untrack(dead.getUniqueId());
        BLOOD.spawn(dead.getLocation().add(0, 1, 0), 30, 0.5);
        e.setDroppedExp(e.getDroppedExp() + type.merit() * 5);
        Player killer = dead.getKiller();
        if (type.isBoss()) {
            addBossDrops(type, e.getDrops());
            Bukkit.broadcast(Component.text(type.fullName() + " has been slain"
                    + (killer != null ? " by " + killer.getName() : "") + "!", NamedTextColor.GOLD));
            for (Player p : dead.getWorld().getPlayers()) {
                if (p != killer && !plugin.slayers().isDemon(p) && p.getLocation().distanceSquared(dead.getLocation()) < 48 * 48) {
                    plugin.slayers().addMerit(p, type.merit() / 2);
                }
            }
        }
        if (killer != null && !plugin.slayers().isDemon(killer)) {
            plugin.slayers().addMerit(killer, type.merit());
            killer.sendActionBar(Component.text("+" + type.merit() + " merit for slaying " + type.fullName(), NamedTextColor.GOLD));
        }
    }

    private void addBossDrops(DemonType type, List<ItemStack> drops) {
        drops.add(new ItemStack(Material.DIAMOND, Math.max(1, type.merit() / 15)));
        drops.add(new ItemStack(Material.GOLDEN_APPLE, 2));
        if (type.ordinal() >= DemonType.AKAZA.ordinal()) {
            drops.add(new ItemStack(Material.NETHERITE_SCRAP, 1 + random.nextInt(2)));
            if (type == DemonType.MUZAN || random.nextDouble() < 0.3) {
                drops.add(Items.demonBlood());
            }
        }
        if (type == DemonType.MUZAN) {
            drops.add(new ItemStack(Material.NETHER_STAR));
        }
    }

    // ---- special attacks ----

    private static void lunge(LivingEntity demon, Player target, double strength) {
        Vector to = target.getLocation().toVector().subtract(demon.getLocation().toVector());
        if (to.lengthSquared() < 1.0E-4) {
            return;
        }
        demon.setVelocity(to.normalize().multiply(strength).setY(0.35));
    }

    private void minions(LivingEntity boss, int count) {
        long nearby = boss.getNearbyEntities(16, 8, 16).stream().filter(this::isDemonMob).count();
        for (int i = 0; i < count && nearby + i < 8; i++) {
            Location at = boss.getLocation().add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            if (at.getBlock().isPassable() && at.clone().add(0, 1, 0).getBlock().isPassable()) {
                SMOKE.spawn(at.clone().add(0, 1, 0), 10, 0.3);
                spawn(DemonType.LESSER, at);
            }
        }
    }

    private void registerAbilities() {
        abilities.put(DemonType.LESSER, List.of((d, t) -> lunge(d, t, 0.9)));
        abilities.put(DemonType.SWAMP, List.of((d, t) -> {
            SMOKE.spawn(d.getLocation().add(0, 1, 0), 15, 0.4);
            Moves.behind(d, t);
            Moves.sound(d, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.6f);
            Moves.later(d, 5, () -> Moves.arc(d, 3, 120, 7, BLOOD, none()));
        }));
        abilities.put(DemonType.TEMARI, List.of((d, t) -> {
            Moves.sound(d, Sound.ENTITY_SNOWBALL_THROW, 1f, 0.6f);
            Moves.beam(d, 16, 1.2, 7, BALL, knockBack(1.2));
        }));
        abilities.put(DemonType.DRUM, List.of((d, t) -> {
            Moves.sound(d, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 2f, 0.5f);
            Moves.aoe(d, d.getLocation(), 7, 5, NOTE, (c, target) ->
                    target.setVelocity(new Vector(random.nextDouble() - 0.5, 0.6, random.nextDouble() - 0.5)));
        }));
        abilities.put(DemonType.HAND, List.of((d, t) -> {
            Moves.sound(d, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1f, 0.5f);
            Moves.beam(d, 8, 1.3, 6, FLESH, pull(1.2).and(effect(PotionEffectType.SLOW, 40, 1)));
        }));

        abilities.put(DemonType.RUI, List.of(
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 0.6f);
                    Moves.beam(d, 18, 1.2, 10, THREAD, effect(PotionEffectType.SLOW, 60, 2));
                },
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_SPIDER_HURT, 1.5f, 0.5f);
                    Moves.repeat(d, 3, 6, () -> Moves.aoe(d, d.getLocation(), 8, 8, THREAD, none()));
                },
                (d, t) -> minions(d, 2)));

        abilities.put(DemonType.ENMU, List.of(
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.7f);
                    Moves.aoe(d, d.getLocation(), 14, 2, DREAM, effect(PotionEffectType.BLINDNESS, 60, 0)
                            .and(effect(PotionEffectType.SLOW, 80, 2))
                            .and(effect(PotionEffectType.CONFUSION, 120, 0)));
                    t.sendActionBar(Component.text("Enmu: \"Sleep... and have a happy dream.\"", NamedTextColor.LIGHT_PURPLE));
                },
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_SLIME_SQUISH, 2f, 0.5f);
                    Moves.aoe(d, d.getLocation(), 6, 12, FLESH, pull(0.8));
                },
                (d, t) -> {
                    SMOKE.spawn(d.getLocation().add(0, 1, 0), 15, 0.4);
                    Moves.behind(d, t);
                }));

        abilities.put(DemonType.AKAZA, List.of(
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.7f);
                    Moves.dash(d, 1.5, 8, 16, CRIT, knockBack(1.4));
                },
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.4f);
                    Moves.beam(d, 20, 1.8, 14, CLOUD, knockBack(1.0));
                },
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 1.2f);
                    Moves.repeat(d, 5, 3, () -> Moves.aoe(d, d.getLocation(), 5, 6, CRIT, none()));
                }));

        abilities.put(DemonType.DOMA, List.of(
                (d, t) -> {
                    Moves.sound(d, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
                    Moves.aoe(d, d.getLocation(), 9, 10, ICE, freeze(220).and(effect(PotionEffectType.SLOW, 60, 1)));
                },
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.8f);
                    Moves.beam(d, 18, 1.5, 12, ICE, freeze(220));
                },
                (d, t) -> {
                    Moves.sound(d, Sound.BLOCK_POWDER_SNOW_BREAK, 1.5f, 0.6f);
                    Moves.arc(d, 7, 130, 14, ICE, freeze(160));
                }));

        abilities.put(DemonType.KOKUSHIBO, List.of(
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2f, 0.5f);
                    Moves.arc(d, 8, 150, 18, MOON, none());
                },
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.2f);
                    Moves.beam(d, 24, 2.0, 20, MOON, none());
                },
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2f, 0.7f);
                    Moves.repeat(d, 3, 5, () -> Moves.aoe(d, d.getLocation(), 10, 10, MOON, none()));
                },
                (d, t) -> {
                    Moves.behind(d, t);
                    Moves.later(d, 4, () -> Moves.arc(d, 4, 120, 18, MOON, none()));
                }));

        abilities.put(DemonType.MUZAN, List.of(
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
                    Moves.aoe(d, d.getLocation(), 9, 20, BLOOD, effect(PotionEffectType.WITHER, 100, 1));
                },
                (d, t) -> {
                    Moves.sound(d, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.6f);
                    Moves.beam(d, 20, 1.8, 18, BLOOD, effect(PotionEffectType.WITHER, 100, 1));
                },
                (d, t) -> minions(d, 3),
                (d, t) -> {
                    SMOKE.spawn(d.getLocation().add(0, 1, 0), 20, 0.5);
                    Moves.behind(d, t);
                    Moves.later(d, 4, () -> Moves.arc(d, 4, 140, 22, BLOOD, none()));
                },
                (d, t) -> {
                    double max = maxHealth(d);
                    d.setHealth(Math.min(max, d.getHealth() + max * 0.05));
                    BLOOD.spawn(d.getLocation().add(0, 1, 0), 30, 0.6);
                }));
    }
}
