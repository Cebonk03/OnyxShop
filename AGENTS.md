# Repository Guidelines

OnyxShop is a Minecraft economy/shop plugin for Spigot, Paper and Folia: GUI shops, rotating shops,
global/player stock limits, sell limits, dynamic pricing, Bedrock forms and multi-economy integration.
Preserve behaviour and binary layout over cosmetic cleanup — fix only what the task requires.

## Project Overview

- Single Gradle project (`settings.gradle`: `rootProject.name = 'OnyxShop'`), group `com.onyxshop`,
  version `1.0.0`.
- Output: one plugin jar (`build/libs/OnyxShop-1.0.0.jar`) carrying the
  `paperweight-mappings-namespace: spigot` manifest attribute.
- Everything compiles **offline** from vendored jars; no Maven repositories are consulted.

## Architecture & Data Flow

Manual, singleton-based plugin architecture (no DI framework). Layers, loosely:

| Layer | Packages | Responsibility |
|---|---|---|
| Bootstrap | `com.onyxshop.OnyxShop` | `JavaPlugin` + global service locator (`OnyxShop.getInstance()`), ordered wiring in `onEnable`, command/listener registration, scheduler facade |
| Adapters | `commands/`, `events/`, `inventorys/editor/`, `objects/navbar`, `objects/bedrock` | Bukkit commands (registered via `CommandMap`, not `plugin.yml`), inventory-click routing, admin item editor, GUIs/forms |
| Domain | `objects/` | `ShopSection` implementations (`shops/types/ShopCategory`, `RotatingShop`), `ShopItem`, `DisplayItem`, `TransactionMenu`, `ShopPage`, `User` |
| Services | `methodes/`, `providers/` | startup/reload, provider selection, stock/sell-limit/dynamic-pricing engines, economy/item/spawner/permission hooks |
| Persistence | `database/`, `files/` | `Database` contract with SQLite/MySQL/PostgreSQL dialects, `Query` command objects + `SQLBatchQueue`; YAML config/caches (`ConfigManager`, `TransactionLog`) |
| Platform seams | `util/`, `VersionHandler` | `ServerScheduler` (Bukkit vs Folia), `MetaUtils` (Bukkit/Adventure/Paper), NMS abstraction |
| Public API | `api/` | `OnyxShopHook` static facade, `PreTransactionEvent`/`PostTransactionEvent` |

Key flows:

- **Startup:** `OnyxShop.onEnable` → `ConfigManager.loadConfig` → `ProviderManager` (DB selection, optional
  stock/sell-limit/pricing providers) → `StartupReload.loadItems` builds `ShopSection`s → `StartupReload`
  reflectively loads the version adapter (`Class.forName("com.onyxshop.versions.<v>.<v>")`, around
  `StartupReload.java:450`).
- **Purchase:** `/shop` (`commands/Shop.java`) → `ShopSection.openShopSection` → `events/MenuHandler`
  (`onMenuClick`) → `TransactionMenu` → `MenuHandler.completeTransaction`: stock/balance checks →
  `PreTransactionEvent` → `EconomyProvider` withdraw/deposit → `LimitedStockProvider` /
  `DynamicPricingProvider` updates → item/command delivery → `PostTransactionEvent` → `TransactionLog`.
- **Persistence:** in-memory provider caches are authoritative; writes are wrapped as `Query` objects,
  queued in `SQLBatchQueue` and flushed asynchronously. Never call `Database` from the main thread for
  hot paths.
- **Versioning:** main code only depends on `VersionHandler`; concrete NMS classes live in
  `src/versions/<module>/java` and are picked at enable by `Class.forName("com.onyxshop.versions.<v>.<v>")`
  (paper-mapped variants for the `paper_*` modules). Adding NMS behaviour means touching the interface plus
  every compiled version module. `src/apistubs/java` is a compile-only source set for soft-depend plugin
  APIs that are not published anywhere — it ships nothing at runtime.

The final jar is assembled from three inputs, in `build.gradle`'s `jar` task: `main` output, each compiled
`v_<module>` source set, and the vendored `quartz.jar` plus the HikariCP classes. Every class under
`com/onyxshop/` is compiled from source; there is no vendored bytecode tree.

Dependency direction is pragmatic, not strictly layered: `Database` imports `providers.ScheduleManager`,
and `RotatingShopProvider` extends `objects/shops/types/RotatingShop`. Do not "fix" these couplings
without an explicit request.

## Key Directories

```
src/main/java/com/onyxshop/     main plugin sources (415 files)
  commands/                     /shop, /sellall, /sellgui, /editshop (/onyxshop subcommands)
  events/                       MenuHandler (click dispatcher), join/level/spawner listeners
  objects/                      domain model: shops, items, transaction UI, users, stands, prices
  providers/                    economy/item/spawner/permission hooks, stock, sell limits, pricing
  methodes/                     startup orchestration, item building, messaging (sic: "methodes")
  database/                     Database interface, SQL dialects, queries, batch queue
  files/                        ConfigManager, Lang, Translatable, caches, TransactionLog
  inventorys/                   admin item-editor screens (sic: "inventorys")
  util/                         scheduler, meta, NBT, XMaterial, item builders, caches
  api/                          public API facade and events
src/apistubs/java/              32 compile-only stubs for third-party plugin APIs
src/versions/<mc>/java/         per-Minecraft NMS modules (v1_16_R3 … v26_3, paper_v1_21_R6/R7)
src/optional/java/              excluded reference trees (Quartz, Terracotta)
src/main/resources/             plugin.yml, paper-plugin.yml, config.yml, LanguageFiles,
                                <mc>-shops/ + <mc>-sections/ YAML, ExampleShop.yml, ExampleSection.yml
libs/cp, libs/api, libs/nms     vendored compile jars (Paper/Spigot API, NMS, plugin APIs)
libs/vendored                   quartz.jar shipped verbatim (HikariCP comes from libs/cp)
scripts/                        compile.sh, git hooks
.github/workflows/ci.yml        offline build + Paper smoke matrix
```

## Development Commands

```bash
./gradlew --offline clean build          # full build (no tests exist)
./gradlew --offline jar                  # package only
./gradlew --offline compileJava          # fastest single-source-set check
./gradlew --offline compileVersions      # all compiled NMS source sets

unzip -p build/libs/*.jar META-INF/MANIFEST.MF | grep paperweight-mappings-namespace  # must print: spigot
python3 scripts/gen_dependency_manifest.py  # refresh libs/VERSIONS.tsv after changing libs/
python3 scripts/check_updates.py            # ONLINE only: compare vendored Maven coordinates
sh scripts/install-hooks.sh              # install the compile-check pre-commit hook
SKIP_COMPILE=1 git commit -m "..."       # bypass that hook deliberately
```

Always pass `--offline`; network resolution is unsupported by design. CI uses JDK 25 and Gradle 9.9.2
(wrapper), so match that JDK locally.

## Code Conventions & Common Patterns

- **Java 8 language level** (`sourceCompatibility`/`targetCompatibility = 1.8`). No `var`, records,
  `switch` expressions, text blocks, or sealed types. Do not switch the build to `--release 8` (it rejects
  record supertypes) or to a newer target.
- **Local variable names are descriptive; the rest of the legacy style is intentional.** The decompiled
  `var1`, `var2`, … locals and parameters were renamed to descriptive names in every compiled source set
  (`src/main`, `src/apistubs`, `src/versions`) — give new locals descriptive names and never reintroduce
  `varN`/`nullx` names. Raw `Map` returns, redundant casts and misspelled packages (`methodes`,
  `inventorys`, `SubCommad`) are load-bearing for binary compatibility; leave them alone.
- **Formatting:** 3-space indentation inside class bodies, explicit single-class imports (no wildcards),
  grouped `java.*` → `com.onyxshop.*` → third-party → `org.bukkit.*`. Keep the surrounding style even
  where it differs from modern Java norms.
- **Naming:** PascalCase types, camelCase members, UPPER_SNAKE constants, kebab-case YAML keys
  (`sell-limit-player`, `dynamic-pricing`), `OnyxShop.<permission>` permission nodes, `ONYX_`/`OnyxShop`
  branded identifiers (`NBTMaps`, `getOnyxNbtString`).
- **Errors:** declare `throws SQLException` / custom `util.exceptions.*` on persistence and config seams;
  callers catch, log via `SendMessage`, and degrade. Config failures set the global `ConfigManager.badYaml`
  latch — check it before acting on config.
- **Async/scheduling:** never call `Bukkit.getScheduler()` directly. Use the plugin facade
  (`plugin.runTask`, `runTaskAsync`, `runTaskLater`, `runTaskTimer`, `runTaskAsyncTimer`,
  `runKillableTask*`), which delegates to `util/scheduler/ServerScheduler` so Folia stays supported.
  For work bound to one player (opening/refreshing their GUI, touching their inventory), use the
  player-scoped hops `plugin.runTaskFor` / `plugin.runTaskLaterFor`: they dispatch on the entity
  scheduler under Folia and degrade to a plain delayed/chained task on Bukkit. Keep global-state
  work on `runTask`/`runTaskLater`.
- **Messaging:** use `methodes/SendMessage` (info/warn/error/debug) and `files/Lang` + `Translatable` for
  player text; do not add `java.util.logging` calls or hardcoded English in user-facing paths.
- **State:** per-player state lives in `objects/User` via Guava `LoadingCache<UUID, User>`; plugin-wide state
  is public fields on `OnyxShop`. Reuse these instead of introducing new registries.
- **Integration seams:** new platform hooks implement the matching interface (`EconomyProvider`,
  `ItemProvider`, `VersionHandler`, `Database`, `MetaUtils`, `Translatable`) and register through
  `EconomyHandler` / `ItemManager` / `ProviderManager` — never hardcode a plugin call in domain code.
- **NMS code:** `net.minecraft.*`/`org.bukkit.craftbukkit.*` imports are allowed **only** in
  `src/versions/**`. Main sources go through `VersionHandler`.

## Important Files

- `src/main/java/com/onyxshop/OnyxShop.java` — lifecycle, wiring, command registration, scheduler facade.
- `src/main/java/com/onyxshop/VersionHandler.java` — NMS contract (NBT, lore, titles, anvils).
- `src/main/java/com/onyxshop/events/MenuHandler.java` — click routing and transaction execution.
- `src/main/java/com/onyxshop/methodes/StartupReload.java` — config → domain/UI loading, adapter selection.
- `src/main/java/com/onyxshop/methodes/ProviderManager.java` — DB choice and optional-service bootstrap.
- `src/main/java/com/onyxshop/database/SQLBatchQueue.java`, `database/queries/Query.java` — async writes.
- `src/main/java/com/onyxshop/files/ConfigManager.java` — config/shop YAML loading and `badYaml` latch.
- `src/main/resources/config.yml` — runtime defaults (`storage-type: SQLite`, `economy-provider: VAULT`,
  telemetry off). `config-version: 1` is informational only; `ConfigUtil` performs no version comparison.
- `src/main/resources/plugin.yml` / `paper-plugin.yml` — **two descriptors ship in one jar**. Paper reads
  `paper-plugin.yml` (`api-version: '1.19'`, `loader: com.onyxshop.util.PaperLibrariesLoader`,
  `has-open-classloader: false`, all soft-deps as `dependencies.server.*`); legacy Bukkit/Spigot reads
  `plugin.yml` (`api-version: 1.13`, `libraries:` for Adventure + PostgreSQL). Changing a dependency,
  permission node or plugin identity means editing **both** files.
- `build.gradle` — source sets, exclusions, version modules, jar manifest.

## Runtime/Tooling Preferences

- **Runtime:** Java 25 JDK to build; class-file target Java 8 (major 52) so 1.16–1.26 servers can load it.
  Gradle wrapper 9.9.2, Groovy DSL, `org.gradle.parallel=false`.
- **Hermetic, vendored dependencies only:** `libs/cp`, `libs/api`, `libs/nms`, `libs/vendored`. Never add a remote `repositories {}` block or a Maven coordinate; drop the jar in `libs/`
  and wire it via `compileOnly fileTree`.
- **Version inventory:** `libs/VERSIONS.tsv` pins every vendored jar to its detected coordinate and SHA-256.
  Regenerate it after adding or replacing jars. Coordinates come from embedded `pom.properties`, then a
  filename fallback (`paper-api-*` → `io.papermc.paper:paper-api`, `paper-dev-*`, `spigot-*`).
  `scripts/check_updates.py` is the only network-dependent tool: Maven Central and the PaperMC repo for
  normal coordinates, plus `fill.papermc.io` to compare Paper build numbers and channels. It is never
  invoked by the build or CI.
- **Main-source exclusions** (`build.gradle`): `org/terracotta/**`,
  `org/quartz/core/QuartzScheduler_Stub.java`, `com/onyxshop/versions/**`. Everything else under
  `src/main/java` is compiled from source.
- **Compiled version modules:** 21 Spigot modules (`v1_16_R3` … `v26_3`) and two Paper-mapped modules
  (`paper_v1_21_R6`, `paper_v1_21_R7`) are compiled by `compileVersions`; they are not `vendoredModules`.
  `./gradlew --offline compileVersions` is part of CI and of the pre-commit hook when only version sources
  change.
- **Quartz:** compiles against `libs/cp/quartz-2.3.2.jar`, ships 2.3.0 classes from `libs/vendored/quartz.jar`.
- **Runtime env:** no required environment variables. Storage is SQLite by default
  (`plugins/OnyxShop/database.db`); MySQL/PostgreSQL are optional. `util/PaperLibrariesLoader` downloads
  `org.postgresql:postgresql:42.7.4` at runtime and selects its mirror from `PAPER_DEFAULT_CENTRAL_REPOSITORY`
  or `-Dorg.bukkit.plugin.java.LibraryLoader.centralURL`. Schema is created with
  `CREATE TABLE IF NOT EXISTS` plus idempotent `ALTER TABLE ... ADD COLUMN` migrations in
  `SQLite.migrate()` (no versioned migration framework).
- **Optional code index:** `.codegraph/` (gitignored, regenerable) may exist locally; when present, prefer
  `codegraph explore` over `grep` for symbol/flow questions. `build/`, `bin/` and `.gradle/` are generated
  and gitignored — never treat them as sources.

## Testing & QA

Tests are JUnit 6, vendored under `libs/cp` and wired through `src/test/java` (no Mockito, no
JaCoCo/Checkstyle/Spotless; `src/main/resources/checkstyle.xml` is legacy payload, not wired into the
build). `./gradlew --offline test` runs them. Coverage is deliberately narrow — pure logic only
(`SQLQueries` formatting, `Query` equality/dedupe, `Gradient` rendering); anything touching Bukkit
objects is verified by the runtime smoke gate instead. Verification is:

1. **Compile gate:** `./gradlew --offline build` (or the pre-commit hook, which runs `scripts/compile.sh`
   and blocks on any `error:` line).
2. **Packaging gate:** the jar manifest must contain `paperweight-mappings-namespace: spigot`. Without it,
   Paper's reflection rewriter fails at enable with `NoClassDefFoundError: net/minecraft/...`.
3. **Runtime smoke:** CI boots Paper 1.20.4 / 1.21.4 / 1.21.10 / 1.21.11 / 26.1.2 / 26.3 with the built
   jar in `server/plugins/`, waits for `Done (`, sends `stop` over stdin so `onDisable` runs, and fails if
   `Enabling OnyxShop` or `Disabling OnyxShop` is absent or if `Could not load .*OnyxShop` /
   `Error occurred while enabling OnyxShop` / `Exception in thread "Server thread"` appears.
4. **Descriptor gate:** `python3 scripts/verify_descriptors.py` (wired to `check` as
   `verifyDescriptors`) compares `plugin.yml` with `paper-plugin.yml` — name/version/main, permission
   sets, `softdepend`+`loadbefore` vs `dependencies.server`, and duplicate keys.

Manual repro locally: copy the jar into a Paper server's `plugins/`, set `eula=true`, `online-mode=false`,
`level-type=flat`, and check `server.log` for a clean enable and `/shop` opening.

When changing behaviour, state explicitly which of the three gates you ran; do not claim test coverage
that does not exist. New runtime verification should be added as a CI step in `.github/workflows/ci.yml`
rather than as an ad-hoc script.
