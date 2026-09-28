// End-to-end test: joins the local test server (offline mode) as "TestSlayer",
// uses every form of every style against summoned demons, fights each boss,
// and checks that kills give merit. The CI script ops the bot and checks the server log for errors.
const mineflayer = require('mineflayer');

const FORMS = {
  water: 5, flame: 5, thunder: 5, wind: 5, stone: 5, mist: 4, beast: 5, insect: 4,
  serpent: 4, love: 4, sound: 4, flower: 5, sun: 6,
};
const DEMON_ARTS = { moon: 4, destructive_death: 4, cryokinesis: 4, blood_threads: 3, exploding_blood: 4 };
const BOSSES = ['rui', 'enmu', 'akaza', 'doma', 'kokushibo', 'muzan'];

const bot = mineflayer.createBot({
  host: process.env.MC_HOST || '127.0.0.1',
  port: Number(process.env.MC_PORT || 25565),
  username: 'TestSlayer',
  auth: 'offline',
  version: '1.20.1',
});

const chat = [];
let outOfBreath = 0;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

bot.on('messagestr', (msg, position) => {
  if (position === 'game_info') {
    if (/Out of breath|Not enough blood/.test(msg)) outOfBreath++;
    return;
  }
  chat.push(msg);
  console.log('[chat]', msg);
});
bot.on('kicked', (reason) => fail('kicked: ' + JSON.stringify(reason)));
bot.on('error', (err) => fail(err));
bot.on('death', () => console.log('[bot] died (respawning)'));

function fail(why) {
  console.error('TEST FAILED:', why);
  process.exit(1);
}

async function waitForChat(pattern, ms) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (chat.some((m) => pattern.test(m))) return true;
    await sleep(250);
  }
  return false;
}

async function cmd(command, wait = 600) {
  bot.chat('/' + command);
  await sleep(wait);
}

async function lookAtNearestDemon() {
  const demon = bot.nearestEntity((e) => e.type === 'mob' || e.type === 'hostile');
  if (demon) await bot.lookAt(demon.position.offset(0, 1.2, 0), true);
  return demon;
}

async function useForm(swapHands) {
  await lookAtNearestDemon();
  if (swapHands) {
    // Swap-hands key (F) is how demons use their Blood Demon Art.
    bot._client.write('block_dig', { status: 6, location: { x: 0, y: 0, z: 0 }, face: 0, sequence: 0 });
  } else {
    bot.activateItem();
    bot.deactivateItem();
  }
}

async function nextForm(swapHands) {
  bot.setControlState('sneak', true);
  await sleep(250);
  await useForm(swapHands);
  // Stay sneaking a moment: Total Concentration regenerates breath faster.
  await sleep(1500);
  bot.setControlState('sneak', false);
  await sleep(200);
}

async function runStyle(style, forms, swapHands) {
  console.log(`[bot] style ${style} (${forms} forms)`);
  await cmd(`slayer style ${style}`);
  for (let i = 0; i < forms; i++) {
    await cmd('slayer summon lesser', 700);
    await useForm(swapHands);
    await sleep(1200);
    await nextForm(swapHands);
  }
}

async function merit() {
  chat.length = 0;
  await cmd('slayer info', 1000);
  const line = chat.find((m) => m.startsWith('Merit: '));
  return line ? parseInt(line.slice('Merit: '.length), 10) : NaN;
}

bot.once('spawn', async () => {
  try {
    console.log('[bot] spawned, waiting to be opped');
    let opped = false;
    for (let i = 0; i < 60 && !opped; i++) {
      chat.length = 0;
      bot.chat('/slayer merit TestSlayer 0');
      opped = await waitForChat(/now has \d+ merit/, 1000);
    }
    if (!opped) fail('bot was never opped');

    await cmd('gamerule doDaylightCycle false');
    await cmd('time set midnight');
    await cmd('effect give TestSlayer minecraft:resistance 100000 4 true');
    await cmd('effect give TestSlayer minecraft:saturation 100000 1 true');

    const sword = bot.inventory.items().find((i) => i.name === 'iron_sword');
    if (!sword) fail('starter Nichirin blade missing from inventory');
    await bot.equip(sword, 'hand');

    await cmd('slayer merit TestSlayer 400', 1000);
    const start = await merit();
    console.log('[bot] merit at start:', start);
    if (start !== 400) fail('expected 400 merit, got ' + start);

    for (const [style, forms] of Object.entries(FORMS)) await runStyle(style, forms, false);
    const afterHuman = await merit();
    console.log('[bot] merit after breathing styles:', afterHuman);
    if (!(afterHuman > start)) fail('no merit gained: techniques did not kill any demons');

    await cmd('slayer demon TestSlayer', 1000);
    for (const [style, forms] of Object.entries(DEMON_ARTS)) await runStyle(style, forms, true);
    await cmd('slayer human TestSlayer', 1000);

    // Sunlight: a demon standing in the open at noon should burn away.
    await cmd('kill @e[type=!player]', 1000);
    await cmd('time set noon');
    await cmd('slayer summon lesser', 7000);
    const survivors = Object.values(bot.entities).filter((e) => e.name === 'zombie').length;
    console.log('[bot] zombies left after 7s of sunlight:', survivors);
    if (survivors > 0) fail('demon did not burn in sunlight');
    await cmd('time set midnight');

    // Bosses: let each use its special attacks on the bot for a while.
    for (const boss of BOSSES) {
      console.log('[bot] fighting', boss);
      await cmd(`slayer summon ${boss}`, 9000);
      await cmd('kill @e[type=!player]', 1000);
    }

    console.log(`[bot] casts refused for lack of breath: ${outOfBreath}`);
    console.log('TEST PASSED');
    bot.quit();
    setTimeout(() => process.exit(0), 500);
  } catch (err) {
    fail(err && err.stack ? err.stack : err);
  }
});

setTimeout(() => fail('timed out'), 15 * 60 * 1000);
