package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端事件处理器 / Client Event Handler
 * 处理玩家渲染相关的客户端事件，控制伪装效果的显示
 */
@Mod.EventBusSubscriber(modid = "rotp_yellowtemperance", value = Dist.CLIENT)
public class ClientHandler {

    /**
     * 玩家渲染前事件 / Player render pre event
     * 在玩家渲染前隐藏原始模型部件以显示伪装效果
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        // 检查玩家是否有伪装标签，如果有则隐藏原始模型部分 / Check if player has disguise tag, hide original model parts if present
        PlayerEntity player = event.getPlayer();
        CompoundNBT nbt = player.getPersistentData();

        if (nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
//            RotPYellowTemperanceAddon.LOGGER.debug("ClientHandler hiding original model parts for player: {}", player.getName().getString());
            // 调用DisguiseLayer的方法来隐藏原始模型并调整手臂类型 / Call DisguiseLayer methods to hide original model and adjust arm types
            com.TheChaYe.rotp_yellowtemperance.client.render.DisguiseLayer.hideOriginalModelParts(
                    player, event.getRenderer().getModel());
        }
    }

    /**
     * 玩家渲染后事件 / Player render post event
     * 在玩家渲染后恢复原始模型部件的可见性
     */
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        // 恢复原始模型部分的可见性 / Restore visibility of original model parts
        PlayerEntity player = event.getPlayer();
        CompoundNBT nbt = player.getPersistentData();

        // 如果玩家有伪装标签，恢复原始模型部分的可见性 / If player has disguise tag, restore original model parts visibility
        if (nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
//            RotPYellowTemperanceAddon.LOGGER.debug("ClientHandler restoring original model parts for player: {}", player.getName().getString());
            // 调用DisguiseLayer的方法来恢复原始模型 / Call DisguiseLayer methods to restore original model
            com.TheChaYe.rotp_yellowtemperance.client.render.DisguiseLayer.restoreOriginalModelParts(
                    player, event.getRenderer().getModel());
        }
    }
}