package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.init.InitStands;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.helpers.FoodHelper;


/**
 * AppleSkin集成处理 / AppleSkin Integration Handler
 * 处理与AppleSkin模组的兼容性，自定义饥饿值和饱和度显示
 */
public class AppleSkinIntegration {

    /** 黄色节制饱和度纹理路径 / Yellow Temperance saturation texture path */
    private static final ResourceLocation YT_SATURATION_TEXTURE =
            new ResourceLocation("rotp_yellowtemperance", "textures/appleskin/icons.png");
    /** 黄色节制饥饿值纹理路径 / Yellow Temperance hunger texture path */
    private static final ResourceLocation YT_HUNGER_TEXTURE =
            new ResourceLocation("rotp_yellowtemperance", "textures/gui/yt_energy.png");

    /** AppleSkin模组是否可用 / Whether AppleSkin mod is available */
    private static final boolean APPLE_SKIN_AVAILABLE = ModList.get().isLoaded("appleskin");

    /** 闪烁透明度变量 / Flash alpha variables */
    private static float flashAlpha = 0f;
    private static float unclampedFlashAlpha = 0f;
    private static byte alphaDir = 1;

    /**
     * 客户端tick事件处理 / Client tick event handler
     * 处理食物闪烁效果的计算
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 只在AppleSkin存在时运行 / Only run when AppleSkin is available
        if (!APPLE_SKIN_AVAILABLE) return;

        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;

        // 检查玩家主手是否持有可食用物品 / Check if player is holding edible item in main hand
        ItemStack mainHand = player.getMainHandItem();
        boolean hasFood = !mainHand.isEmpty() && FoodHelper.canConsume(mainHand, player);

        // 如果没拿食物，复位闪烁变量 / Reset flash variables if no food held
        if (!hasFood) {
            unclampedFlashAlpha = 0f;
            flashAlpha = 0f;
            alphaDir = 1;
            return;
        }

        // 计算闪烁效果 / Calculate flash effect
        unclampedFlashAlpha += alphaDir * 0.125f;
        if (unclampedFlashAlpha >= 1.5f) {
            alphaDir = -1;
        } else if (unclampedFlashAlpha <= -0.5f) {
            alphaDir = 1;
        }
        flashAlpha = Math.max(0F, Math.min(1F, unclampedFlashAlpha)) * 0.65f;
    }

    /**
     * 饱和度覆盖事件处理 / Saturation overlay event handler
     * 自定义饱和度显示效果
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSaturationOverlay(HUDOverlayEvent.Saturation event) {
        // 只在AppleSkin存在时运行 / Only run when AppleSkin is available
        if (!APPLE_SKIN_AVAILABLE) return;

        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;

        IStandPower standPower = IStandPower.getStandPowerOptional(player).orElse(null);
        boolean isActive = standPower != null &&
                standPower.getType() == InitStands.STAND_YELLOW_TEMPERANCE.getStandType() &&
                standPower.isActive();

        if (!isActive) return;

        event.setCanceled(true);
        renderCustomSaturation(event.saturationLevel, 0, event.x, event.y, event.matrixStack, mc, 1.0f);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHungerRestoredOverlay(HUDOverlayEvent.HungerRestored event) {
        // 只在AppleSkin存在时运行
        if (!APPLE_SKIN_AVAILABLE) return;

        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;

        IStandPower standPower = IStandPower.getStandPowerOptional(player).orElse(null);
        boolean isActive = standPower != null &&
                standPower.getType() == InitStands.STAND_YELLOW_TEMPERANCE.getStandType() &&
                standPower.isActive();

        if (!isActive) return;

        event.setCanceled(true);

        float foodSaturationIncrement = event.foodValues.getSaturationIncrement();
        float currentSaturation = player.getFoodData().getSaturationLevel();
        int foodHunger = event.foodValues.hunger;
        int newFoodValue = event.currentFoodLevel + foodHunger;
        float newSaturationValue = currentSaturation + foodSaturationIncrement;
        float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - currentSaturation : foodSaturationIncrement;

        if (saturationGained > 0) {
            renderCustomSaturation(currentSaturation, saturationGained, event.x, event.y, event.matrixStack, mc, flashAlpha);
        }

        if (foodHunger > 0) {
            renderCustomHungerOverlay(foodHunger, event.currentFoodLevel, event.x, event.y, event.matrixStack, mc,
                    FoodHelper.isRotten(event.itemStack), flashAlpha);
        }
    }

    // 以下两个渲染方法与您的旧版本完全一致（无改动）
    private static void renderCustomSaturation(float saturationLevel, float saturationGained, int right, int top, MatrixStack matrixStack, Minecraft mc, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bind(YT_SATURATION_TEXTURE);

        float modifiedSaturation;
        int startSaturationBar;

        if (saturationGained > 0) {
            modifiedSaturation = Math.max(0, Math.min(saturationLevel + saturationGained, 20));
            startSaturationBar = (int) Math.max(saturationLevel / 2.0F, 0);
        } else {
            modifiedSaturation = Math.max(0, Math.min(saturationLevel, 20));
            startSaturationBar = 0;
        }

        int endSaturationBar = (int) Math.ceil(modifiedSaturation / 2.0F);
        int iconSize = 9;

        for (int i = startSaturationBar; i < endSaturationBar; ++i) {
            int x = right - i * 8 - 9;
            int y = top;
            float effectiveSaturationOfBar = (modifiedSaturation / 2.0F) - i;
            int u;
            if (effectiveSaturationOfBar >= 1)
                u = 3 * iconSize;
            else if (effectiveSaturationOfBar > .5)
                u = 2 * iconSize;
            else if (effectiveSaturationOfBar > .25)
                u = 1 * iconSize;
            else
                u = 0;

            mc.gui.blit(matrixStack, x, y, u, 0, iconSize, iconSize);
        }

        mc.getTextureManager().bind(AbstractGui.GUI_ICONS_LOCATION);
    }

    private static void renderCustomHungerOverlay(int hungerRestored, int foodLevel, int right, int top, MatrixStack matrixStack, Minecraft mc, boolean useRottenTextures, float alpha) {
        if (hungerRestored <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bind(YT_HUNGER_TEXTURE);

        int modifiedFood = Math.max(0, Math.min(20, foodLevel + hungerRestored));
        int startFoodBars = Math.max(0, foodLevel / 2);
        int endFoodBars = (int) Math.ceil(modifiedFood / 2.0F);
        int iconStartOffset = 16;
        int iconSize = 9;

        for (int i = startFoodBars; i < endFoodBars; ++i) {
            int x = right - i * 8 - 9;
            int y = top;

            int v = 3 * iconSize;
            int u = iconStartOffset + 4 * iconSize;
            int ub = iconStartOffset;

            if (useRottenTextures) {
                u += 4 * iconSize;
                ub += 11 * iconSize;
            }

            if (i * 2 + 1 == modifiedFood)
                u += 1 * iconSize;

            mc.gui.blit(matrixStack, x, y, u, v, iconSize, iconSize);
        }

        mc.getTextureManager().bind(AbstractGui.GUI_ICONS_LOCATION);
    }
}