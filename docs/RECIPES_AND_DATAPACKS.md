# Alchemy 2048: Recipes and Datapacks

Alchemy 2048 presets use standard Minecraft 1.21.1 recipe files. Each recipe defines an evolution tree that links a starter item or tag to an 11-tier ladder.

## 1. Recipe Format

Datapack location: `data/<namespace>/recipe/<recipe_name>.json`

```json
{
  "type": "alchemy2048:evolution_tree",
  "starter": {
    "item": "minecraft:cobblestone"
  },
  "priority": 100,
  "ladder": [
    "minecraft:stone",
    "minecraft:smooth_stone",
    "minecraft:deepslate",
    "minecraft:copper_ingot",
    "minecraft:iron_ingot",
    "minecraft:gold_ingot",
    "minecraft:redstone",
    "minecraft:lapis_lazuli",
    "minecraft:diamond",
    "minecraft:emerald",
    "minecraft:netherite_ingot"
  ]
}
```

### Fields
- `type`: Must be `"alchemy2048:evolution_tree"`.
- `starter`: Standard Minecraft `Ingredient`:
  - Item starter: `{"item": "minecraft:cobblestone"}`
  - Tag starter: `{"tag": "c:cobblestones"}` or `{"tag": "minecraft:saplings"}`
- `priority` *(integer, optional, default: 0)*: Highest priority wins if multiple trees share a starter item.
- `ladder`: Array of 11 item IDs matching values 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, and 2048.

## 2. Missing Items and Ladder Compaction

If an item in `ladder` is unregistered or removed by a mod:
- The ladder collapses across the gap.
- Active tiles on a board pointing to an invalid item drop to the nearest valid lower tier.
- If fewer than 2 valid items remain, or if the starter item does not exist, the recipe is ignored.

## 3. Banning Outputs

To prevent specific items from appearing in any ladder:
Add the item ID to `#alchemy2048:banned_outputs` at `data/alchemy2048/tags/item/banned_outputs.json`:

```json
{
  "replace": false,
  "values": [
    "minecraft:bedrock",
    "minecraft:barrier",
    "minecraft:command_block"
  ]
}
```

Any ladder entry in this tag is stripped on load and the ladder collapses across the gap.

## 4. Hot-Reload Safe Eject

When `/reload` removes an active recipe during gameplay:
- The machine detects that the current recipe was unregistered.
- The board enters Safe Eject mode: sliding is disabled, and Claim All unlocks so the player can claim their items without lost progress.
