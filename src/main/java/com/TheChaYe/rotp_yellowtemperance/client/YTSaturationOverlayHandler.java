package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 黄色节制饱和度覆盖处理器 / Yellow Temperance Saturation Overlay Handler
 * 处理与AppleSkin模组的饱和度显示集成
 */
@Mod.EventBusSubscriber(modid = "rotp_yellowtemperance", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class YTSaturationOverlayHandler {

    /**
     * 客户端设置事件 / Client setup event
     * 初始化AppleSkin集成功能
     */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // 检查AppleSkin是否加载且集成功能已启用 / Check if AppleSkin is loaded and integration is enabled
        if (ModList.get().isLoaded("appleskin") && YellowTemperanceConfig.enableAppleSkinIntegration.get()) {
            try {
                Class<?> integrationClass = Class.forName("com.TheChaYe.rotp_yellowtemperance.client.AppleSkinIntegration");
                // 直接注册类，无需创建实例
                MinecraftForge.EVENT_BUS.register(integrationClass);
            } catch (ClassNotFoundException e) {
//                System.err.println("[YT] AppleSkin integration class not found: " + e.getMessage());
            }
        }
    }
}