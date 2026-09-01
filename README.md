<div align="center">

# TrackerTips

[![](Scroll.webp)](https://github.com/Lithum-12/TrackerTips)

more prominent popup hints, suitable for various modpacks.

</div>

[![GitHub](https://img.shields.io/github/stars/Lithum-12/TrackerTips?label=GitHub&labelColor=%232D2D2D)](https://github.com/Lithum-12/TrackerTips)
[![GitHub Release](https://img.shields.io/github/v/release/Lithum-12/TrackerTips?label=Latest%20version&labelColor=%232D2D2D)](https://github.com/Lithum-12/TrackerTips/releases)

### Compatibility Warning

This mod is still under development – most features are not yet complete, and compatibility with other mods has not been verified. If you run into issues, please report them to this mod's issue tracker first.

### Introduction

A lightweight, customizable hint overlay mod, similar to the vanilla toast notifications.

When a player meets certain conditions, a hint with an icon, title and body text pops up in the corner of the screen.

Just write a JSON file under the config folder – or edit it through Cloth Config API – to easily customize text, colors, and even entrance animations.

### Configuration

You can configure this mod in the following ways.

#### Commands

Commands adjust the world configuration.

No OP required:

- `/trackertips about` – show mod info

OP required:

- `/trackertips reload` – reload config files from the world
- `/trackertips test <id>` – force-test a hint
- `/trackertips list [global/saves]` – list global/world hints

#### Config Screen

Press **F8** (may be removed later) or use the Forge config button to open the GUI.

You can change general settings, or read how to use the mod.

With Cloth Config API installed, you can also fine-tune each hint's text / theme files.

#### Editing JSON

Server-side config files live under `config/trackertips/`:

- `hints/` – hint text files
- `themes/` – hint theme files

Client-side config files:

- `config/trackertips-client.toml` – client settings (overlay position, width, fade, etc.)
- `config/trackertips/global_config.json` – global properties

### Support

#### Triggers

The current version supports the following triggers:

- `First_join`
- `Item_obtained`
- `Kill_entity`
- `Mine_block`
- `Potion_added`
- `Potion_removed`
- `Advancement`
- `Dimension_change`

`triggers` allows other mods to register new triggers through the public entry point, and can also ship their own language and theme files.

#### Versions

Currently only Forge 1.20.1 is supported. Fabric and more Forge versions are planned.

### Feedback

If you have any suggestions, please let me know! I am open to any ideas for new features or improvements.

If you find any bugs, please report them on the GitHub issue tracker.

### License

LGPLv3-only. You can find the full license text in the LICENSE file included with the mod.

This mod is not affiliated with Mojang AB or Microsoft Corporation. All rights reserved to their respective owners.