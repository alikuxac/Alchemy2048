# Alchemy 2048: KubeJS Integration

Alchemy 2048 integrates with KubeJS in Minecraft 1.21.1 to define evolution trees, remove recipes, and filter banned outputs via server scripts.

## 1. Recipe Registration

Place server scripts in `kubejs/server_scripts/alchemy2048.js`:

```javascript
ServerEvents.recipes(event => {
    // Register custom evolution tree
    event.custom({
        type: 'alchemy2048:evolution_tree',
        starter: {
            item: 'minecraft:amethyst_shard'
        },
        priority: 50,
        ladder: [
            'minecraft:quartz',
            'minecraft:glowstone_dust',
            'minecraft:prismarine_crystals',
            'minecraft:ender_pearl',
            'minecraft:ghast_tear',
            'minecraft:blaze_rod',
            'minecraft:echo_shard',
            'minecraft:dragon_breath',
            'minecraft:nether_star',
            'minecraft:totem_of_undying',
            'minecraft:elytra'
        ]
    });

    // Register with item tag starter
    event.custom({
        type: 'alchemy2048:evolution_tree',
        starter: {
            tag: 'c:raw_materials/iron'
        },
        priority: 25,
        ladder: [
            'minecraft:iron_nugget',
            'minecraft:iron_ingot',
            'minecraft:iron_block',
            'minecraft:raw_iron_block',
            'minecraft:anvil',
            'minecraft:shears',
            'minecraft:shield',
            'minecraft:iron_sword',
            'minecraft:iron_pickaxe',
            'minecraft:iron_chestplate',
            'minecraft:heavy_core'
        ]
    });

    // Remove default recipe
    event.remove({ type: 'alchemy2048:evolution_tree', output: 'minecraft:netherite_ingot' });
});
```

## 2. Banning Items via Tag Events

To prevent items from appearing in any ladder across all datapacks, append them to `#alchemy2048:banned_outputs`:

```javascript
ServerEvents.tags('item', event => {
    event.add('alchemy2048:banned_outputs', [
        'minecraft:bedrock',
        'minecraft:barrier',
        'minecraft:structure_void',
        'minecraft:command_block',
        'minecraft:repeating_command_block',
        'minecraft:chain_command_block',
        'minecraft:structure_block',
        'minecraft:jigsaw'
    ]);
});
```

Items matching `#alchemy2048:banned_outputs` are stripped when recipes load, and surrounding ladder rungs collapse to bridge the gap.

## 3. Recipe Schema Reference

Schema definition located at `kubejs/recipe_schema/alchemy2048/evolution_tree.json`:

```json
{
  "type": "kubejs:recipe_schema",
  "components": {
    "starter": "kubejs:input_item",
    "ladder": "kubejs:item_id_list",
    "priority": {
      "type": "kubejs:int",
      "optional": 0
    }
  },
  "constructor": [
    "starter",
    "ladder"
  ]
}
```

Enables editor autocompletion and schema validation during script execution.
