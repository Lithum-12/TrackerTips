# TrackerTips — AI Development Prompt

> 将本文件作为 System Prompt / 上下文提供给大模型，使其能够准确协助开发 TrackerTips 模组。

## Project Overview

You are assisting with the development of **TrackerTips**, a Minecraft Forge mod.

- **Mod ID**: `trackertips`
- **Description**: Event-driven gameplay hints for Minecraft. A lightweight, customizable hint overlay: when a player meets certain conditions, a hint with icon/title/text pops up in the corner of the screen.
- **Author**: Lithum-12
- **License**: LGPLv3-only
- **Repository**: https://github.com/Lithum-12/TrackerTips
- **Target**: Minecraft **1.20.1** + Forge **47.x** (ForgeGradle 6, official mappings, Java 17)
- **Hard dependency**: Cloth Config API (`me.shedaniel.cloth:cloth-config-forge:11.1.136`), client-side only, used for the event/theme editors.

## Architecture

Package root: `io.github.lithum12.trackertips`

```
io.github.lithum12.trackertips
├── TrackerTips.java          # @Mod entry: registers network, triggers, client config
├── command/TTCommands.java   # /trackertips about|reload|test|list (Brigadier)
├── config/
│   ├── TTClientConfig.java   # Forge TOML client config (overlay position/width/fade)
│   ├── TTConfigManager.java  # Loads global_config.json + hints/ from config & world save
│   ├── TTSettings.java       # Global settings POJO (checkInterval, debug, defaultDuration, shortcutCommand...)
│   └── HintDefinition.java   # Parses one hint JSON; matches()/matchesEvent()/currentState()
├── engine/HintEngine.java    # tickPlayer() polling + triggerEvent() event dispatch; sends packets
├── event/TTCommonEvents.java # Forge event bus: tick, pickup, kill, block break, potion, advancement, dimension, first join
├── network/                  # TTNetwork + ShowHintPacket / HideHintPacket (server -> client)
├── player/                   # TTCapabilities + PlayerHintData (per-player shown/cooldown/count state, NBT persisted)
├── theme/
│   ├── TTTheme.java          # Theme POJO (colors, border, corner radius, padding, animations)
│   ├── TTThemeManager.java   # Loads/saves themes from config/trackertips/themes/*.json
│   └── TTAnimation.java      # Animation helpers (none/fade/slide/slide_up)
├── trigger/
│   ├── IHintTrigger.java         # State trigger: test(player), currentState(player)
│   ├── IEventHintTrigger.java    # Event trigger: matchesEvent(player, TriggerEvent)
│   ├── Triggers.java             # Public registry: register(ResourceLocation, Function<JsonObject, IHintTrigger>)
│   ├── TriggerEvent.java         # Wraps Forge events; Type enum: ITEM_OBTAINED, KILL_ENTITY, MINE_BLOCK,
│   │                             #   POTION_ADDED, POTION_REMOVED, ADVANCEMENT, DIMENSION_CHANGE, FIRST_JOIN
│   └── ... (GameTimeTrigger, PotionEffectTrigger, HasItemTrigger, AdvancementTrigger,
│            ItemObtainedTrigger, InDimensionTrigger, HealthBelowTrigger,
│            KillEntityTrigger, MineBlockTrigger, FirstJoinTrigger)
└── client/
    ├── ClientHintManager.java    # Client-side active hint list, toggle/dismiss
    ├── HintRenderer.java         # GuiOverlay rendering with theme + animation
    ├── TTKeyMappings.java        # H = dismiss, J = toggle
    ├── TTClientEvents.java       # Key handling, overlay registration
    └── gui/
        ├── TTConfigScreen.java   # Native tabbed config screen (TabNavigationBar)
        ├── TTConfigTab.java      # General/Events/Themes/Usage tabs, scrollable
        ├── TTScrollBar.java      # Custom scrollbar for tabs
        ├── TTClothEventEditor.java # Cloth Config editor for a single hint JSON
        ├── TTThemeEditorScreen.java# Cloth Config editor for themes
        ├── TTNewEventScreen.java   # Create new hint file
        ├── TTConfirmScreen.java    # Confirmation dialog
        └── TTLabel.java
```

## Configuration Files (runtime)

- `config/trackertips-client.toml` — Forge client config: enable, offset_x, offset_y, max_width, max_hints, fade_in, fade_out.
- `config/trackertips/global_config.json` — global settings: enable, checkInterval, maxActiveHints, defaultDuration, debug, shortcutCommand. Copied to `saves/<world>/trackertips/world_config.json` (world copy wins on load).
- `config/trackertips/hints/*.json` — hint definitions (server-side logic). Also copied into each world save.
- `config/trackertips/themes/*.json` — themes (client rendering).

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

- `title`/`text` entries are Minecraft text components: either `{"text": "..."}` or `{"translate": "key"}`, with optional `color`, `bold`, `italic`.
- `duration: -1` = persistent hint (stays while condition holds).
- Trigger types: `game_time` (mode after/before/range, time, end_time), `potion_effect` (mode added/removed/active, effect, amplifier_min), `has_item` / `item_obtained` (item, count), `advancement` (mode done/state, id), `in_dimension` (dimension), `health_below` (health), `kill_entity` (entity), `mine_block` (block), `first_join` (no fields).

## Translation Keys — IMPORTANT CONVENTIONS

Language files: `src/main/resources/assets/trackertips/lang/en_us.json` and `zh_cn.json`. They must stay in sync.

When adding any user-facing string, you MUST add the key to BOTH language files. Key prefixes:

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
  - `trackertips.gui.theme.animation.type.{none|fade|slide|slide_up}`
  - `trackertips.gui.trigger_type.{game_time|potion_effect|has_item|advancement|item_obtained|in_dimension|health_below|kill_entity|mine_block|first_join}`
- `key.trackertips.*` and `key.categories.trackertips` — keybinds

Vanilla keys (`gui.cancel`, `gui.yes`) are used directly and must not be added to lang files.

## Coding Conventions

- Java 17, switch expressions with arrow labels are used throughout.
- Comments often document bug fixes / design rationale in detail — preserve and extend this style.
- Server-side logic must never reference client-only classes; use packets (`ShowHintPacket`/`HideHintPacket`) to communicate.
- Per-player state lives in the `PlayerHintData` capability (NBT persisted, cloned on death).
- Config loading order: global config → world config (world overrides); hints from both global and world folders (world wins by ID).
- New triggers must: implement `IHintTrigger` or `IEventHintTrigger`, provide a static `fromJson(JsonObject)` factory, be registered in `Triggers.init()`, get a `trackertips.gui.trigger_type.*` translation key, and (if event-driven) have a `TriggerEvent.Type` + dispatch in `TTCommonEvents`.

## Commands

- `/trackertips about` — mod info (no OP)
- `/trackertips reload` — reload configs (OP)
- `/trackertips test <id>` — force-show a hint (OP)
- `/trackertips list [global/saves]` — list hints (OP)

## Current Status & Roadmap

- The mod is still in development; features may be incomplete.
- Only Forge 1.20.1 is supported; Fabric and more Forge versions are planned.
- Third-party integration (FTB Quests, KubeJS) is theoretically supported via the public `Triggers.register()` API and JSON files, but no dedicated integration layer exists yet.

## Task Guidance

When asked to modify this project:
1. Keep both language files in sync (en_us + zh_cn) for every new translation key.
2. Follow the existing JSON config formats exactly; keep backward compatibility with existing hint files.
3. Prefer targeted edits; match the file's existing code style and comment conventions.
4. Client-only code must be guarded (`DistExecutor`, `OnlyIn`, or client-only event bus classes).
5. After changing config structures, update `TTConfigManager` defaults and the GUI editors accordingly.