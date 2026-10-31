package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import com.TheChaYe.rotp_yellowtemperance.client.DisguiseRenderHandler;
import com.TheChaYe.rotp_yellowtemperance.client.render.DisguiseLayer;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端伪装事件处理器 / Client-Side Disguise Event Handler
 * 专门处理仅在客户端执行的伪装相关事件，如实体尺寸调整、玩家模型可见性切换。
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
            if (DisguiseRenderHandler.CURRENT_DISGUISE_TYPES.containsKey(player.getUUID())
                    && DisguiseRenderHandler.isUserDisguised(player)) {
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

    /**
     * 主模型渲染前决定"画原玩家"还是"留给伪装层画"。
     *
     * <p>原实现只在 DisguiseLayer 里偷偷改模型部件的 visible，而 Layer 是在主模型
     * 之后才渲染的：一旦某一帧皮肤没准备好直接 return，上一帧设下的隐藏状态就会
     * 保留下来 —— 玩家会隐身。这里把可见性判断提前到主模型渲染之前，并且以
     * "伪装皮肤是否真的可用"为唯一依据，从结构上消除隐身窗口。</p>
     */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        PlayerEntity player = event.getPlayer();
        if (!(player instanceof AbstractClientPlayerEntity)) {
            return;
        }
        PlayerRenderer renderer = event.getRenderer();
        if (renderer == null) {
            return;
        }
        PlayerModel<?> model = renderer.getModel();
        if (ClientDisguiseCache.shouldRenderDisguise(player)) {
            // 主模型不画，交给 DisguiseLayer 画伪装模型
            DisguiseLayer.hideOriginalModelParts(player, model);
        } else {
            // 没伪装，或伪装皮肤还没准备好：照常画原玩家（绝不隐身）
            // 玩家伪装数据不再需要单独清脏，这里每帧都会把状态纠正回来
            DisguiseLayer.restoreOriginalModelParts(player, model);
        }
    }

}