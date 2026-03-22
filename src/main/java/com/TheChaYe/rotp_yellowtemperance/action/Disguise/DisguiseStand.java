package com.TheChaYe.rotp_yellowtemperance.action.Disguise;

import com.TheChaYe.rotp_yellowtemperance.client.ui.StandDisguiseChoiceUI;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

public class DisguiseStand extends StandEntityAction {
    public DisguiseStand(Builder builder) {
        super(builder);
    }

    @Override
    public void standPerform(World world, StandEntity stand, IStandPower userPower, StandEntityTask task) {
        final AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity)
                stand;
        if (world.isClientSide()) StandDisguiseChoiceUI.openUI(Minecraft.getInstance());
    }
}
