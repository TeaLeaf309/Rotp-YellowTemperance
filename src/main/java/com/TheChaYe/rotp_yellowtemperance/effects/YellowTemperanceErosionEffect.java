package com.TheChaYe.rotp_yellowtemperance.effects;

import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import com.TheChaYe.rotp_yellowtemperance.util.YellowTemperanceDamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

import java.util.Collections;
import java.util.List;

/**
 * 黄色节制侵蚀效果 / Yellow Temperance Erosion Effect
 * 负面效果，会对受影响的实体造成持续伤害
 */
public class YellowTemperanceErosionEffect extends Effect {
    /**
     * 施加者UUID存储标签名 / Caster UUID storage tag name
     */
    public static final String CASTER_UUID_TAG = "YellowTemperanceCasterUUID";

    /**
     * 构造函数 / Constructor
     * 初始化黄色节制侵蚀效果，设置为负面效果类型
     */
    public YellowTemperanceErosionEffect() {
        // 设置为负面效果(HARMFUL)，使用暗黄色（RGB: 0x8B8000） / Set as harmful effect with dark yellow color (RGB: 0x8B8000)
        super(EffectType.HARMFUL, 0x8B8000);
    }

    /**
     * 获取治疗物品列表 / Get curative items list
     * 返回空列表表示该效果不可被常规物品治愈
     */
    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList(); // 不可被牛奶、金苹果等常规物品治愈 / Cannot be cured by milk, golden apple or other conventional items
    }

    /**
     * 应用效果tick / Apply effect tick
     * 每tick执行的侵蚀效果逻辑
     */
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 每tick执行的效果：如果有黄色节制的保护效果，则不造成伤害并且直接移除侵蚀效果 / Effect executed each tick: if protection effect exists, no damage and remove erosion effect
        if (!entity.level.isClientSide) {
            // 检查是否有黄色节制保护效果 / Check if yellow temperance protection effect exists
            if (entity.hasEffect(InitEffects.YELLOW_TEMPERANCE_PROTECTION.get())) {
                // 移除侵蚀效果 / Remove erosion effect
                entity.removeEffect(this);
                return;
            }

            // 获取配置值 / Get config values
            double baseDamage = YellowTemperanceConfig.erosionBaseDamage.get();
            double damagePerLevel = YellowTemperanceConfig.erosionDamagePerLevel.get();
            double maxDamage = YellowTemperanceConfig.erosionMaxDamage.get();

            // 计算伤害：基础伤害 + 每级伤害增量，但不超过上限 / Calculate damage: base damage + damage per level, but not exceeding cap
            float damage = (float) Math.min(baseDamage + amplifier * damagePerLevel, maxDamage);
            // 使用黄色节制侵蚀伤害类型 / Use yellow temperance erosion damage type
            entity.hurt(new YellowTemperanceDamageSource("yellow_temperance_erosion"), damage);
        }
    }

    /**
     * 判断是否执行效果tick / Determine if effect tick should execute
     * 控制效果执行的频率
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 固定每20 tick（1秒）执行一次效果 / Fixed execution every 20 ticks (1 second)
        return duration % 20 == 0;
    }

    /**
     * 移除属性修饰符 / Remove attribute modifiers
     * 效果结束时的清理逻辑
     */
    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeModifierManager attributes, int amplifier) {
        // 效果结束时不再造成额外伤害 / No additional damage when effect ends
        super.removeAttributeModifiers(entity, attributes, amplifier);
    }

    /**
     * 判断是否为有益效果 / Determine if beneficial effect
     * 防止通过爆炸等方式传播效果
     */
    @Override
    public boolean isBeneficial() {
        return false;
    }
}