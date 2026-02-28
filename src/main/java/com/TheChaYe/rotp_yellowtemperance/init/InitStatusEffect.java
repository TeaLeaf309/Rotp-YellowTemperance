package com.TheChaYe.rotp_yellowtemperance.init;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.github.standobyte.jojo.init.ModStatusEffects;
import net.minecraftforge.fml.common.Mod;

/**
 * 状态效果初始化 / Status Effects Initialization
 * 管理状态效果的后期注册和追踪设置
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class InitStatusEffect {
    /**
     * 效果注册后的处理 / Post-effects registration processing
     * 设置需要追踪的状态效果
     */
    public static void afterEffectsRegister() {
        // 引用InitEffects中注册的效果 / Reference effects registered in InitEffects
        ModStatusEffects.setEffectAsTracked(
                InitEffects.YELLOW_TEMPERANCE_EROSION.get()
        );
    }
}