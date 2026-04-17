package com.TheChaYe.rotp_yellowtemperance.action;

import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.YellowTemperanceEntity;
import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandManifestation;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

/**
 * 黄色节制格挡能力 / Yellow Temperance Block Ability
 * 处理替身的格挡防御逻辑，提供保护效果并消耗耐力
 * Handles the stand's blocking defense logic, providing protective effects while consuming stamina
 */
public class YellowTemperanceBlock extends StandAction {

    /**
     * 构造函数 / Constructor
     *
     * @param builder 动作构建器 / Action builder
     */
    public YellowTemperanceBlock(Builder builder) {
        super(builder);
    }

    /**
     * 持续执行格挡逻辑 / Hold tick block logic
     *
     * @param world                 当前世界 / Current world
     * @param user                  用户实体 / User entity
     * @param userPower             用户替身能力 / User stand power
     * @param ticksHeld             持续时间 / Ticks held
     * @param target                目标 / Target
     * @param requirementsFulfilled 条件是否满足 / Whether requirements fulfilled
     */
    @Override
    protected void holdTick(World world, LivingEntity user, IStandPower userPower, int ticksHeld, ActionTarget target, boolean requirementsFulfilled) {
        if (requirementsFulfilled) {
            if (!world.isClientSide) {
                if (user != null) {
                    //黄色节制在饱食度为零时无法使用此技能
                    if (user instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) user;
                        int foodLevel = player.getFoodData().getFoodLevel();
                        IStandManifestation manifestation = userPower.getStandManifestation();
                        if (manifestation instanceof YellowTemperanceEntity && foodLevel <= 0) {
                            YellowTemperanceEntity stand = (YellowTemperanceEntity) manifestation;
                            // 向玩家发送状态消息 / Send status message to player
                            IFormattableTextComponent message = new TranslationTextComponent(
                                    "action.rotp_yellowtemperance.no_food"
                            );
                            player.displayClientMessage(message, true);
                            return;
                        }
                    }
                    // 获取配置值 / Get config values
                    int protectionLevel = YellowTemperanceConfig.blockProtectionLevel.get();

                    // 应用保护效果 / Apply protection effect
                    Effect customEffect = InitEffects.YELLOW_TEMPERANCE_PROTECTION.get();
                    user.addEffect(new EffectInstance(
                            customEffect,
                            2,
                            protectionLevel,
                            false,
                            false
                    ));

                    // 消耗耐力 / Consume stamina
                    float staminaCost = 10.0F; // Stamina cost per tick
                    if (!userPower.consumeStamina(staminaCost)) {
                        // 耐力不足时停止格挡 / Stop blocking when stamina is insufficient
                        userPower.stopHeldAction(false);
                    }
                }
            }
        }
    }
}