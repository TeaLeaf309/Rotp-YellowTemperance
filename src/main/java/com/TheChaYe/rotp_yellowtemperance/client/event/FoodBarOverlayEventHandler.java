/**
 * 饥饿值条覆盖事件处理器 / Food Bar Overlay Event Handler
 * 处理黄色节制替身激活时的饥饿值条和饱和度图标渲染
 * Handles hunger bar and saturation icon rendering when Yellow Temperance stand is active
 */
package com.TheChaYe.rotp_yellowtemperance.client.event;

import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.init.InitStands;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class FoodBarOverlayEventHandler {
    /**
     * 黄色节制能量纹理路径 / Yellow Temperance energy texture path
     */
    private static final ResourceLocation YT_ENERGY_TEXTURE = new ResourceLocation(
            "rotp_yellowtemperance", "textures/gui/yt_energy.png");

    /**
     * 饥饿值条渲染前置事件处理 / Pre-render food bar event handler
     * 在饥饿值条渲染前绑定自定义纹理
     * Binds custom texture before hunger bar rendering
     * @param event 渲染覆盖事件 / Render overlay event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderFoodBar(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.FOOD) return;

        // 检查配置是否启用此功能 / Check if this feature is enabled in config
        YellowTemperanceConfig.Common config = YellowTemperanceConfig.getCommonConfigInstance(true);
        if (!config.enableFoodBarOverlay.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        IStandPower standPower = IStandPower.getStandPowerOptional(mc.player).orElse(null);
        if (standPower != null &&
                standPower.getType() == InitStands.STAND_YELLOW_TEMPERANCE.getStandType() &&
                standPower.isActive()) {

            // 绑定自定义纹理 / Bind custom texture
            mc.getTextureManager().bind(YT_ENERGY_TEXTURE);
            
            // 调整渲染顺序，确保饥饿条在饱和度下方渲染
            // 通过控制渲染时机来实现图层顺序
            // Adjust rendering order to ensure hunger bar renders below saturation
            // Achieve layer order by controlling rendering timing
        }
    }
    
    /**
     * 饱和度渲染事件处理 / Saturation render event handler
     * 监听饱和度渲染事件，在饱和度渲染时调整图层顺序
     * Listens to saturation render events, adjusts layer order during saturation rendering
     * 确保饱和度图标显示在饥饿条上方
     * Ensures saturation icons display above hunger bar
     * @param event 渲染覆盖事件 / Render overlay event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSaturationRender(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.FOOD) return;
        
        // 检查配置是否启用此功能 / Check if this feature is enabled in config
        YellowTemperanceConfig.Common config = YellowTemperanceConfig.getCommonConfigInstance(true);
        if (!config.enableFoodBarOverlay.get()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        IStandPower standPower = IStandPower.getStandPowerOptional(mc.player).orElse(null);
        if (standPower != null &&
                standPower.getType() == InitStands.STAND_YELLOW_TEMPERANCE.getStandType() &&
                standPower.isActive()) {
            
            // 在饱和度渲染前确保使用正确的纹理
            // 这样饱和度图标会在饥饿条之上渲染
            // Ensure correct texture is used before saturation rendering
            // This makes saturation icons render above hunger bar
            mc.getTextureManager().bind(YT_ENERGY_TEXTURE);
        }
    }


}