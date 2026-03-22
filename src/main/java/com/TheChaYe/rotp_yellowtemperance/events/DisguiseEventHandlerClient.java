package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.client.DisguiseRenderHandler;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端伪装事件处理器 / Client-Side Disguise Event Handler
 * 专门处理仅在客户端执行的伪装相关事件，如实体尺寸调整。
 * 此类被标记为 @Mod.EventBusSubscriber(Dist.CLIENT)，确保只在客户端加载，
 * 避免服务端因引用客户端专用类而崩溃。
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
public class DisguiseEventHandlerClient {

    /**
     * 实体尺寸事件处理 / Entity size event handler
     * 调整伪装玩家的碰撞箱尺寸以匹配伪装实体
     */
    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            // 检查玩家是否正在伪装 / Check if player is disguised
            if (DisguiseRenderHandler.CURRENT_DISGUISE_TYPES.containsKey(player.getUUID())) {
                // 获取伪装实体 / Get disguise entity
                LivingEntity disguiseEntity = DisguiseRenderHandler.PERSISTENT_DISGUISE_ENTITIES.get(player.getUUID());
                if (disguiseEntity != null) {
                    // 使用伪装实体的尺寸和眼高 / Use disguise entity's dimensions and eye height
                    EntitySize size = disguiseEntity.getDimensions(player.getPose());
                    event.setNewSize(size);
                    event.setNewEyeHeight(disguiseEntity.getEyeHeight(player.getPose()));
                }
            }
        }
    }
}
