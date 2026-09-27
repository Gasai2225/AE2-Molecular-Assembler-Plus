# Botania integration — 1.2.0

Included in 1.2.0 for Forge 1.20.1 and NeoForge 1.21.1, as an optional integration:

- Independent ME mana resource and 1k–256k cells.
- Cable-mounted mana import/export buses, acceleration and redstone cards.
- One magical pattern item; encoded patterns have a glint, a station name, native AE2 ingredient/output tooltips and Shift output preview.
- Encoded patterns are limited to one per stack; blanks stack to 64. Sneak-use clears back to a magical blank.
- ME magical pattern terminal with network search, station tabs, pool catalyst selection and recipe preview.

## Encoder

Connect the encoder to a powered ME cable with a free channel.
Put ingredient copies in the left grid. These are ghost slots and do not consume ingredients.
Each occupied slot represents one item. Repeat an ingredient in multiple slots when needed.

The separate reagent slot accepts seeds for the apothecary, livingrock/the recipe reagent
for the runic altar, or the brew container for the brewery.
No per-operation water input is required by apothecary templates.
Put a blank magical pattern in the upper pattern slot and press the downward arrow.
The lower pattern slot must be empty.

Each station has its own panel: single input for pool/Daisy, three inputs for the plate,
ingredients around the brewing vessel/apothecary seed, a separate altar reagent,
and five input sockets plus one output socket for elven trades. Only active sockets accept ghost items.
Switching stations clears ghost inputs (never real items or patterns).
Returned rune outputs are paged in groups of six; additional trade outputs are paged one at a time.
Elven trades use exact ingredients, one item per input socket (repeat an ingredient in
another socket when two are required). Mixed-item trades are supported, including the
1.21.1 manasteel ingot/block trade. The five-slot limit applies to recipe entry.

For pools click the square catalyst button below the recipe to cycle None, Alchemy and Conjuration.
The selected mode is stored in the pattern.
The Daisy substitution button allows alternate blocks accepted by the same native recipe,
only when outputs, mana and processing time stay identical. It is off by default.
Other stations still use exact inputs; no cosmetic substitution toggles are shown for them.
Mana cost is available on the encode button tooltip.
Outputs and mana requirements are resolved from the world's recipe manager,
not trusted from item NBT. Unknown format versions and missing/invalid recipes are rejected.
Mana appears as a required resource in the AE2 pattern; returned runes/catalysts are outputs.

## Botanical assemblers

Seven separate blocks execute the corresponding magical patterns. Place the matching
assembler next to an AE2 Pattern Provider, put encoded magical patterns in the provider,
and connect both to a powered ME network with item and mana storage. Leave the assembler's
pattern slot empty for provider-driven jobs. Alternatively, insert a matching magical pattern
into the assembler and supply its ingredients through the GUI or item automation; this mode
reserves mana directly from ME storage and does not accept provider jobs. Each block renders uniformly scaled native Botania models inside the AE2 assembler shell,
both in-world and in the inventory. Brewery and pylons use Botania's own special renderers;
the portal uses its original outline and animated Botania portal texture.
Each menu has its station's name/layout, a restricted pattern slot, separate output slots,
progress and reserved-mana bars, and four speed-card slots. Manual inputs accept only their
encoded ingredients; active job inputs and templates cannot be removed. Provider jobs remain
read-only. Rejected patterns never consume CPU counters or fall back to an untyped inventory.
All seven machine recipes use the craftable Terrasteel Assembler Frame component.
The shell retains AE2 geometry, UVs and glass; only the twelve frame beams receive a green tint.
The native AE2 light model is rendered full-bright through its tripwire layer when ME is powered.
Power state is synchronized to clients. No catalyst blocks beneath pools or blocks around daisies.

The crafting CPU supplies exact ingredients and mana together. A machine accepts one
job atomically, checks the native recipe, and consumes 2 AE per processing tick.
Pending ingredients, mana and progress survive saves. Finished outputs wait when ME
storage is full. Breaking an unfinished machine returns its ingredients and a mana
recovery capsule; breaking a finished one drops only the unexported results.
Pool machines support all three catalyst modes. Apothecary crafting uses one water
bucket when building the machine, not on every flower recipe.

## Not finished

- Assemblers have station-specific menus, manual-template/provider modes and four speed-card slots.
  Cards double throughput each (up to 16x) without reducing total mana or AE cost per craft.
  Processing animations are not implemented.
- JEI's recipe-transfer button fills the magical encoder for all seven stations, including
  the pool catalyst, ritual reagent and brewing container. Only a recipe ID is sent;
  the server validates the native recipe before changing ghost slots. No real items are moved.
  Unsupported world/fluid transformations show an error without clearing the current editor.
- Network tick scheduling no longer credits pre-job idle time to a new craft. New jobs wake
  promptly; natural-tick tests cover intermediate menu progress and all four speed-card counts.
  Both game versions have been play-tested by the maintainer.
- Pure Daisy support currently covers item-to-item block transformations only.
  Fluids, non-item outputs and recipes with world functions need a dedicated adapter.
- Assembler block visuals and station-specific menus have been play-tested on both versions.
- Gaia summoning and Mana Enchanter are intentionally excluded.

Release version: 1.2.0. Maintained by Gasai / ChillCraft Team.

The magical terminal frame is reconstructed from each target AE2 version's
`assets/ae2/textures/guis/pattern.png` (TeamAppliedEnergistics, LGPL-3.0-or-later),
with a taller station panel; original frame pixels and native tab transitions are retained.
Botanical Machinery was consulted for station layouts, not copied as a dependency.
