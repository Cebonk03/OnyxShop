# OnyxShop

A Minecraft economy/shop plugin for Spigot, Paper and Folia (1.16 – 26.x): GUI shops, rotating
shops, global/player stock limits, sell limits, dynamic pricing, Bedrock forms and multi-economy
support.

## Build

The build is hermetic: every dependency is vendored under `libs/`, so Gradle never contacts a
repository. Use JDK 25 and always pass `--offline`.

```bash
./gradlew --offline build          # full build (java 8 target, 23 NMS modules, jar)
./gradlew --offline jar            # package only
./gradlew --offline compileJava    # fastest main-source check
./gradlew --offline compileVersions
./gradlew --offline test           # JUnit 6 (vendored)
./gradlew --offline verifyDescriptors
```

Output: `build/libs/OnyxShop-1.0.0.jar`. It must carry the manifest attribute
`paperweight-mappings-namespace: spigot`; without it Paper cannot rewrite NMS references.

`sh scripts/install-hooks.sh` installs a pre-commit hook that runs `scripts/compile.sh` (or
`compileVersions` when only `src/versions/**` changed) and blocks on any javac error.

## Install

1. Drop the jar into `plugins/`.
2. Start the server once to generate `plugins/OnyxShop/`.
3. Edit `plugins/OnyxShop/config.yml` (storage, economy, limits) and the shop YAML under
   `plugins/OnyxShop/shops/` and `plugins/OnyxShop/sections/`.
4. `/sreload` applies config/shop changes; `/shop` opens the menu; `/editshop` is the in-game editor.

Storage defaults to SQLite (`plugins/OnyxShop/database.db`); MySQL and PostgreSQL are supported via
`storage-type`. Existing databases are migrated in place at connect (idempotent `ALTER TABLE ...
ADD COLUMN` for the `restock` columns).

## Commands

| Command | Permission | Purpose |
|---|---|---|
| `/shop [section\|search <query>]` | `OnyxShop.shop` (+ `OnyxShop.shop.<section>`) | open the shop |
| `/sellall [inventory\|hand\|item] [qty]` | `OnyxShop.sellall` / `.sellallhand` / `.sellallitem` | bulk selling |
| `/sellgui [player]` | `OnyxShop.sellgui` | sell-through-GUI |
| `/shopgive <section> <item> [player] [amount]` | `OnyxShop.shopgive` | give a shop item |
| `/sreload` | `OnyxShop.reload` | reload configs, shops and providers |
| `/editshop <subcommand>` | `OnyxShop.editshop.<subcommand>` | in-game shop editor |

Console and remote console are allowed for the admin commands; command blocks are not.

## Verification

There is no server in this repository. The gates are:

1. `./gradlew --offline build` (compile) — CI and the pre-commit hook.
2. Manifest attribute check (packaging).
3. `./gradlew --offline verifyDescriptors` (the two plugin descriptors stay in sync).
4. Paper smoke boot in CI (6 versions) with a real `stop`, asserting a clean enable **and** disable.

See `AGENTS.md` for the architecture, persistence model and coding conventions.
