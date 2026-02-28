package com.TheChaYe.rotp_yellowtemperance.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

/**
 * 黄色节制伤害源 / Yellow Temperance Damage Source
 * 自定义伤害源类型，用于黄色节制替身的特殊攻击机制
 * Custom damage source type for Yellow Temperance Stand's special attack mechanics
 */
public class YellowTemperanceDamageSource extends DamageSource {

    private Entity entity;

    /**
     * 构造函数 / Constructor
     * 创建基础的黄色节制伤害源
     * @param damageTypeIn 伤害类型标识 / Damage type identifier
     */
    public YellowTemperanceDamageSource(String damageTypeIn) {
        super(damageTypeIn);
    }

    /**
     * 带实体的构造函数 / Constructor with Entity
     * 创建带来源实体的黄色节制伤害源
     * @param damageTypeIn 伤害类型标识 / Damage type identifier
     * @param damageSourceEntityIn 伤害来源实体 / Damage source entity
     */
    public YellowTemperanceDamageSource(String damageTypeIn, @Nullable Entity damageSourceEntityIn) {
        super(damageTypeIn);
        this.entity = damageSourceEntityIn;
    }

    @Override
    public ITextComponent getLocalizedDeathMessage(net.minecraft.entity.LivingEntity entityLivingBaseIn) {
        net.minecraft.entity.LivingEntity attacker = entityLivingBaseIn.getKillCredit();
        String s = "death.attack." + this.msgId;
        String s1 = s + ".player";
        return attacker != null ?
                new TranslationTextComponent(s1, entityLivingBaseIn.getDisplayName(), attacker.getDisplayName()) :
                new TranslationTextComponent(s, entityLivingBaseIn.getDisplayName());
    }

    @Override
    public boolean isMagic() {
        return true; // 与魔法伤害一样的减免机制 / Same reduction mechanism as magic damage
    }

    @Nullable
    @Override
    public Entity getEntity() {
        return this.entity; // 返回伤害来源实体 / Return damage source entity
    }
}