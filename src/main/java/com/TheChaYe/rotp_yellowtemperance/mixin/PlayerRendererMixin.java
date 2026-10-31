package com.TheChaYe.rotp_yellowtemperance.mixin;

import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 第一人称手臂皮肤伪装。
 *
 * <p>原版 {@code PlayerRenderer.renderHand} 直接用 {@code player.getSkinTextureLocation()} 作为
 * 手臂/袖子的贴图，所以伪装层（只作用于第三人称模型）对第一人称手臂完全无效。
 * 这里在 renderHand 内部把取皮肤的两处调用重定向到伪装缓存：皮肤没准备好时返回原皮肤，
 * 不会把手臂画成透明/缺失纹理。</p>
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Redirect(method = "renderHand",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;getSkinTextureLocation()Lnet/minecraft/util/ResourceLocation;"))
    private ResourceLocation yt$disguiseFirstPersonSkin(AbstractClientPlayerEntity player) {
        // 以 NBT 伪装标记 + 皮肤可用为唯一依据：解除伪装后这里必须回到原皮肤
        if (!ClientDisguiseCache.shouldRenderDisguise(player)) {
            return player.getSkinTextureLocation();
        }
        ResourceLocation disguiseSkin = ClientDisguiseCache.getRenderableSkin(player.getUUID());
        return disguiseSkin != null ? disguiseSkin : player.getSkinTextureLocation();
    }
}