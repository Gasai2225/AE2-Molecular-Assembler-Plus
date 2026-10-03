# AE2 Molecular Assembler Plus 1.2.1

For NeoForge 1.21.1.

## Fixes

- Recipes are now revalidated after a datapack reload, preventing stale patterns from producing a different result.
- Fixed Draconic Evolution recipes with overlapping ingredients while preserving each ingredient's `consume` flag.
- Added support for initialized tools and items containing components/modules in Draconic Fusion without losing their data.
- Fixed catalyst and output stack sizes in Draconic Fusion, including the Awakened Draconium Block recipe.
- Excess ingredients, reusable components, containers, and recipe-specific remainders are now preserved.
- Crafting remainders are buffered, persisted across restarts, dropped when the machine is broken, and extractable through pipes.
- Draconic Fusion progress is reset when the pattern changes and preserved when the same pattern is reloaded.
- Existing Extreme and Draconic patterns can now be re-encoded without another Blank Pattern.
- Fixed Botania substitutions for the Petal Apothecary and Pure Daisy, including different valid alternatives in repeated slots.
- The Botanical Assembler now validates the promised recipe result, persists the active job, and correctly updates progress in an open GUI.
- Fixed button sizing, catalyst/output stack count display, and Draconic terminal label localization.
- Fixed the build artifact name: `AE2 Molecular Assembler Plus-1.2.1-neoforge-1.21.1.jar`.

## Tests

- Added GameTests for substitutions, recipe reloads, crafting remainders, persistence, pipe extraction, and pattern re-encoding.
