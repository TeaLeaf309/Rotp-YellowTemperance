package com.TheChaYe.rotp_yellowtemperance.action.Disguise;

import com.TheChaYe.rotp_yellowtemperance.client.ui.StandDisguiseChoiceUI;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.YellowTemperanceEntity;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class DisguiseStand extends StandEntityAction {
    public DisguiseStand(Builder builder) {
        super(builder);
    }

    @Override
    public void standPerform(World world, StandEntity stand, IStandPower userPower, StandEntityTask task) {
        final AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity)
                stand;
        //黄色节制在饱食度为零时无法使用此技能
        LivingEntity user = userPower.getUser();
        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            int foodLevel = player.getFoodData().getFoodLevel();
            if (stand instanceof YellowTemperanceEntity && foodLevel <= 0) {
                // 向玩家发送状态消息 / Send status message to player
                IFormattableTextComponent message = new TranslationTextComponent(
                        "action.rotp_yellowtemperance.no_food"
                );
                player.displayClientMessage(message, true);
                return;
            }
        }
        if (world.isClientSide()) StandDisguiseChoiceUI.openUI(Minecraft.getInstance());
    }
}
