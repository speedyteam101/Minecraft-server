package com.speedyteam101.demonslayer;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.speedyteam101.demonslayer.Moves.aoe;
import static com.speedyteam101.demonslayer.Moves.arc;
import static com.speedyteam101.demonslayer.Moves.beam;
import static com.speedyteam101.demonslayer.Moves.buff;
import static com.speedyteam101.demonslayer.Moves.dash;
import static com.speedyteam101.demonslayer.Moves.later;
import static com.speedyteam101.demonslayer.Moves.leap;
import static com.speedyteam101.demonslayer.Moves.repeat;
import static com.speedyteam101.demonslayer.OnHit.effect;
import static com.speedyteam101.demonslayer.OnHit.fire;
import static com.speedyteam101.demonslayer.OnHit.freeze;
import static com.speedyteam101.demonslayer.OnHit.knockBack;
import static com.speedyteam101.demonslayer.OnHit.knockUp;
import static com.speedyteam101.demonslayer.OnHit.lightning;
import static com.speedyteam101.demonslayer.OnHit.none;

/** Every form of every breathing style and Blood Demon Art. */
public final class Forms {
    private static final Fx WATER = Fx.of(Particle.WATER_SPLASH);
    private static final Fx WAVE = Fx.of(Particle.DOLPHIN);
    private static final Fx FLAME = Fx.of(Particle.FLAME);
    private static final Fx SPARK = Fx.of(Particle.ELECTRIC_SPARK);
    private static final Fx CLOUD = Fx.of(Particle.CLOUD);
    private static final Fx SWEEP = Fx.of(Particle.SWEEP_ATTACK);
    private static final Fx ROCK = Fx.block(Material.STONE);
    private static final Fx MIST = Fx.dust(Color.fromRGB(235, 240, 255), 2.0f);
    private static final Fx BEAST = Fx.dust(Color.fromRGB(60, 70, 160), 1.6f);
    private static final Fx BUTTERFLY = Fx.dust(Color.fromRGB(190, 120, 240), 1.3f);
    private static final Fx SERPENT = Fx.dust(Color.fromRGB(120, 60, 160), 1.5f);
    private static final Fx LOVE = Fx.dust(Color.fromRGB(255, 130, 190), 1.6f);
    private static final Fx BOOM = Fx.of(Particle.EXPLOSION_NORMAL);
    private static final Fx FLOWER = Fx.of(Particle.CHERRY_LEAVES);
    private static final Fx SUN = Fx.dust(Color.fromRGB(255, 170, 30), 1.8f);
    private static final Fx MOON = Fx.dust(Color.fromRGB(150, 40, 180), 1.8f);
    private static final Fx CRIT = Fx.of(Particle.CRIT);
    private static final Fx ICE = Fx.of(Particle.SNOWFLAKE);
    private static final Fx THREAD = Fx.dust(Color.fromRGB(160, 0, 0), 1.0f);
    private static final Fx BLOOD = Fx.dust(Color.fromRGB(200, 20, 20), 1.6f);
    private static final Fx BIG_BOOM = Fx.of(Particle.EXPLOSION_LARGE);

    private final Map<Style, List<Form>> forms = new EnumMap<>(Style.class);

    public Forms() {
        forms.put(Style.WATER, List.of(
                form("First Form: Water Surface Slash", 15, p -> arc(p, 4.5, 110, 9, WATER, none())),
                form("Second Form: Water Wheel", 20, p -> {
                    leap(p, 0.6, 0.8);
                    later(p, 6, () -> aoe(p, p.getLocation(), 3.5, 10, WATER, knockUp(0.5)));
                }),
                form("Fourth Form: Striking Tide", 25, p -> repeat(p, 4, 4, () -> arc(p, 4, 90, 5, WATER, none()))),
                form("Tenth Form: Constant Flux", 40, p -> dash(p, 1.1, 12, 14, WAVE, knockBack(1.0))),
                form("Eleventh Form: Dead Calm", 45, p -> {
                    buff(p, PotionEffectType.DAMAGE_RESISTANCE, 80, 2);
                    repeat(p, 8, 5, () -> aoe(p, p.getLocation(), 3, 5, WATER, none()));
                })));

        forms.put(Style.FLAME, List.of(
                form("First Form: Unknowing Fire", 20, p -> dash(p, 1.3, 7, 11, FLAME, fire(80))),
                form("Second Form: Rising Scorching Sun", 20, p -> arc(p, 4, 100, 10, FLAME, fire(80).and(knockUp(0.8)))),
                form("Third Form: Blazing Universe", 30, p -> {
                    leap(p, 0.7, 0.3);
                    later(p, 8, () -> aoe(p, p.getLocation(), 4.5, 16, FLAME, fire(100)));
                }),
                form("Fifth Form: Flame Tiger", 35, p -> beam(p, 14, 1.8, 15, FLAME, fire(100))),
                form("Ninth Form: Rengoku", 60, p -> dash(p, 1.6, 14, 24, FLAME, fire(160).and(knockBack(1.4))))));

        forms.put(Style.THUNDER, List.of(
                form("First Form: Thunderclap and Flash", 25, p -> dash(p, 2.2, 6, 18, SPARK, lightning())),
                form("First Form: Thunderclap and Flash, Sixfold", 40, p -> {
                    dash(p, 2.0, 5, 6, SPARK, none());
                    repeat(p, 6, 3, () -> aoe(p, p.getLocation(), 3.5, 5, SPARK, none()));
                }),
                form("Fourth Form: Distant Thunder", 25, p -> beam(p, 18, 1.5, 12, SPARK, none())),
                form("Fifth Form: Heat Lightning", 35, p -> aoe(p, p.getLocation(), 6, 12, SPARK, lightning())),
                form("Seventh Form: Flaming Thunder God", 70, p -> dash(p, 3.0, 8, 30, FLAME, lightning().and(fire(60))))));

        forms.put(Style.WIND, List.of(
                form("First Form: Dust Whirlwind Cutter", 20, p -> dash(p, 1.4, 8, 10, CLOUD, none())),
                form("Second Form: Claws-Purifying Wind", 20, p -> repeat(p, 4, 2, () -> arc(p, 5, 70, 4.5, SWEEP, none()))),
                form("Fifth Form: Cold Mountain Wind", 30, p -> aoe(p, p.getLocation(), 5.5, 12, CLOUD, knockBack(1.2))),
                form("Sixth Form: Black Wind Mountain Mist", 30, p -> beam(p, 16, 2.0, 12, CLOUD, none())),
                form("Ninth Form: Idaten Typhoon", 50, p -> {
                    leap(p, 1.0, 0.2);
                    later(p, 10, () -> aoe(p, p.getLocation(), 6, 18, CLOUD, knockUp(0.9)));
                })));

        forms.put(Style.STONE, List.of(
                form("First Form: Serpentinite Bipolar", 20, p -> arc(p, 5, 90, 12, ROCK, knockBack(0.8))),
                form("Second Form: Upper Smash", 30, p -> aoe(p, p.getLocation(), 5, 14, ROCK, knockUp(1.0))),
                form("Third Form: Stone Skin", 30, p -> {
                    buff(p, PotionEffectType.DAMAGE_RESISTANCE, 200, 2);
                    buff(p, PotionEffectType.ABSORPTION, 200, 2);
                }),
                form("Fourth Form: Volcanic Rock, Rapid Conquest", 45, p -> repeat(p, 5, 4, () -> aoe(p, p.getLocation(), 4, 7, ROCK, none()))),
                form("Fifth Form: Arcs of Justice", 55, p -> beam(p, 12, 2.5, 22, ROCK, knockBack(1.5)))));

        forms.put(Style.MIST, List.of(
                form("First Form: Low Clouds, Distant Haze", 20, p -> dash(p, 1.6, 6, 11, MIST, none())),
                form("Second Form: Eight-Layered Mist", 35, p -> repeat(p, 8, 2, () -> arc(p, 4, 90, 3.5, MIST, none()))),
                form("Third Form: Scattering Mist Splash", 25, p -> aoe(p, p.getLocation(), 5, 10, MIST, effect(PotionEffectType.BLINDNESS, 40, 0))),
                form("Seventh Form: Obscuring Clouds", 45, p -> {
                    buff(p, PotionEffectType.INVISIBILITY, 100, 0);
                    buff(p, PotionEffectType.SPEED, 100, 1);
                    dash(p, 1.4, 5, 12, MIST, none());
                })));

        forms.put(Style.BEAST, List.of(
                form("First Fang: Pierce", 20, p -> dash(p, 1.5, 6, 11, BEAST, none())),
                form("Fifth Fang: Crazy Cutting", 30, p -> repeat(p, 5, 3, () -> aoe(p, p.getLocation(), 3.5, 5, SWEEP, none()))),
                form("Spatial Awareness", 15, p -> {
                    for (LivingEntity t : Moves.targets(p, p.getLocation(), 24)) {
                        t.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.GLOWING, 200, 0));
                    }
                }),
                form("Sixth Fang: Palisade Bite", 30, p -> arc(p, 4, 60, 16, BEAST, none())),
                form("Eighth Fang: Explosive Rush", 40, p -> dash(p, 1.8, 8, 16, BEAST, knockBack(1.3)))));

        // Wisteria poison: demons are undead-like mobs, so Wither stands in for poison.
        OnHit wisteria = effect(PotionEffectType.WITHER, 120, 1).and(effect(PotionEffectType.SLOW, 60, 0));
        forms.put(Style.INSECT, List.of(
                form("Butterfly Dance: Caprice", 20, p -> dash(p, 1.3, 6, 7, BUTTERFLY, wisteria)),
                form("Bee Sting: True Flutter", 20, p -> beam(p, 8, 1.2, 9, BUTTERFLY, wisteria)),
                form("Dragonfly Dance: Compound Eye Hexagon", 35, p -> repeat(p, 6, 2, () -> beam(p, 5, 1.2, 3.5, BUTTERFLY, wisteria))),
                form("Centipede Dance: Hundred-Foot Zigzag", 35, p -> dash(p, 1.7, 10, 9, BUTTERFLY, wisteria))));

        forms.put(Style.SERPENT, List.of(
                form("First Form: Winding Serpent Slash", 20, p -> arc(p, 5, 130, 9, SERPENT, none())),
                form("Second Form: Venom Fangs of the Narrow Head", 25, p -> beam(p, 9, 1.2, 13, SERPENT, none())),
                form("Third Form: Coil Choke", 30, p -> aoe(p, p.getLocation(), 4, 10, SERPENT, effect(PotionEffectType.SLOW, 60, 2))),
                form("Fifth Form: Slithering Serpent", 40, p -> dash(p, 1.4, 12, 12, SERPENT, none()))));

        forms.put(Style.LOVE, List.of(
                form("First Form: Shivers of First Love", 20, p -> dash(p, 1.6, 7, 10, LOVE, none())),
                form("Second Form: Love Pangs", 25, p -> arc(p, 5, 160, 10, LOVE, none())),
                form("Fifth Form: Swaying Love, Wildclaw", 35, p -> repeat(p, 4, 3, () -> aoe(p, p.getLocation(), 4.5, 5, LOVE, none()))),
                form("Sixth Form: Cat-Legged Love", 40, p -> beam(p, 12, 2, 14, LOVE, none()))));

        forms.put(Style.SOUND, List.of(
                form("First Form: Roar", 25, p -> aoe(p, p.getLocation(), 5, 11, BOOM, knockBack(1.1))),
                form("Fourth Form: Constant Resounding Slashes", 35, p -> repeat(p, 5, 3, () -> arc(p, 4.5, 120, 5, BOOM, none()))),
                form("Fifth Form: String Performance", 45, p -> beam(p, 14, 2, 16, BIG_BOOM, knockBack(0.8))),
                form("Musical Score", 20, p -> {
                    buff(p, PotionEffectType.SPEED, 200, 1);
                    buff(p, PotionEffectType.INCREASE_DAMAGE, 200, 0);
                })));

        forms.put(Style.FLOWER, List.of(
                form("Second Form: Honorable Shadow Plum", 20, p -> aoe(p, p.getLocation(), 4, 9, FLOWER, none())),
                form("Fourth Form: Crimson Hanagoromo", 25, p -> arc(p, 5, 140, 11, FLOWER, none())),
                form("Fifth Form: Peonies of Futility", 30, p -> repeat(p, 9, 2, () -> arc(p, 4, 80, 2.5, FLOWER, none()))),
                form("Sixth Form: Whirling Peach", 30, p -> {
                    leap(p, 0.6, 0.6);
                    later(p, 6, () -> aoe(p, p.getLocation(), 4, 12, FLOWER, none()));
                }),
                form("Final Form: Equinoctial Vermilion Eye", 50, p -> {
                    buff(p, PotionEffectType.SPEED, 200, 2);
                    buff(p, PotionEffectType.NIGHT_VISION, 300, 0);
                    buff(p, PotionEffectType.INCREASE_DAMAGE, 200, 1);
                })));

        forms.put(Style.SUN, List.of(
                form("Dance", 20, p -> arc(p, 5, 140, 14, SUN, fire(100))),
                form("Clear Blue Sky", 30, p -> aoe(p, p.getLocation(), 5, 16, SUN, fire(100))),
                form("Fire Wheel", 30, p -> {
                    leap(p, 0.8, 0.6);
                    later(p, 7, () -> aoe(p, p.getLocation(), 5, 18, FLAME, fire(120)));
                }),
                form("Sunflower Thrust", 30, p -> beam(p, 16, 1.5, 20, SUN, fire(120))),
                form("Setting Sun Transformation", 40, p -> dash(p, 1.6, 8, 18, SUN, fire(100))),
                form("Thirteenth Form", 80, p -> {
                    buff(p, PotionEffectType.DAMAGE_RESISTANCE, 80, 1);
                    repeat(p, 12, 3, () -> arc(p, 5, 180, 8, SUN, fire(100)));
                })));

        forms.put(Style.MOON, List.of(
                form("First Form: Dark Moon, Evening Palace", 20, p -> arc(p, 6, 150, 14, MOON, none())),
                form("Third Form: Loathsome Moon, Chains", 30, p -> repeat(p, 3, 4, () -> arc(p, 7, 120, 8, MOON, none()))),
                form("Fifth Form: Moon Spirit Calamitous Eddy", 35, p -> aoe(p, p.getLocation(), 6, 15, MOON, none())),
                form("Tenth Form: Drilling Slashes, Moon Through Bamboo Leaves", 45, p -> beam(p, 20, 2, 20, MOON, none()))));

        forms.put(Style.DESTRUCTIVE_DEATH, List.of(
                form("Destructive Death: Air Type", 20, p -> beam(p, 16, 1.6, 13, CLOUD, knockBack(1.0))),
                form("Technique Development: Compass Needle", 20, p -> {
                    buff(p, PotionEffectType.SPEED, 300, 1);
                    buff(p, PotionEffectType.INCREASE_DAMAGE, 300, 1);
                    for (LivingEntity t : Moves.targets(p, p.getLocation(), 20)) {
                        t.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.GLOWING, 300, 0));
                    }
                }),
                form("Destructive Death: Disorder", 35, p -> repeat(p, 5, 3, () -> aoe(p, p.getLocation(), 4, 6, CRIT, none()))),
                form("Destructive Death: Annihilation Type", 50, p -> dash(p, 1.7, 8, 22, CRIT, knockBack(1.5)))));

        forms.put(Style.CRYOKINESIS, List.of(
                form("Frozen Lotus", 25, p -> aoe(p, p.getLocation(), 6, 10, ICE, freeze(200).and(effect(PotionEffectType.SLOW, 60, 1)))),
                form("Barren Hanging Garden", 30, p -> arc(p, 6, 130, 13, ICE, freeze(160))),
                form("Rime", 25, p -> beam(p, 16, 1.5, 11, ICE, freeze(200))),
                form("Winter Ice, Scattered Lotus", 45, p -> repeat(p, 4, 5, () -> aoe(p, p.getLocation(), 7, 6, ICE, freeze(160))))));

        forms.put(Style.BLOOD_THREADS, List.of(
                form("Cutting Thread Cage", 30, p -> repeat(p, 3, 5, () -> aoe(p, p.getLocation(), 6, 7, THREAD, none()))),
                form("Thread Web", 25, p -> arc(p, 7, 120, 9, THREAD, effect(PotionEffectType.SLOW, 80, 2))),
                form("Crimson Blood Thread", 25, p -> beam(p, 18, 1.2, 13, THREAD, effect(PotionEffectType.SLOW, 40, 1)))));

        forms.put(Style.EXPLODING_BLOOD, List.of(
                form("Exploding Blood", 30, p -> {
                    BIG_BOOM.spawn(p.getLocation().add(0, 1, 0), 3, 1.0);
                    aoe(p, p.getLocation(), 5, 12, FLAME, fire(120));
                }),
                form("Blood Kick", 20, p -> dash(p, 1.4, 6, 12, BLOOD, fire(80).and(knockUp(0.6)))),
                form("Regeneration", 30, p -> buff(p, PotionEffectType.REGENERATION, 100, 2)),
                form("Burning Blood Rush", 40, p -> beam(p, 12, 2, 14, FLAME, fire(120)))));
    }

    private static Form form(String name, int cost, java.util.function.Consumer<Player> action) {
        return new Form(name, cost, action);
    }

    public List<Form> of(Style style) {
        return forms.getOrDefault(style, List.of());
    }

    public int count() {
        return forms.values().stream().mapToInt(List::size).sum();
    }
}
