package com.TheChaYe.rotp_yellowtemperance.mixin;

import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * 第一人称手臂模型（粗/细手臂）跟随伪装目标。
 *
 * <p>原版第一人称手臂用的是"本地玩家自己皮肤对应的 PlayerRenderer"，所以伪装成
 * slim 皮肤时手臂几何仍是本地玩家的粗手臂，贴图会被拉伸。这里在
 * {@code renderPlayerArm} 取渲染器时按伪装目标的 slim 标记换成 default/slim 渲染器。</p>
 */
@Mixin(FirstPersonRenderer.class)
public abstract class FirstPersonRendererMixin {

    @Redirect(method = "renderPlayerArm",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRendererManager;getRenderer(Lnet/minecraft/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;"))
    private EntityRenderer<?> yt$disguiseFirstPersonRenderer(EntityRendererManager rendererManager, Entity entity) {
        EntityRenderer<?> original = rendererManager.getRenderer(entity);
        try {
            if (entity instanceof AbstractClientPlayerEntity && entity == Minecraft.getInstance().player) {
                if (ClientDisguiseCache.shouldRenderDisguise((AbstractClientPlayerEntity) entity)) {
                    Map<String, PlayerRenderer> skinMap = rendererManager.getSkinMap();
                    String modelType = ClientDisguiseCache.isSlimSkinModel(entity.getUUID()) ? "slim" : "default";
                    PlayerRenderer disguiseRenderer = skinMap.get(modelType);
                    if (disguiseRenderer != null) {
                        return disguiseRenderer;
                    }
                }
            }
        } catch (Throwable ignored) {
            // 出错时退回原渲染器，绝对不能影响第一人称渲染
        }
        return original;
    }
}