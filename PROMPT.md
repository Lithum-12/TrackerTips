# TrackerTips — AI Development Prompt

> 将本文件作为 System Prompt / 上下文提供给大模型，使其能够准确协助开发 TrackerTips 模组。本文件内容以源码中的 Javadoc 注释为主要依据编写。

## Project Overview

You are assisting with the development of **TrackerTips**, a Minecraft Forge mod.

- **Mod ID**: `trackertips`
- **Current Version**: 21.13 (see Versioning Scheme below)
- **Description**: Event-driven gameplay hints for Minecraft. A lightweight, customizable hint overlay: when a player meets certain conditions, a hint with icon/title/text pops up on screen.
- **Author**: Lithum-12
- **License**: LGPLv3-only
- **Repository**: https://github.com/Lithum-12/TrackerTips
- **Target**: Minecraft **1.20.1** + Forge **47.x** (ForgeGradle 6, official mappings, Java 17)
- **Hard dependency**: Cloth Config API (`me.shedaniel.cloth:cloth-config-forge:11.1.136`), client-side only, used for the event/theme editors.

## Versioning Scheme (MAJOR.MINOR)

Defined in `gradle.properties` as `mod_version`:

- **MAJOR** — incremented by 1 for every major update (new features / systems). MAJOR only ever grows; it is never reset.
- **MINOR** — the number of files changed in the release commit. Reset to the changed-file count on a major update; accumulates across minor-only updates.

## Architecture

Package root: `io.github.lithum12.trackertips`

```
io.github.lithum12.trackertips
├── TrackerTips.java          # @Mod entry: registers network, triggers (Triggers.init), client config
├── command/TTCommands.java   # /trackertips about|reload|test|list (Brigadier)
├── config/
│   ├── TTClientConfig.java   # Forge TOML client config: overlay position, ANCHOR (see HintAnchor),
│   │                         #   AVOID_CHAT_OVERLAP, max_width, max_hints, fade in/out
│   ├── TTConfigManager.java  # Loads global_config.json + hints/ from config & world save
│   │                         #   init(): first load + seeds defaults; reload(): re-reads from disk
│   │                         #   hintById(): lookup by id; readGlobalSettings/saveGlobalSettings: client GUI helpers
│   ├── TTSettings.java       # Global settings POJO (checkInterval, debug, defaultDuration, shortcutCommand...)
│   ├── HintAnchor.java       # Popup anchor enum: 8 screen positions; growsDown(), touchesLeftEdge()
│   └── HintDefinition.java   # Parses one hint JSON; matches()/matchesEvent()/currentState()
├── engine/HintEngine.java    # tickPlayer() polling + triggerEvent() event dispatch; forceShow() for /test
├── event/TTCommonEvents.java # Forge event bus: tick, pickup, kill, block break, potion, advancement, dimension, first join
├── network/                  # TTNetwork + ShowHintPacket / HideHintPacket (server -> client)
├── player/                   # TTCapabilities + PlayerHintData (shown/cooldown/count/chain-listening state, NBT persisted)
├── theme/
│   ├── TTTheme.java          # A serializable notification theme (colors, border, corner radius, padding, animations)
│   ├── TTThemeManager.java   # load(): built-in registrations first (lowest precedence), then user JSON files (override)
│   └── TTAnimation.java      # Data-driven animation record (type, duration, delay)
├── trigger/
│   ├── IHintTrigger.java         # State trigger: test(player), currentState(player)
│   ├── IEventHintTrigger.java    # Event trigger: matchesEvent(player, TriggerEvent)
│   ├── Triggers.java             # Public registry: register(ResourceLocation, Function<JsonObject, IHintTrigger>)
│   │                             #   init(): registers every built-in trigger type, called once from the mod constructor
│   ├── TriggerEvent.java         # Wraps Forge events; Type enum: ITEM_OBTAINED, KILL_ENTITY, MINE_BLOCK,
│   │                             #   POTION_ADDED, POTION_REMOVED, ADVANCEMENT, DIMENSION_CHANGE, FIRST_JOIN
│   ├── HintChain.java            # Follow-up trigger after a hint is shown; Action: DISMISS (hide only) | NEXT (hide + show next)
│   ├── FirstJoinTrigger.java     # Fires exactly once per player, ever (survives relog, restart, respawn via player-clone)
│   ├── StructureTrigger.java     # Player standing inside a structure; periodic check, not event-driven
│   ├── AdvancementTrigger.java   # "done" uses AdvancementEvent (fire once); "state" uses state polling (persistent)
│   ├── InDimensionTrigger.java   # PlayerChangedDimensionEvent; fires only once on entering the dimension
│   ├── ItemObtainedTrigger.java  # Precisely detects newly obtained items via Forge's ItemPickupEvent
│   ├── KillEntityTrigger.java    # Precisely detects player kills via LivingDeathEvent
│   ├── MineBlockTrigger.java     # Precisely detects block breaking via BlockEvent.BreakEvent
│   └── ... (GameTimeTrigger, PotionEffectTrigger, HasItemTrigger, HealthBelowTrigger, InStructureTrigger)
└── client/
    ├── ClientHintManager.java    # Client-side active hint list, toggle/dismiss
    ├── HintRenderer.java         # GuiOverlay rendering with anchor + theme + animation;
    │                             #   CHAT_MARGIN: gap between reserved chat area and left-anchored hint stack
    ├── TTKeyMappings.java        # H = dismiss, J = toggle
    ├── TTClientEvents.java       # Key handling, overlay registration
    └── gui/
        ├── TTConfigScreen.java        # Configuration hub using Minecraft 1.20.1's native TabNavigationBar
        ├── TTConfigTab.java           # One native tab: General / Events / Themes / Usage; scrollable
        ├── TTScrollBar.java           # Custom scrollbar for tabs
        ├── TTClothEventEditor.java    # Cloth Config editor for a single hint JSON
        ├── TTThemeEditorScreen.java   # Cloth Config editor for a single JSON theme
        ├── TTNewEventScreen.java      # Create new hint file
        ├── TTConfirmScreen.java       # Confirmation dialog
        ├── TTConfigScreenFactory.java # Entry point for ModMenu / config-menu integration
        └── TTLabel.java               # Small layout-aware text label for the native tab system
```

## Matching Model (core semantics, from HintEngine/HintDefinition Javadoc)

- `HintEngine.tickPlayer(player)` — **low-frequency polling entry point** for regular state conditions, called at the configured `checkInterval`.
- `HintEngine.triggerEvent(player, event)` — **Forge event dispatch entry point**; after an event fires, only event-triggers relevant to it are checked (plus live state conditions in "all" mode). `checkPlayer()` still exists but is backward-compat only — new code must use `triggerEvent()`.
- `HintDefinition.matches(player)` — regular polling; **event-driven triggers are skipped** so an event condition isn't mistaken for a persistent state.
- `HintDefinition.matchesEvent(player, event)` — event-driven conditions are supplied by `event`; regular state conditions are still checked live.
- `HintDefinition.currentState(player)` — current state for **persistent** hints; event-only triggers return false.
- `duration() <= 0` means the hint is **persistent** — it stays visible while `currentState()` holds (e.g. `health_below`), and is hidden via `HideHintPacket` when the state ends.
- `HintEngine.forceShow(player, id)` — forces a hint to display immediately, **bypassing its own triggers/cooldown/max_times**. Used by `/trackertips test`.

## Chain System (HintChain)

After a chained hint is shown, the engine starts a **chain listener** for it (`PlayerHintData.startChainListening`). While listening, the chain's trigger is checked; when it matches:

- `Action.DISMISS` — hide the popup that owns this chain; **no follow-up popup** is shown.
- `Action.NEXT` — hide the popup and **immediately show** `chain.next` (referenced by hint id).

The listener is stopped once it matches or the hint is otherwise hidden (`hideHint()` cancels any pending chain listener).

## Extending TrackerTips (API Entry Points)

These are the official entry points for addon mods / third-party integrations.

### 1. Registering a custom trigger (`Triggers.register`)

`Triggers.register(ResourceLocation id, Function<JsonObject, IHintTrigger> factory)` is public and additive — any mod can register its own trigger types. Trigger types are just JSON factories: the factory receives the trigger's JSON object from a hint file and returns a trigger instance.

**State trigger** (polled every `checkInterval` ticks):

```java
public class MyStateTrigger implements IHintTrigger {
    private final String someArg;

    private MyStateTrigger(String someArg) { this.someArg = someArg; }

    /** Factory: receives the trigger's JSON object from the hint file. */
    public static IHintTrigger fromJson(JsonObject json) {
        return new MyStateTrigger(GsonHelper.getAsString(json, "some_arg", "default"));
    }

    @Override
    public boolean test(ServerPlayer player) {
        return /* your condition */ false;
    }

    // Optional: only needed for PERSISTENT hints (duration <= 0).
    // Default implementation delegates to test().
    @Override
    public boolean currentState(ServerPlayer player) {
        return test(player);
    }
}
```

**Event trigger** (fires on a specific Forge event, via `HintEngine.triggerEvent`):

```java
public class MyEventTrigger implements IEventHintTrigger {
    public static IHintTrigger fromJson(JsonObject json) { return new MyEventTrigger(); }

    @Override
    public boolean matchesEvent(ServerPlayer player, TriggerEvent event) {
        return /* match against the event */ false;
    }
    // test() defaults to false — event triggers are never satisfied by polling.
}
```

**Registration timing**: `Triggers.init()` runs once from the mod constructor, so built-ins are registered very early. Third-party mods should register during `FMLCommonSetupEvent` (on the mod event bus), ideally inside `event.enqueueWork(...)`:

```java
@SubscribeEvent
public static void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() ->
        Triggers.register(new ResourceLocation("mymod", "my_trigger"), MyStateTrigger::fromJson));
}
```

Use your own mod id as the namespace (`mymod:my_trigger`) to avoid collisions. Once registered, the trigger works everywhere a trigger is accepted:

```json
"triggers": [ { "type": "mymod:my_trigger", "some_arg": "value" } ]
```

### 2. Custom events (`TriggerEvent` + `HintEngine.triggerEvent`)

`TriggerEvent.Type` is a **closed enum** — it cannot be extended by addons. To hook a hint to a Forge event TrackerTips doesn't cover:

- **Preferred**: write a *state trigger* (see above) that polls your condition — no event plumbing needed.
- **Alternative**: listen to the Forge event yourself, then dispatch a compatible built-in `TriggerEvent` via `HintEngine.triggerEvent(player, TriggerEvent.itemObtained/kill/mine/potionAdded/potionRemoved/advancement/dimensionChange(...))` so existing triggers can react to it.
- `FIRST_JOIN` is dispatched internally by `TTCommonEvents` the first time it observes a player; addons should not dispatch it.

### 3. Shipping resources with your trigger

- Add `trackertips.gui.trigger_type.mymod_my_trigger`-style entries in **your own** mod's language file so the built-in event editor dropdown can localize the type (TrackerTips' editor lists trigger types by translation key).
- Themes are plain JSON files in `config/trackertips/themes/`; a pack/mod can ship one by copying the file there (or users can create it in the theme editor). Hint files live in `config/trackertips/hints/` and are copied into each world save on first run.

### 4. Chains accept any registered trigger ("nested listeners")

Per the `HintChain` Javadoc: a chain's `trigger` can be **any registered `IHintTrigger`, built-in or addon-provided** — the same vocabulary as a hint's top-level triggers. This makes chains nested listeners scoped to a single already-shown popup:

```json
"chain": {
  "trigger": { "type": "trackertips:mine_block", "block": "minecraft:stone" },
  "action": "next",
  "next": "trackertips:welcome_step2"
}
```

So an addon-registered trigger (`"type": "mymod:my_trigger"`) works identically inside a chain — enabling guided popup sequences driven by custom addon conditions.

### 5. Config screen integration

`TTConfigScreenFactory` is the entry point for ModMenu or any other config-menu integration — point your config button at `TTConfigScreenFactory.create(parent)`.

### 6. Registering hints & themes programmatically

Addon mods don't have to rely on users hand-writing JSON — both hint definitions and themes have official programmatic entry points, both called from your own `FMLCommonSetupEvent` handler (ideally inside `event.enqueueWork(...)`).

**Register a built-in theme** (`TTThemeManager.registerBuiltIn(TTTheme)`):

```java
@SubscribeEvent
public static void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() ->
        TTThemeManager.registerBuiltIn(TTTheme.fromJson(myThemeJson)));
}
```

- Built-in themes are **always available** without requiring a user-authored JSON file.
- They load first, i.e. with the **lowest precedence**: a user-authored JSON file with the same `TTTheme#id()` always overrides the built-in registration.

**Register hint definitions** (`TTConfigManager.registerHintProvider(Supplier<Collection<HintDefinition>>)`):

```java
@SubscribeEvent
public static void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() ->
        TTConfigManager.registerHintProvider(MyHints::supply));
}

private static Collection<HintDefinition> supply() {
    return List.of(HintDefinition.fromJson(myHintJson));
    // may also return an empty collection
}
```

- Registered providers run on **every** `reload()` (including the initial load via `init()`); their definitions are merged into the loaded set keyed by `HintDefinition#id()` (so a provider definition can override a file-based one, and later registrations win).
- A provider returning an empty collection is fine; if a provider throws, the exception is logged and the other providers still run.
- Use case: ship a guided hint sequence / custom theme with your mod, without asking users to hand-write JSON under `config/trackertips/`.

## Popup Anchoring (HintAnchor)

- 8 anchors: BOTTOM_LEFT, LEFT, TOP_LEFT, TOP, TOP_RIGHT, RIGHT, BOTTOM_RIGHT, BOTTOM.
- `BOTTOM` = horizontally centered, anchored to the bottom of the screen — i.e. **directly above the hotbar**.
- `growsDown()` — whether newly-stacked hints extend **downward** (true) or **upward** (false) from the anchor point. BOTTOM grows upward; the rest grow downward.
- `touchesLeftEdge()` — whether this anchor touches the screen's left edge, **the only side vanilla chat can overlap**; `AVOID_CHAT_OVERLAP` reserves a `CHAT_MARGIN`-wide gap for left-anchored stacks.

## First Join (FirstJoinTrigger)

Fires **exactly once per player**: the very first time they ever log into a world running TrackerTips. Tracked via `PlayerHintData.hasJoinedBefore()` / `markJoinedBefore()` (irreversible for the lifetime of the save), persisted with the player's capability data — survives relogging, server restarts, and death/respawn (via the vanilla player-clone path).

## Configuration Files (runtime)

- `config/trackertips-client.toml` — Forge client config: enable, offset_x, offset_y, max_width, max_hints, fade_in, fade_out, anchor, avoid_chat_overlap.
- `config/trackertips/global_config.json` — global settings: enable, checkInterval, maxActiveHints, defaultDuration, debug, shortcutCommand. Copied to `saves/<world>/trackertips/world_config.json` (world copy wins on load).
- `config/trackertips/hints/*.json` — hint definitions (server-side logic). Also copied into each world save.
- `config/trackertips/themes/*.json` — themes (client rendering). Built-in registrations load first (lowest precedence); user JSON files override.

### Hint JSON format

```json
{
  "id": "trackertips:welcome",
  "once": true,
  "priority": 100,
  "cooldown": 0,
  "duration": 240,
  "require": "all",
  "accent": "F2C14E",
  "theme": "trackertips:default",
  "sound": "minecraft:block.note_block.pling",
  "pitch": 1.0,
  "icon": "",
  "max_times": 0,
  "title": { "translate": "trackertips.hint.welcome.title", "color": "gold", "bold": true },
  "text": [ { "translate": "trackertips.hint.welcome.text", "color": "gray" } ],
  "triggers": [ { "type": "trackertips:game_time", "mode": "after", "time": 200 } ],
  "chain": { "enabled": false, "action": "dismiss", "next": "", "trigger": {} }
}
```

Field semantics (aligned with `HintDefinition` Javadoc):

- `id` — stable, mod-namespaced identifier (e.g. `trackertips:welcome`); used by chains and `/trackertips test`.
- `once` — show at most once per player (equivalent to `max_times: 1` unless overridden).
- `priority` — stacking priority; **higher values render closer to the bottom of the on-screen stack**.
- `cooldown` — ticks that must pass before this hint can trigger again for the same player, after last being shown.
- `duration` — how long the popup stays visible, in ticks; `<= 0` means persistent (tied to `currentState`).
- `max_times` — max trigger count per player across the whole save; 0 = unlimited.
- `title`/`text` entries are Minecraft text components: `{"text": "..."}` or `{"translate": "key"}`, with optional `color`, `bold`, `italic`.
- Trigger types: `game_time` (mode after/before/range, time, end_time), `potion_effect` (mode added/removed/active, effect, amplifier_min), `has_item` / `item_obtained` (item, count), `advancement` (mode done/state, id), `in_dimension` (dimension), `in_structure` (structure; periodic check), `health_below` (health, half-hearts), `kill_entity` (entity), `mine_block` (block), `first_join` (no fields).

## Translation Keys — IMPORTANT CONVENTIONS

Language files: `src/main/resources/assets/trackertips/lang/en_us.json` and `zh_cn.json`. They must stay in sync, and use the same grouped ordering (each field's `.tooltip` key directly follows its label key).

When adding any user-facing string, you MUST add the key to BOTH language files. Long tooltips should use `\n` line breaks. Key prefixes:

- `trackertips.config.*` — Forge TOML config translations
- `trackertips.command.*` — command feedback
- `trackertips.message.*` — client toggle messages
- `trackertips.hint.*` — built-in hint text (e.g. welcome)
- `trackertips.gui.*` — all GUI labels, including dynamic suffixes:
  - `trackertips.gui.event.mode.{after|before|range}`
  - `trackertips.gui.event.potion_mode.{added|removed|active}`
  - `trackertips.gui.event.advancement_mode.{done|state}`
  - `trackertips.gui.event.require.{all|any}`
  - `trackertips.gui.event.chain_action.{dismiss|next}`
  - `trackertips.gui.anchor.{bottom_left|left|top_left|top|top_right|right|bottom_right|bottom}`
  - `trackertips.gui.theme.animation.type.{none|fade|slide|slide_up}`
  - `trackertips.gui.trigger_type.{game_time|potion_effect|has_item|advancement|item_obtained|in_dimension|in_structure|health_below|kill_entity|mine_block|first_join}`
- `key.trackertips.*` and `key.categories.trackertips` — keybinds

Vanilla keys (`gui.cancel`, `gui.yes`) are used directly and must not be added to lang files.

## Commands

- `/trackertips about` — mod info (no OP)
- `/trackertips reload` — re-reads settings + hints from disk (OP)
- `/trackertips test <id>` — force-show a hint, bypassing triggers/cooldown/max_times (OP)
- `/trackertips list [global/saves]` — list global/save-specific configuration (OP)
- `/tt` — shorthand for `/trackertips`, only when `shortcutCommand` is enabled (default off)

## Coding Conventions

- Java 17, switch expressions with arrow labels are used throughout.
- **Javadoc is the source of truth**: every public class/method carries a concise Javadoc describing its exact behavior (e.g. event sources, one-shot vs persistent semantics). When changing behavior, update the Javadoc first, then keep this file consistent with it.
- Comments often document bug fixes / design rationale in detail — preserve and extend this style.
- Server-side logic must never reference client-only classes; use packets (`ShowHintPacket`/`HideHintPacket`) to communicate.
- Per-player state lives in the `PlayerHintData` capability (NBT persisted, cloned on death).
- Config loading order: global config → world config (world overrides); hints from both global and world folders (world wins by ID).
- New triggers must: implement `IHintTrigger` or `IEventHintTrigger`, provide a static `fromJson(JsonObject)` factory, be registered in `Triggers.init()`, get a `trackertips.gui.trigger_type.*` translation key, and (if event-driven) have a `TriggerEvent.Type` + dispatch in `TTCommonEvents`.

## Current Status & Roadmap

- The mod is still in development; features may be incomplete.
- Only Forge 1.20.1 is supported; Fabric and more Forge versions are planned.
- Third-party integration (FTB Quests, KubeJS) is theoretically supported via the public `Triggers.register()` API and JSON files, but no dedicated integration layer exists yet.

## Task Guidance

When asked to modify this project:
1. Keep both language files in sync (en_us + zh_cn) for every new translation key; keep the grouped ordering (label key directly before its `.tooltip` key).
2. Follow the existing JSON config formats exactly; keep backward compatibility with existing hint files.
3. Prefer targeted edits; match the file's existing code style, Javadoc and comment conventions.
4. Client-only code must be guarded (`DistExecutor`, `OnlyIn`, or client-only event bus classes).
5. After changing config structures, update `TTConfigManager` defaults and the GUI editors accordingly.
6. When bumping `mod_version`, follow the Versioning Scheme section above.