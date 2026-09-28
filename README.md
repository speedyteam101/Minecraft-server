# Demon Slayer Minecraft Server

An unofficial, fan-made **Demon Slayer (Kimetsu no Yaiba)** server for **Minecraft Java Edition 1.20.1**, running on [Paper](https://papermc.io).
Everything comes from one server plugin (`plugin/`), so **players join with a normal, unmodded Minecraft 1.20.1 client**.
All effects use vanilla mobs, items, particles and sounds. There's no resource pack.

Not affiliated with Koyoharu Gotouge, Shueisha, ufotable, Mojang or Microsoft.

## What's on the server

### Demon Slayers
- **Nichirin Blade**: every new player gets one, together with a **Demon Slayer Uniform** (black, Protection III, unbreakable). The blade takes on the colour of your breathing style.
- **13 Breathing Styles** with 4–6 forms each: Water, Flame, Thunder, Wind, Stone, Mist, Beast, Insect, Serpent, Love, Sound, Flower, and **Sun Breathing** (Hashira only).
  - Choose one with `/slayer menu`.
  - **Right-click** with the blade to use the current form. **Sneak + right-click** switches form. The swap-hands key (**F**) works too.
- **Breath**: forms cost breath, shown in the action bar. **Sneak** to use *Total Concentration* and get it back faster.
- **Ranks**: slaying demons earns merit and promotes you from **Mizunoto** up through the ten ranks to **Hashira**. Each rank adds 10% technique damage and 10 max breath.

### Demons
- At night, **35% of naturally spawning zombies and husks become demons**: Demon, Swamp Demon, Temari Demon, Drum Demon or Hand Demon. Each has its own special attack.
- **Demons regenerate** and **can't die without a Nichirin blade or sunlight**. Other weapons only do 35% damage and can't finish them.
  - A Nichirin hit stops them regenerating for 5 seconds.
  - Blades and breathing techniques do 1.5× damage to demons.
- **Sunlight burns demons to ash.**

### The Twelve Kizuki (bosses)
Each boss has a boss bar, special attacks, and a stronger phase below 50% health.

| Boss | Mob | HP | Attacks |
| --- | --- | --- | --- |
| Lower Moon Five **Rui** | Skeleton | 300 | Blood thread beam, thread cage, summons his "family" |
| Lower Moon One **Enmu** | Illusioner | 350 | Sleep (blindness, slowness, nausea), flesh tentacles, teleport |
| Upper Moon Three **Akaza** | Zombie | 600 | Dash strike, Air Type shockwave, Disorder combo |
| Upper Moon Two **Doma** | Stray | 650 | Frozen lotus, ice beam, ice fan (freezing) |
| Upper Moon One **Kokushibo** | Wither Skeleton | 800 | Moon Breathing crescents, long moon beam, teleport slash |
| Demon King **Muzan Kibutsuji** | Vindicator | 1000 | Whip arms (Wither), blood beam, summons demons, teleport, heals |

- At night your **Kasugai Crow** may warn you that a boss is coming. There's a 4% chance per minute, for Mizunoe rank and above. The boss you get depends on your rank.
- Bosses drop diamonds, golden apples and netherite scrap. Upper Moons have a 30% chance to drop **Muzan's Blood**, and Muzan always drops it.

### Becoming a demon
- Drinking **Muzan's Blood** turns you into a demon.
  - Demons get night vision, don't need food, and get Strength, Speed and Regeneration II at night.
  - **Direct sunlight burns you.**
- Demons use **Blood Demon Arts** instead of breathing (**F** to use, **sneak + F** to switch). The five arts are Moon Breathing (Kokushibo), Destructive Death (Akaza), Cryokinesis (Doma), Blood Threads (Rui) and Exploding Blood (Nezuko).
- **Tamayo's Cure** turns you back into a human.

### Recipes (shown in the recipe book)
| Item | Recipe |
| --- | --- |
| Nichirin Blade | Iron Block on top, Glowstone Dust – Iron Block – Glowstone Dust in the middle, Stick at the bottom (all centre column) |
| Demon Slayer Uniform | 7 Black Wool in the chestplate shape |
| Tamayo's Cure | Golden Apple + Allium + Lilac + Glistering Melon Slice (shapeless) |

Technique names follow the anime and manga as closely as I could, but some are approximations.

## Commands

| Command | Who | What |
| --- | --- | --- |
| `/slayer menu` | everyone | Choose a breathing style or Blood Demon Art |
| `/slayer style <name>` | everyone | Choose a style by name |
| `/slayer info [player]` | everyone | Rank, merit and style |
| `/slayer give <nichirin_blade\|slayer_uniform\|demon_blood\|tamayo_cure> [player]` | op | Give an item |
| `/slayer summon <demon>` | op | Summon a demon or boss (`lesser`, `swamp`, `temari`, `drum`, `hand`, `rui`, `enmu`, `akaza`, `doma`, `kokushibo`, `muzan`) |
| `/slayer demon <player>` / `/slayer human <player>` | op | Turn a player into a demon or back |
| `/slayer merit <player> <amount>` | op | Add (or remove) merit |

Aliases: `/ds`, `/demonslayer`. Settings (spawn chance, sunlight, boss events, starter kit) are in `plugins/DemonSlayer/config.yml`.

## Running the server

You need **Java 17 or newer** and **Maven** (to build the plugin).
You can also skip building: download the ready-made `DemonSlayer-plugin` jar from the latest run on this repo's **Actions** tab.

### 1. Build the plugin
```bash
cd plugin
mvn package        # creates plugin/target/DemonSlayer-1.0.0.jar
```

### 2a. Start it (macOS / Linux)
```bash
cd server
./start.sh         # downloads Paper 1.20.1 the first time, copies in the plugin, asks you to accept the EULA
```
Use `MEMORY=4G ./start.sh` to give it more RAM (the default is 2G).

### 2b. Start it (Windows)
Download Paper 1.20.1 from <https://papermc.io/downloads/all>, save it as `server/paper.jar`, then double-click `server/start.bat`.

### 2c. Or with Docker
```bash
mkdir -p server/plugins && cp plugin/target/DemonSlayer-*.jar server/plugins/
cd server
EULA=TRUE docker compose up -d
```
This uses the community [`itzg/minecraft-server`](https://github.com/itzg/docker-minecraft-server) image. Setting `EULA=TRUE` means you accept the [Minecraft EULA](https://aka.ms/MinecraftEULA).

### 3. Join
In Minecraft Java Edition **1.20.1**, go to Multiplayer → Add Server and enter `localhost`, or your computer's IP address for friends on the same network.
For friends over the internet you need to forward port 25565 on your router, or use a hosting service.

## Testing

Every push runs `.github/workflows/build.yml`:
1. **build**: compiles the plugin against the Paper 1.20.1 API.
2. **server-test**: starts a real Paper 1.20.1 server with `server/start.sh` and summons every demon type. A [mineflayer](https://github.com/PrismarineJS/mineflayer) bot (`test-bot/`) then joins and does the following:
   - uses every form of every breathing style and Blood Demon Art against summoned demons;
   - checks that kills give merit and that demons burn in sunlight;
   - fights each boss.

   The job fails if the server log shows any plugin error.
3. **docker-test**: starts the Docker Compose setup and checks that the plugin loads.

The bot checks that everything runs without errors. It doesn't judge balance or how the effects look, so play-testing is still worthwhile.
