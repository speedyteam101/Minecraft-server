#!/usr/bin/env bash
# CI end-to-end test: starts the real Paper 1.20.1 server with the plugin (via server/start.sh),
# summons every demon type from the console, runs the test bot, then checks the log for errors.
set -euo pipefail
cd "$(dirname "$0")/../../server"

# Test-only settings: offline mode so the bot can join, and a flat world for a fast, predictable start.
cat >> server.properties <<'EOF'
online-mode=false
level-type=minecraft\:flat
spawn-protection=0
EOF

rm -f console
mkfifo console
EULA=true ./start.sh < console > server.log 2>&1 &
SERVER_PID=$!
exec 3>console

wait_for() {
    local pattern="$1" seconds="$2"
    for _ in $(seq "$seconds"); do
        if grep -q "$pattern" server.log; then
            return 0
        fi
        if ! kill -0 "$SERVER_PID" 2>/dev/null; then
            echo "Server exited early."
            cat server.log
            return 1
        fi
        sleep 1
    done
    echo "Timed out waiting for: $pattern"
    tail -n 80 server.log
    return 1
}

wait_for 'Done (' 600
grep 'Demon Slayer ready' server.log || { echo "Plugin did not enable"; cat server.log; exit 1; }

echo "--- Summoning every demon type from the console (at night, so sunlight doesn't kill them) ---"
echo "gamerule doDaylightCycle false" >&3
echo "time set midnight" >&3
sleep 1
for t in lesser swamp temari drum hand rui enmu akaza doma kokushibo muzan; do
    echo "slayer summon $t" >&3
done
sleep 10
if grep -q 'has been slain' server.log; then
    echo "A boss died before /kill: something else is killing demons."
    grep 'has been slain' server.log
    exit 1
fi
echo "kill @e[type=!player]" >&3
sleep 3
for boss in 'Lower Moon Five Rui' 'Lower Moon One Enmu' 'Upper Moon Three Akaza' 'Upper Moon Two Doma' 'Upper Moon One Kokushibo' 'Demon King Muzan Kibutsuji'; do
    grep -q "$boss has been slain" server.log || { echo "/kill did not kill $boss"; tail -n 40 server.log; exit 1; }
done
echo "All bosses were summoned and could be removed with /kill."

echo "--- Running the test bot ---"
(cd ../test-bot && node bot.js) &
BOT_PID=$!
wait_for 'TestSlayer joined the game' 90
echo "op TestSlayer" >&3
BOT_STATUS=0
wait "$BOT_PID" || BOT_STATUS=$?

echo "stop" >&3
exec 3>&-
wait "$SERVER_PID" || true

echo "--- Checking the server log ---"
ERRORS=0
if grep -nE 'Could not pass event|Error occurred while enabling|generated an exception|Exception in thread|at com\.speedyteam101' server.log; then
    echo "Plugin errors found in the server log."
    ERRORS=1
fi
if [ "$BOT_STATUS" -ne 0 ]; then
    echo "Test bot failed (exit $BOT_STATUS)."
fi
if [ "$BOT_STATUS" -ne 0 ] || [ "$ERRORS" -ne 0 ]; then
    exit 1
fi
echo "Server test passed."
