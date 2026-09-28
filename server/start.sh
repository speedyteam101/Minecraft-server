#!/usr/bin/env bash
# Starts the Demon Slayer Paper server (Minecraft Java 1.20.1).
# Downloads Paper on first run, copies in the plugin, and asks you to accept the Minecraft EULA.
set -euo pipefail
cd "$(dirname "$0")"

MC_VERSION="1.20.1"
MEMORY="${MEMORY:-2G}"

download_paper() {
    echo "Downloading Paper ${MC_VERSION}..."
    local url=""
    # PaperMC "Fill" v3 API.
    local json
    if json="$(curl -fsSL -H 'User-Agent: demon-slayer-mc-server' "https://fill.papermc.io/v3/projects/paper/versions/${MC_VERSION}/builds" 2>/dev/null)"; then
        url="$(printf '%s' "$json" | grep -o '"url":"[^"]*\.jar"' | head -n 1 | cut -d'"' -f4 || true)"
    fi
    # Older v2 API as a fallback.
    if [ -z "$url" ]; then
        local build
        build="$(curl -fsSL "https://api.papermc.io/v2/projects/paper/versions/${MC_VERSION}/builds" 2>/dev/null \
            | grep -o '"build":[0-9]*' | tail -n 1 | cut -d: -f2 || true)"
        if [ -n "$build" ]; then
            url="https://api.papermc.io/v2/projects/paper/versions/${MC_VERSION}/builds/${build}/downloads/paper-${MC_VERSION}-${build}.jar"
        fi
    fi
    if [ -z "$url" ]; then
        echo "Could not find a Paper ${MC_VERSION} download. Download it from https://papermc.io/downloads/all"
        echo "and save it here as paper.jar."
        exit 1
    fi
    echo "From: $url"
    curl -fL -o paper.jar.part "$url"
    mv paper.jar.part paper.jar
}

if ! command -v java >/dev/null 2>&1; then
    echo "Java was not found. Install Java 17 or newer (for example from https://adoptium.net) and try again."
    exit 1
fi

[ -f paper.jar ] || download_paper

# Copy the freshly built plugin in, if it exists.
mkdir -p plugins
built="$(ls ../plugin/target/DemonSlayer-*.jar 2>/dev/null | head -n 1 || true)"
if [ -n "$built" ]; then
    rm -f plugins/DemonSlayer-*.jar
    cp "$built" plugins/
fi
if ! ls plugins/DemonSlayer-*.jar >/dev/null 2>&1; then
    echo "WARNING: the Demon Slayer plugin is not in plugins/. Build it first: (cd ../plugin && mvn package)"
fi

if ! grep -qs '^eula=true' eula.txt; then
    if [ "${EULA:-}" = "true" ]; then
        echo "eula=true" > eula.txt
    else
        echo "To run a Minecraft server you must agree to the Minecraft EULA: https://aka.ms/MinecraftEULA"
        read -r -p "Do you agree? (yes/no) " answer
        if [ "$answer" != "yes" ]; then
            echo "You need to accept the EULA to run the server."
            exit 1
        fi
        echo "eula=true" > eula.txt
    fi
fi

exec java -Xms"$MEMORY" -Xmx"$MEMORY" -jar paper.jar --nogui
