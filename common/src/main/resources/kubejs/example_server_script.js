ServerEvents.recipes(event => {
    event.custom({
        type: 'alchemy2048:evolution_tree',
        starter: {
            item: 'minecraft:cobblestone'
        },
        priority: 10,
        ladder: [
            'minecraft:stone',
            'minecraft:smooth_stone',
            'minecraft:deepslate',
            'minecraft:copper_ingot',
            'minecraft:iron_ingot',
            'minecraft:gold_ingot',
            'minecraft:redstone',
            'minecraft:lapis_lazuli',
            'minecraft:diamond',
            'minecraft:emerald',
            'minecraft:netherite_ingot'
        ]
    });

    event.custom({
        type: 'alchemy2048:evolution_tree',
        starter: {
            tag: 'minecraft:saplings'
        },
        priority: 20,
        ladder: [
            'minecraft:oak_planks',
            'minecraft:birch_planks',
            'minecraft:spruce_planks',
            'minecraft:jungle_planks',
            'minecraft:acacia_planks',
            'minecraft:dark_oak_planks',
            'minecraft:mangrove_planks',
            'minecraft:cherry_planks',
            'minecraft:bamboo_planks',
            'minecraft:crimson_planks',
            'minecraft:warped_planks'
        ]
    });
});

ServerEvents.tags('item', event => {
    event.add('alchemy2048:banned_outputs', [
        'minecraft:bedrock',
        'minecraft:command_block',
        'minecraft:barrier'
    ]);
});
