# Roadmap

AE2 Molecular Assembler Plus connects AE2 pattern automation to modded crafting systems.
Maintained by **ChillCraft Team**. Current release: **1.2.0** for Forge 1.20.1 and NeoForge 1.21.1.

## Completed Integrations

| Mod | Support |
| --- | --- |
| Extended Crafting | Large crafting-table recipes and JEI transfer through the Extreme Pattern Encoding Terminal. Compatible Expanded categories use the same integration. |
| Re:Avaritia / AvaritiaNeo | Large crafting recipes and JEI transfer where supported by the installed fork and Minecraft version. |
| Draconic Evolution | Fusion patterns, Wyvern/Draconic/Chaotic assemblers, tier and energy checks, and JEI transfer. |
| Botania (1.2.0) | Magical pattern terminal; Mana Pool, Runic Altar, Petal Apothecary, Terrestrial Agglomeration Plate, Elven Trade, Botanical Brewery and Pure Daisy assemblers; JEI transfer; ME mana cells and buses. |

## Completed in 1.2.0

- Optional Botania registration and conditional recipes.
- Station-specific encoder and assembler interfaces with native miniature 3D station models.
- Correct station routing for installed patterns and Pattern Provider jobs.
- Mana reservation, progress indicators and four acceleration cards per assembler.
- Native recipe validation, ingredient/output tooltips and encoded-pattern previews.
- Regression coverage for recipe transfer, resource recovery, progress and acceleration.

## Planned Integrations

| Mod | Planned Support |
| --- | --- |
| Blood Magic | Blood altar workflows and stations requiring catalysts, tiers or special inputs. |
| Other crafting station mods | Additional grids and custom stations based on demand and technical fit. |

## Future Improvements

- Expand non-grid recipe compatibility and keep optional-mod loading covered by regression tests.
- Consider EMI and REI transfer support.
- Explore adapters for fluid inputs and world-dependent transformations, including additional Pure Daisy recipes.
- Improve shared station infrastructure as integrations grow.
- Evaluate additional processing animations and usability improvements.

Gaia summoning and the Mana Enchanter are outside the current Botania integration.
Plans are not release promises; priorities depend on feedback and technical feasibility.
