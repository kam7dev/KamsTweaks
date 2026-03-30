package kam.kamsTweaks.ext;


import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.java.*;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Holders;
import org.geysermc.geyser.api.util.Identifier;

public class GeyserItemData implements EventRegistrar {
    public GeyserItemData() {
        Logger.info("Setting up geyser listener");
        GeyserApi.api().eventBus().subscribe(this, GeyserDefineCustomItemsEvent.class, this::onGeyserDefineCustomItems);
        GeyserApi.api().eventBus().subscribe(this, GeyserDefineResourcePacksEvent.class, this::onGeyserDefineResourcePacks);
    }

    public void onGeyserDefineResourcePacks(GeyserDefineResourcePacksEvent event) {
        KamsTweaks.getInstance().saveResource("bedrock.zip", true);
        event.register(ResourcePack.builder(PackCodec.path(KamsTweaks.getInstance().getDataPath().resolve("bedrock.zip"))).build());
    }


    public void onGeyserDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        // KamsTweaks
        Logger.info("Setting up KamsTweaks items with Geyser");

        // Claim Tool
        event.register(Identifier.of("music_disc_pigstep"), CustomItemDefinition.builder(
                        Identifier.of("kamstweaks:claim_tool"), // Bedrock item identifier
                        Identifier.of("minecraft:structure_void") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("kamstweaks"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
                .priority(1)
                .build());

        // Anvixo's Bits
        Logger.info("Setting up Anvixo's Bits items with Geyser");
        // 23 / Daisy Bell
        event.register(Identifier.of("music_disc_chirp"), CustomItemDefinition.builder(
                        // for some reason ltsmp:23 breaks
                        Identifier.of("ltsmp:disc_23"), // Bedrock item identifier
                        Identifier.of("ltsmp:23") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder()
                        .protectionValue(7)
                        .creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .displayName("§e%item.record.name\n§7Anvixo - 23")
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // Bat Bat
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:bbat"), // Bedrock item identifier
                        Identifier.of("ltsmp:bbat") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // Blessed Trident
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:blessedtrident_old"), // Bedrock item identifier
                        Identifier.of("minecraft:trident") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder()
                        .allowOffhand(true)
                        .creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.TOOL, JavaTool.builder()
                        .canDestroyBlocksInCreative(false)
                        .rule(JavaTool.Rule.builder().speed(.0000001f).blocks(Holders.builder().tag(Identifier.of("none")).build()).build())
                        .build())
                .priority(1)
                .build());
        // New model
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:blessedtrident"), // Bedrock item identifier
                        Identifier.of("ltsmp:blessedtrident") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().allowOffhand(true).creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.TOOL, JavaTool.builder()
                        .canDestroyBlocksInCreative(false)
                        .rule(JavaTool.Rule.builder().speed(.0000001f).blocks(Holders.builder().tag(Identifier.of("none")).build()).build())
                        .build())
                .priority(1)
                .build());

        // Cooked Flesh
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:cookedflesh"), // Bedrock item identifier
                        Identifier.of("ltsmp:cookedflesh") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                        .consumeSeconds(1.6f)
                        .build())
                .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder()
                        .nutrition(4)
                        .saturation(0.8f)
                        .canAlwaysEat(false)
                        .build())
                .priority(1)
                .build());

        // Enchanted (disc)
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:enchanted_disc"), // Bedrock item identifier
                        Identifier.of("ltsmp:enchanted_disc") // Java item model
                )
                .displayName("§b%item.record.name\n§7noski - enchanted")
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .priority(1)
                .build());

        // Flower Crown
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:flowercrown"), // Bedrock item identifier
                        Identifier.of("ltsmp:flowercrown") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.builder()
                        .slot(JavaEquippable.EquipmentSlot.HEAD)
                        .build())
                .priority(1)
                .build());

        // Genderfluid
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:genderfluid"), // Bedrock item identifier
                        Identifier.of("ltsmp:genderfluid") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                        .consumeSeconds(1.6f)
                        .animation(JavaConsumable.Animation.DRINK)
                        .build())
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .priority(1)
                .build());

        // Haunt Muskie
        event.register(Identifier.of("music_disc_wait"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:hauntmuskie"), // Bedrock item identifier
                        Identifier.of("ltsmp:hauntmuskie") // Java item model
                )
                .displayName("§b%item.record.name\n§7C418 - Haunt Muskie")
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // Haunt Muskie Cover
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:hauntmuskiecover"), // Bedrock item identifier
                        Identifier.of("ltsmp:hauntmuskiecover") // Java item model
                )
                .displayName("§d%item.record.name\n§7Jurjo13 - Haunt Muskie (Cover)")
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // Could not replicate hemelytra functionality in bedrock as a custom item.
//        // Hemelytra
//        event.register(Identifier.of("elytra"), CustomItemDefinition.builder(
//                        Identifier.of("ltsmp:hemelytra"), // Bedrock item identifier
//                        Identifier.of("ltsmp:hemelytra") // Java item model
//                )
//                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
//                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
//                .component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.builder().slot(JavaEquippable.EquipmentSlot.CHEST).build())
//                .priority(1)
//                .build());

        // Maraca
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:maraca"), // Bedrock item identifier
                        Identifier.of("ltsmp:maraca") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                        .consumeSeconds(.1f)
                        .animation(JavaConsumable.Animation.BRUSH)
                        .build())
                .component(JavaItemDataComponents.USE_COOLDOWN, JavaUseCooldown.builder()
                        .seconds(120f)
                        .cooldownGroup(Identifier.of("ltsmp:maraca"))
                        .build())
                .priority(1)
                .build());

        // Ominous End Key
        event.register(Identifier.of("ominous_trial_key"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:ominous_end_key"), // Bedrock item identifier
                        Identifier.of("ltsmp:ominous_end_key") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
                .priority(1)
                .build());

        // Permafrosted Snowball
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:permafrost"), // Bedrock item identifier
                        Identifier.of("ltsmp:permafrost") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 16)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                        .consumeSeconds(1.6f)
                        .build())
                .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder()
                        .nutrition(1)
                        .saturation(0)
                        .canAlwaysEat(true)
                        .build())
                .priority(1)
                .build());

        // Pike
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:pike"), // Bedrock item identifier
                        Identifier.of("ltsmp:pike") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.TOOL, JavaTool.builder()
                        .rule(JavaTool.Rule.builder()
                                .blocks(Holders.builder().tag(Identifier.of("minecraft:leaves")).build())
                                .speed(6)
                                .build())
                        .defaultMiningSpeed(1)
                        .canDestroyBlocksInCreative(false)
                        .build()
                )
                .priority(1)
                .build());

        // Ominous Pink Liquid
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:pinkpot"), // Bedrock item identifier
                        Identifier.of("ltsmp:pinkpot") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                        .animation(JavaConsumable.Animation.DRINK)
                        .consumeSeconds(1.6f)
                        .build())
                .priority(1)
                .build());

        // Sunglasses
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:sunglasses"), // Bedrock item identifier
                        Identifier.of("ltsmp:sunglasses") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.builder().slot(JavaEquippable.EquipmentSlot.HEAD).build())
                .priority(1)
                .build());

        // Radical Sunglasses
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:sunglasses_rad"), // Bedrock item identifier
                        Identifier.of("ltsmp:sunglasses_rad") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.builder().slot(JavaEquippable.EquipmentSlot.HEAD).build())
                .priority(1)
                .build());

        // Wild / 18
        event.register(Identifier.of("music_disc_far"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:wild"), // Bedrock item identifier
                        Identifier.of("ltsmp:wild") // Java item model
                )
                .displayName("§b%item.record.name\n§7JamiesName - 18")
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // Winner's Dinner
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:winner_dinner"), // Bedrock item identifier
                        Identifier.of("ltsmp:winner_dinner") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 3)
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                        .consumeSeconds(3.2f)
                        .animation(JavaConsumable.Animation.EAT)
                        .build())
                .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder()
                        .nutrition(6)
                        .saturation(13)
                        .canAlwaysEat(true)
                        .build())
                .priority(1)
                .build());

        // Withered Potato
        event.register(Identifier.of("poisonous_potato"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:withered_potato"), // Bedrock item identifier
                        Identifier.of("ltsmp:withered_potato") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 3)
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                        .consumeSeconds(1.6f)
                        .animation(JavaConsumable.Animation.EAT)
                        .build())
                .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder()
                        .nutrition(8)
                        .saturation(1)
                        .canAlwaysEat(true)
                        .build())
                .priority(1)
                .build());

        // Woosh
        event.register(Identifier.of("music_disc_wait"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:woosh"), // Bedrock item identifier
                        Identifier.of("ltsmp:woosh") // Java item model
                )
                .displayName("§e%item.record.name\n§7Terraainn - Woosh")
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());
    }
}
