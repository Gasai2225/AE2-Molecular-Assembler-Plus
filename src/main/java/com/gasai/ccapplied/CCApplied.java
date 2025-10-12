package com.gasai.ccapplied;

import com.gasai.ccapplied.common.core.PatternInit;
import com.gasai.ccapplied.common.item.ExtremePatternEncodedItem;
import com.gasai.ccapplied.common.util.RecipeEncodingUtil;
import com.gasai.ccapplied.core.registry.ModBlockEntities;
import com.gasai.ccapplied.core.registry.ModBlocks;
import com.gasai.ccapplied.core.registry.ModItems;
import com.gasai.ccapplied.core.registry.ModMenus;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.server.level.ServerPlayer;


@Mod(CCApplied.MODID)
public final class CCApplied {

    public static final String MODID = "ccapplied";

    @SuppressWarnings("removal")
    public CCApplied() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(bus);
        ModBlocks.register(bus);
        ModBlockEntities.register(bus);
        ModMenus.MENUS.register(bus);

        // Регистрируем слушатель мод-фазы (вместо прямого вызова в конструкторе)
        bus.addListener(this::commonSetup);

        // Главный класс содержит слушатели — регистрируемся на общую шину Forge
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent e) {
        e.enqueueWork(PatternInit::registerDecoders);
    }

    /* ================= Команда кодирования 9x9-паттерна ================= */



    private int execEncode(Recipe<?> recipe,
                           ResourceLocation id,
                           boolean substitutes,
                           int time,
                           ServerPlayer player) {
        if (recipe == null) {
            player.displayClientMessage(Component.literal("[CCApplied] Recipe not found: " + id), false);
            return 0;
        }

        var tag = RecipeEncodingUtil.encodeRecipeToTag(id, recipe, player.level(), substitutes, time);
        ItemStack pattern = new ItemStack(ModItems.ENCODED_EXTREME_PATTERN.get());
        ExtremePatternEncodedItem.writePatternData(pattern, tag);

        // Пытаемся положить в инвентарь, иначе — дроп у игрока
        if (!player.getInventory().add(pattern)) {
            Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), pattern);
        }

        player.displayClientMessage(
                Component.literal("[CCApplied] Encoded pattern for " + id +
                        " (subs=" + substitutes + ", time=" + time + "t)"),
                false
        );
        return 1;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        var dispatcher = e.getDispatcher();

        dispatcher.register(Commands.literal("ccapplied")
                // только операторы/админы
                .requires(src -> src.hasPermission(2))

                // === Старая команда: закодировать рецепт и выдать паттерн ===
                .then(Commands.literal("encode_recipe")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .executes(ctx -> execEncode(ctx.getSource().getLevel().getRecipeManager()
                                                .byKey(ResourceLocationArgument.getId(ctx, "id")).orElse(null),
                                        ResourceLocationArgument.getId(ctx, "id"),
                                        false, 200,
                                        ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("subs", BoolArgumentType.bool())
                                        .executes(ctx -> execEncode(ctx.getSource().getLevel().getRecipeManager()
                                                        .byKey(ResourceLocationArgument.getId(ctx, "id")).orElse(null),
                                                ResourceLocationArgument.getId(ctx, "id"),
                                                BoolArgumentType.getBool(ctx, "subs"),
                                                200,
                                                ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("time", IntegerArgumentType.integer(1, 20000))
                                                .executes(ctx -> execEncode(ctx.getSource().getLevel().getRecipeManager()
                                                                .byKey(ResourceLocationArgument.getId(ctx, "id")).orElse(null),
                                                        ResourceLocationArgument.getId(ctx, "id"),
                                                        BoolArgumentType.getBool(ctx, "subs"),
                                                        IntegerArgumentType.getInteger(ctx, "time"),
                                                        ctx.getSource().getPlayerOrException()))
                                        )
                                )
                        )
                )

                // === Новая универсальная команда: выдать закодированный паттерн по id ===
                .then(Commands.literal("give_encoded_pattern")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .executes(ctx -> giveEncodedPattern(
                                        ResourceLocationArgument.getId(ctx, "id"),
                                        false, 200, ctx.getSource()))
                                .then(Commands.argument("subs", BoolArgumentType.bool())
                                        .executes(ctx -> giveEncodedPattern(
                                                ResourceLocationArgument.getId(ctx, "id"),
                                                BoolArgumentType.getBool(ctx, "subs"),
                                                200, ctx.getSource()))
                                        .then(Commands.argument("time", IntegerArgumentType.integer(1, 20000))
                                                .executes(ctx -> giveEncodedPattern(
                                                        ResourceLocationArgument.getId(ctx, "id"),
                                                        BoolArgumentType.getBool(ctx, "subs"),
                                                        IntegerArgumentType.getInteger(ctx, "time"),
                                                        ctx.getSource()))
                                        )
                                )
                        )
                )

                // === Шорткат: Ultimate Singularity (как на скрине) ===
                .then(Commands.literal("give_ultimate_singularity")
                        .executes(ctx -> giveEncodedPattern(
                                new ResourceLocation("extendedcrafting", "ultimate_singularity"),
                                false, 200, ctx.getSource()))
                        .then(Commands.argument("subs", BoolArgumentType.bool())
                                .executes(ctx -> giveEncodedPattern(
                                        new ResourceLocation("extendedcrafting", "ultimate_singularity"),
                                        BoolArgumentType.getBool(ctx, "subs"),
                                        200, ctx.getSource()))
                                .then(Commands.argument("time", IntegerArgumentType.integer(1, 20000))
                                        .executes(ctx -> giveEncodedPattern(
                                                new ResourceLocation("extendedcrafting", "ultimate_singularity"),
                                                BoolArgumentType.getBool(ctx, "subs"),
                                                IntegerArgumentType.getInteger(ctx, "time"),
                                                ctx.getSource()))
                                )
                        )
                )
        );
    }

    /* ===== Хелпер: выдать закодированный паттерн по id рецепта ===== */
    private static int giveEncodedPattern(ResourceLocation id, boolean substitutes, int time,
                                          net.minecraft.commands.CommandSourceStack src) {
        var level = src.getLevel();
        var player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("[CCApplied] Only players can receive items."));
            return 0;
        }

        var recipeOpt = level.getRecipeManager().byKey(id);
        if (recipeOpt.isEmpty()) {
            src.sendFailure(Component.literal("[CCApplied] Recipe not found: " + id));
            return 0;
        }

        var tag = com.gasai.ccapplied.common.util.RecipeEncodingUtil
                .encodeRecipeToTag(id, recipeOpt.get(), level, substitutes, time);

        ItemStack pattern = new ItemStack(ModItems.ENCODED_EXTREME_PATTERN.get());
        com.gasai.ccapplied.common.item.ExtremePatternEncodedItem.writePatternData(pattern, tag);

        if (!player.getInventory().add(pattern)) {
            Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), pattern);
        }

        src.sendSuccess(
                () -> Component.literal("[CCApplied] Encoded pattern for " + id +
                        " (subs=" + substitutes + ", time=" + time + "t)"),
                false
        );
        return 1;
    }
}