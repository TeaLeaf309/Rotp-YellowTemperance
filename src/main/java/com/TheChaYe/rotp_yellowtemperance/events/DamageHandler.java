package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.effects.YellowTemperanceProtectionEffect;
import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 伤害事件处理器 / Damage Event Handler
 * 处理黄色节制保护效果的伤害减免逻辑
 */
@Mod.EventBusSubscriber
public class DamageHandler {

    /**
     * 生物受伤事件处理 / Living hurt event handler
     * 应用黄色节制保护效果的伤害减免
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        // 跳过免疫穿透的攻击 / Skip attacks that bypass invulnerability
        if (source.isBypassInvul()) {
            return;
        }

        // 排除溺水、饥饿、魔法和凋零伤害 / Exclude drowning, starvation, magic and wither damage
        if (source == DamageSource.DROWN ||
                source == DamageSource.STARVE ||
                source.isMagic() ||
                source == DamageSource.WITHER) {
            return;
        }

        LivingEntity entity = event.getEntityLiving();
        EffectInstance effect = entity.getEffect(InitEffects.YELLOW_TEMPERANCE_PROTECTION.get());

        if (effect != null) {
            int amplifier = effect.getAmplifier();
            float reduction = YellowTemperanceProtectionEffect.getProtectionFactor(amplifier);
            float newDamage = event.getAmount() * (1 - reduction);
            event.setAmount(newDamage);
        }
    }
}