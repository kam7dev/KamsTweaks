package kam.kamsTweaks.ext;


import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Logger;

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

@SuppressWarnings({"CommentedOutCode", "SpellCheckingInspection"})
public class GeyserItemData implements EventRegistrar {
    public GeyserItemData() {
        Logger.info("Setting up geyser listener");
        GeyserApi.api().eventBus().subscribe(this, GeyserDefineCustomItemsEvent.class, this::onGeyserDefineCustomItems);
        GeyserApi.api().eventBus().subscribe(this, GeyserDefineResourcePacksEvent.class, this::onGeyserDefineResourcePacks);
    }

    public void onGeyserDefineResourcePacks(GeyserDefineResourcePacksEvent event) {
        KamsTweaks.get().saveResource("abrp-bedrock.zip", true);
        KamsTweaks.get().saveResource("ktrp-bedrock.zip", true);
        event.register(ResourcePack.builder(PackCodec.path(KamsTweaks.get().getDataPath().resolve("abrp-bedrock.zip"))).build());
        event.register(ResourcePack.builder(PackCodec.path(KamsTweaks.get().getDataPath().resolve("ktrp-bedrock.zip"))).build());
    }

    void registerForKT(GeyserDefineCustomItemsEvent event) {
        Logger.info("Setting up KamsTweaks items with Geyser");
        // Claim Tool
        event.register(Identifier.of("music_disc_pigstep"), CustomItemDefinition.builder(
                        Identifier.of("kamstweaks:claim_tool"), // Bedrock item identifier
                        Identifier.of("minecraft:structure_void") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("kamstweaks"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .priority(1)
                .build());

        // Grave Head / Stone Brickstep
        event.register(Identifier.of("music_disc_pigstep"), CustomItemDefinition.builder(
                        Identifier.of("kamstweaks:stone_brickstep"), // Bedrock item identifier
                        Identifier.of("minecraft:stone_brick_wall") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("kamstweaks"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .displayName("Stone Brickstep\n§7Lena Raine - Pigstep")
                .priority(1)
                .build());

        // Ice Mountain / Freezeflame (Ice)
        event.register(Identifier.of("music_disc_pigstep"), CustomItemDefinition.builder(
                        Identifier.of("kamstweaks:ice_mountain"), // Bedrock item identifier
                        Identifier.of("kamstweaks:ice_mountain") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("kamstweaks"))
                .displayName("§e%item.record.name\n§7km7dev - Freezeflame (Ice)")
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // Ice Mountain / Freezeflame (Lava)
        event.register(Identifier.of("music_disc_pigstep"), CustomItemDefinition.builder(
                        Identifier.of("kamstweaks:lava_path"), // Bedrock item identifier
                        Identifier.of("kamstweaks:lava_path") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("kamstweaks"))
                .displayName("§e%item.record.name\n§7km7dev - Freezeflame (Lava)")
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());
    }

    void registerABDiscs(GeyserDefineCustomItemsEvent event) {
        // 23 / Daisy Bell
        event.register(Identifier.of("music_disc_chirp"), CustomItemDefinition.builder(
                        // for some reason ltsmp:23 breaks
                        Identifier.of("ltsmp:disc_23"), // Bedrock item identifier
                        Identifier.of("ltsmp:23") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder()
                        .creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .displayName("§e%item.record.name\n§7Anvixo - 23")
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // If You Were Here / Benektelse
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:benektelse"), // Bedrock item identifier
                        Identifier.of("ltsmp:benektelse") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder()
                        .creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .displayName("§e%item.record.name\n§7Benektelse - If You Were Here")
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // Enchanted
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

        // Rib
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:rib"), // Bedrock item identifier
                        Identifier.of("ltsmp:rib") // Java item model
                )
                .displayName("§b%item.record.name\n§7Anvixo - Rib")
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());
    }

    void registerABFood(GeyserDefineCustomItemsEvent event) {
        // Withered Potato
        event.register(Identifier.of("poisonous_potato"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:withered_potato"), // Bedrock item identifier
                        Identifier.of("ltsmp:withered_potato") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
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

        // Unisicle
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:unisicle"), // Bedrock item identifier
                        Identifier.of("ltsmp:unisicle") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 16)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder().build())
                .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder()
                        .nutrition(2)
                        .saturation(10)
                        .canAlwaysEat(true)
                        .build())
                .priority(1)
                .build());

        // Bisicle
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:bisicle"), // Bedrock item identifier
                        Identifier.of("ltsmp:bisicle") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 16)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder().build())
                .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder()
                        .nutrition(2)
                        .saturation(10)
                        .canAlwaysEat(true)
                        .build())
                .priority(1)
                .build());

        // Iatrisic Fungus / Hypercoffee
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:hypercoffee"), // Bedrock item identifier
                        Identifier.of("ltsmp:hypercoffee") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder().animation(JavaConsumable.Animation.DRINK).build())
                .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder().nutrition(1).saturation(5).canAlwaysEat(true).build())
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
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

        // Winner's Dinner
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:winner_dinner"), // Bedrock item identifier
                        Identifier.of("ltsmp:winner_dinner") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 16)
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                        .animation(JavaConsumable.Animation.EAT)
                        .build())
                .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder()
                        .nutrition(6)
                        .saturation(10)
                        .canAlwaysEat(true)
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
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
                .priority(1)
                .build());

        // Sweet Popped Chorus Fruit
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:sweetpoppedchorus"), // Bedrock item identifier
                        Identifier.of("ltsmp:sweetpoppedchorus") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 16)
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder()
                        .consumeSeconds(1.6f)
                        .build())
                .component(JavaItemDataComponents.USE_COOLDOWN, JavaUseCooldown.builder()
                        .seconds(3)
                        .cooldownGroup(Identifier.of("ltsmp:sweetpoppedchorus"))
                        .build())
                .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder()
                        .nutrition(4)
                        .saturation(10)
                        .canAlwaysEat(true)
                        .build())
                .priority(1)
                .build());
    }

    void registerABMisc(GeyserDefineCustomItemsEvent event) {
        // Bottled Lightning
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:bottle_lightning"), // Bedrock item identifier
                        Identifier.of("ltsmp:bottle_lightning") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.USE_COOLDOWN, JavaUseCooldown.builder()
                        .seconds(15)
                        .cooldownGroup(Identifier.of("ltsmp:lightning")).build())
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder().animation(JavaConsumable.Animation.DRINK).build())
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .priority(1)
                .build());

        // Crystalized Ender Eye
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:crystalized_ender_eye"), // Bedrock item identifier
                        Identifier.of("ltsmp:crystalized_ender_eye") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder().animation(JavaConsumable.Animation.NONE).consumeSeconds(10).build())
                .component(JavaItemDataComponents.USE_COOLDOWN, JavaUseCooldown.builder()
                        .seconds(60)
                        .cooldownGroup(Identifier.of("ltsmp:spawnpoint")).build())
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 3)
                .priority(1)
                .build());

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
                        .seconds(10f)
                        .cooldownGroup(Identifier.of("ltsmp:maraca"))
                        .build())
                .priority(1)
                .build());

        // Tungsten Cube
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:tungsten"), // Bedrock item identifier
                        Identifier.of("ltsmp:tungsten") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .build());

        // Permafrosted Snowball
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:permafrost"), // Bedrock item identifier
                        Identifier.of("ltsmp:permafrost") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 16)
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
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

        // End Key
        event.register(Identifier.of("ominous_trial_key"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:end_key"), // Bedrock item identifier
                        Identifier.of("ltsmp:end_key") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
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

        // Ominous Key
        event.register(Identifier.of("ominous_trial_key"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:ominous_key"), // Bedrock item identifier
                        Identifier.of("ltsmp:ominous_key") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
                .priority(1)
                .build());


        // Null (for the one crafting recipe)
        event.register(Identifier.of("stick"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:null"), // Bedrock item identifier
                        Identifier.of("minecraft:air") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits").allowOffhand(true))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // Charged Firework
        event.register(Identifier.of("firework_rocket"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:charged_fw"), // Bedrock item identifier
                        Identifier.of("ltsmp:charged_fw") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
                .priority(1)
                .build());


        //  Below are non-replicable with custom item
/*

        // Outpost Explorer Map
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:outpostmap"), // Bedrock item identifier
                        Identifier.of("ltsmp:outpostmap") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
                .priority(1)
                .build());

        // Bunker Explorer Map
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:bunkermap"), // Bedrock item identifier
                        Identifier.of("ltsmp:bunkermap") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
                .priority(1)
                .build());

        // Wrinkled Explorer Map
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:wrinkledmap"), // Bedrock item identifier
                        Identifier.of("ltsmp:wrinkledmap") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 64)
                .priority(1)
                .build());
*/

    }

    void registerABGear(GeyserDefineCustomItemsEvent event) {
        // Bat Bat
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:bbat"), // Bedrock item identifier
                        Identifier.of("ltsmp:bbat") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.REPAIRABLE, JavaRepairable.builder().items(Holders.of(Identifier.of("minecraft:leather"))).build())
                .component(JavaItemDataComponents.MAX_DAMAGE, 32)
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

        // Journey
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:journey"), // Bedrock item identifier
                        Identifier.of("ltsmp:journey") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits").allowOffhand(true))
                .component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .priority(1)
                .build());

        // Pike
        event.register(Identifier.of("music_disc_creator"), CustomItemDefinition.builder(
                        Identifier.of("ltsmp:pike"), // Bedrock item identifier
                        Identifier.of("ltsmp:pike") // Java item model
                )
                .bedrockOptions(CustomItemBedrockOptions.builder().creativeCategory(CreativeCategory.EQUIPMENT).creativeGroup("bits"))
                .component(JavaItemDataComponents.MAX_STACK_SIZE, 1)
                .component(JavaItemDataComponents.MAX_DAMAGE, 201)
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

        // Blessed Trident
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
    }

    void registerForAB(GeyserDefineCustomItemsEvent event) {
        Logger.info("Setting up Anvixo's Bits items with Geyser");
        registerABDiscs(event);
        registerABFood(event);
        registerABMisc(event);
        registerABGear(event);
    }

    public void onGeyserDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        Logger.info("Setting up geyser items");
        registerForKT(event);
        registerForAB(event);
    }
}
