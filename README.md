# AE2 Molecular Assembler Plus

AE2 Molecular Assembler Plus is a Forge addon for Minecraft 1.20.1 that extends Applied Energistics 2 auto-crafting with universal pattern encoding and dedicated assemblers for modded crafting workflows.

The mod is not limited to 9x9 recipes. It can support different recipe shapes, station layouts, tiers, catalysts, energy costs, and other requirements from supported mods.

## Features

- Extreme Pattern Encoding Terminal with a 9x9 crafting grid.
- Draconic Pattern Encoding Terminal for Draconic Evolution fusion crafting patterns.
- New in **1.2.0**: Magical Pattern Encoding Terminal and seven Botania assemblers with miniature 3D stations.
- ME mana cells (1k-256k), mana import/export buses, catalyst selection, and station-specific interfaces.
- Dedicated blank and encoded pattern items for supported recipe families.
- Molecular assembler variants for supported recipe systems and tiered workflows.
- JEI recipe transfer support: the JEI `+` button can move supported recipes into the matching terminal.
- Optional recipe integration for supported crafting-table mods.
- Conditional crafting recipes that appear only when the matching optional mod is installed.

## Supported Mods

AE2 is the base mod. Everything else in this table is optional: AE2 Molecular Assembler Plus can load without it.

| Mod | Version | Pattern Terminal | JEI `+` Transfer | Conditional Recipes | Links |
| --- | --- | --- | --- | --- | --- |
| Applied Energistics 2 | 1.20.1, AE2 15.4.8+ | Core mod | Vanilla crafting recipes | Base AE2 recipes | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2), [Modrinth](https://modrinth.com/mod/ae2) |
| Extended Crafting | 1.20.1, 6.0.10+ | Yes | Yes | Yes | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/extended-crafting), [Modrinth](https://modrinth.com/mod/extended-crafting) |
| Extended Crafting: Expanded | 1.20.1 | Partial | Via Extended Crafting categories | Via Extended Crafting ids | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/extended-crafting-expanded) |
| Re:Avaritia | 1.20.1 | Yes | Yes | Yes | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/re-avaritia), [Modrinth](https://modrinth.com/mod/re-avaritia) |
| AvaritiaNeo | 1.20.1 | Yes | Yes | Yes | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/avaritianeo) |
| Extended Terminal | 1.20.1 | No | No | No | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/extended-terminal), [Modrinth](https://modrinth.com/mod/extended-terminal) |

## Other Mods

These integrations are for non-standard crafting systems that are not simple crafting grids.

| Mod | Supported Workflows | Pattern Terminal | Assembler Support | JEI `+` Transfer | Notes |
| --- | --- | --- | --- | --- | --- |
| Draconic Evolution | Fusion crafting | Draconic Pattern Encoding Terminal | Wyvern, Draconic, and Chaotic Molecular Assemblers | Yes | Encoded patterns store fusion tier and energy cost. Assemblers enforce tier limits and consume fusion energy. |
| Botania | Mana Pool, Runic Altar, Petal Apothecary, Terrestrial Agglomeration Plate, Elven Trade, Botanical Brewery, Pure Daisy | Magical Pattern Encoding Terminal | Seven station-specific Botanical Assemblers | Yes | Optional. ME mana storage/transport, recipe validation, and up to four acceleration cards. |

Notes:

- Extended Crafting: Expanded uses the same mod id as Extended Crafting, so support is handled through the Extended Crafting integration path.
- Re:Avaritia and AvaritiaNeo both use Avaritia-style ids. Recipes use common item ids so they can work with either fork when possible.
- Extended Terminal is listed because it targets similar terminal workflows, but AE2 Molecular Assembler Plus does not depend on it.

## JEI Integration

When JEI is installed, supported recipe categories show a `+` transfer button. Clicking it sends the recipe layout into the matching pattern terminal.

Supported JEI transfer targets include:

- Minecraft crafting recipes
- Extended Crafting table categories
- Re:Avaritia table categories
- AvaritiaNeo extreme crafting
- Draconic Evolution fusion crafting
- All seven supported Botania station categories, with automatic station, catalyst, and reagent/container selection

## Conditional Recipes

Base AE2 recipes are always available for:

- `ccapplied:extreme_blank_pattern`
- `ccapplied:extreme_pattern_terminal`
- `ccapplied:extreme_molecular_assembler`

Extra recipes are loaded only when the matching mod is present:

- `extendedcrafting` recipes use Extended Crafting components and tables.
- `avaritia` recipes use Avaritia/Re:Avaritia/AvaritiaNeo items that share common ids.
- `draconicevolution` recipes add Draconic Fusion patterns, the Draconic Pattern Encoding Terminal, and tiered Draconic Molecular Assemblers.
- `botania` enables the magical terminal, patterns, seven assemblers, mana cells and buses. Botania is not bundled or required.

Encoded crafting patterns are not craftable directly. They must be created through the matching Pattern Encoding Terminal so they contain recipe data.

## Roadmap

See [ROADMAP.md](ROADMAP.md).

## Botania in 1.2.0

Connect a Magical Pattern Encoding Terminal to a powered ME network. Use JEI's `+` button or
ghost ingredients to prepare a recipe, insert a blank magical pattern, and encode it.
Each assembler accepts only its own station's patterns. Use a Pattern Provider for autocrafting,
or install a pattern in the assembler and supply its ingredients. Mana is supplied through ME
mana storage; import/export buses connect the network to supported Botania mana containers.

Assemblers include station-specific menus, progress and reserved-mana bars, and four acceleration
card slots (up to 16x processing speed). Frames retain AE2-style glass and lighting, with native
miniature Botania stations inside.

Pure Daisy supports item-to-item block transformations only. Fluids, world-function recipes,
Gaia summoning and the Mana Enchanter are not supported. See [Botania details](BOTANIA-DEVELOPMENT.md).

## Building

Requires JDK 17. From this branch's project directory:

```powershell
.\gradlew.bat build
```

The jar is written to `build/libs/`. Integration tests:

```powershell
.\gradlew.bat runGameTestServer
.\gradlew.bat runGameTestServer -PwithoutBotania
```

`-PwithoutBotania` only removes Botania and its supporting libraries from the development runtime;
the distributed mod always treats Botania as optional. With dependencies already cached, use
`--offline` (and `-x downloadMCMeta` for an offline Forge build).

## Team

Developed by **Gasai / ChillCraft Team**. Bug reports, balancing feedback and contributions are welcome.
The ready-to-use project page description is in [DESCRIPTION.md](DESCRIPTION.md).
