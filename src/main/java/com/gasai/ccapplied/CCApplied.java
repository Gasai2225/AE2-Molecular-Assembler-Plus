package com.gasai.ccapplied;

import com.gasai.ccapplied.common.core.PatternInit;
import com.gasai.ccapplied.common.item.ExtremePatternItem;
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
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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
        PatternInit.registerDecoders();

        // Главный класс содержит слушатели — регистрируемся на общую шину Forge
        MinecraftForge.EVENT_BUS.register(this);
    }

    /* ================= Команда кодирования 9x9-паттерна ================= */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        var dispatcher = e.getDispatcher();

        dispatcher.register(Commands.literal("ccapplied")
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
        );
    }



    private int execEncode(Recipe<?> recipe, ResourceLocation id, boolean substitutes, int time, net.minecraft.server.level.ServerPlayer player) {
        if (recipe == null) {
            player.displayClientMessage(Component.literal("[CCApplied] Recipe not found: " + id), false);
            return 0;
        }

        var tag = RecipeEncodingUtil.encodeRecipeToTag(id, recipe, player.level(), substitutes, time);
        ItemStack pattern = new ItemStack(ModItems.EXTREME_PATTERN.get());
        ExtremePatternItem.writePatternData(pattern, tag);

        // Пытаемся положить в инвентарь, иначе — дроп у игрока
        if (!player.getInventory().add(pattern)) {
            Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), pattern);
        }

        player.displayClientMessage(Component.literal("[CCApplied] Encoded pattern for " + id +
                " (subs=" + substitutes + ", time=" + time + "t)"), false);
        return 1;
    }
}
