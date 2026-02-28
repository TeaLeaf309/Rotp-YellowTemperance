package com.TheChaYe.rotp_yellowtemperance.effects;

import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;

/**
 * 黄色节制保护效果 / Yellow Temperance Protection Effect
 * 正面效果，提供伤害减免保护
 */
public class YellowTemperanceProtectionEffect extends Effect {

    /** 橙黄色颜色值 / Orange-yellow color value */
    public YellowTemperanceProtectionEffect() {
        super(EffectType.BENEFICIAL, 0xFFA500);
    }

    /**
     * 计算保护因子 / Calculate protection factor
     * 根据效果等级计算伤害减免比例
     */
    public static float getProtectionFactor(int amplifier) {
        // 获取配置值 / Get config values
        YellowTemperanceConfig.Common config = YellowTemperanceConfig.getCommonConfigInstance(false);
        double reductionPerLevel = config.protectionReductionPerLevel.get();
        double maxReduction = config.protectionMaxReduction.get();
        
        // 每级减免比例 / Damage reduction per level
        float reduction = (float) (amplifier * reductionPerLevel);
        // 最大减免比例上限 / Maximum reduction cap
        return (float) Math.min(reduction, maxReduction);
    }

    /**
     * 获取治疗物品列表 / Get curative items list
     * 返回空列表表示该效果不可被常规物品治愈
     */
    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList(); // 不可被牛奶、金苹果等常规物品治愈，防止通过药水云传播 / Cannot be cured by milk, golden apple or other items to prevent spread through potion clouds
    }

    /**
     * 判断是否执行效果tick / Determine if effect tick should execute
     * 保护效果不需要每tick执行
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // 不需要每tick执行，只在受伤时计算 / No need to execute every tick, only calculate when taking damage
    }

    /**
     * 判断效果是否可见 / Determine if effect is visible
     * 客户端渲染相关方法
     */
    @OnlyIn(Dist.CLIENT)
    public boolean isVisible() {
        return true;
    }

    /**
     * 判断是否应在HUD中渲染 / Determine if should render in HUD
     * 控制效果在状态栏中的显示
     */
    @Override
    public boolean shouldRenderHUD(EffectInstance effect) {
        return true;
    }

    /**
     * 判断是否应渲染效果 / Determine if effect should render
     * 控制效果的视觉渲染
     */
    @Override
    public boolean shouldRender(EffectInstance effect) {
        return true;
    }

    /**
     * 判断是否为有益效果 / Determine if beneficial effect
     * 防止通过爆炸等方式传播效果
     */
    @Override
    public boolean isBeneficial() {
        return true;
    }
}