package com.TheChaYe.rotp_yellowtemperance.network.packets.client;

import com.TheChaYe.rotp_yellowtemperance.client.DisguiseRenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncOtherPlayerPhysicsPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    private final UUID playerUUID;
    private final float eyeHeightScale;
    private final float hitboxWidthScale;
    private final float hitboxHeightScale;

    public SyncOtherPlayerPhysicsPacket(UUID playerUUID, float eyeHeightScale, float hitboxWidthScale, float hitboxHeightScale) {
        this.playerUUID = playerUUID;
        this.eyeHeightScale = eyeHeightScale;
        this.hitboxWidthScale = hitboxWidthScale;
        this.hitboxHeightScale = hitboxHeightScale;
    }

    public static void encode(SyncOtherPlayerPhysicsPacket msg, PacketBuffer buf) {
        buf.writeUUID(msg.playerUUID);
        buf.writeFloat(msg.eyeHeightScale);
        buf.writeFloat(msg.hitboxWidthScale);
        buf.writeFloat(msg.hitboxHeightScale);
    }

    public static SyncOtherPlayerPhysicsPacket decode(PacketBuffer buf) {
        return new SyncOtherPlayerPhysicsPacket(
                buf.readUUID(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SyncOtherPlayerPhysicsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    PlayerEntity player = mc.level.getPlayerByUUID(msg.playerUUID);
                    if (player != null) {
                        // 客户端双重验证：确保缩放值在合理范围内
                        float validatedEyeHeightScale = MathHelper.clamp(msg.eyeHeightScale, 0.1f, 10.0f);
                        float validatedHitboxWidthScale = MathHelper.clamp(msg.hitboxWidthScale, 0.1f, 10.0f);
                        float validatedHitboxHeightScale = MathHelper.clamp(msg.hitboxHeightScale, 0.1f, 10.0f);

                        LOGGER.debug("Received other player physics sync for {} - Eye: {}, Width: {}, Height: {}",
                                player.getName().getString(), validatedEyeHeightScale, validatedHitboxWidthScale, validatedHitboxHeightScale);

                        // 应用眼部高度缩放
                        ScaleData eyeHeightData = ScaleTypes.EYE_HEIGHT.getScaleData(player);
                        eyeHeightData.setBaseScale(validatedEyeHeightScale);
                        eyeHeightData.setScaleTickDelay(0); // 立即应用
                        LOGGER.debug("Applied eye height scale: {} for player {}", validatedEyeHeightScale, player.getName().getString());

                        // 应用碰撞箱宽度缩放
                        ScaleData hitboxWidthData = ScaleTypes.HITBOX_WIDTH.getScaleData(player);
                        hitboxWidthData.setBaseScale(validatedHitboxWidthScale);
                        hitboxWidthData.setScaleTickDelay(0); // 立即应用
                        LOGGER.debug("Applied hitbox width scale: {} for player {}", validatedHitboxWidthScale, player.getName().getString());

                        // 应用碰撞箱高度缩放
                        ScaleData hitboxHeightData = ScaleTypes.HITBOX_HEIGHT.getScaleData(player);
                        hitboxHeightData.setBaseScale(validatedHitboxHeightScale);
                        hitboxHeightData.setScaleTickDelay(0); // 立即应用
                        LOGGER.debug("Applied hitbox height scale: {} for player {}", validatedHitboxHeightScale, player.getName().getString());

                        // 更新玩家边界框
                        EntitySize originalSize = player.getType().getDimensions();
                        EntitySize newSize = new EntitySize(
                                originalSize.width * validatedHitboxWidthScale,
                                originalSize.height * validatedHitboxHeightScale,
                                originalSize.fixed);
                        player.setBoundingBox(newSize.makeBoundingBox(player.position()));

                        LOGGER.debug("Updated bounding box for player {}", player.getName().getString());

                        // 更新伪装渲染处理器中的缓存
                        DisguiseRenderHandler.updatePhysicsCache(player, validatedEyeHeightScale, validatedHitboxWidthScale, validatedHitboxHeightScale);
                    } else {
                        LOGGER.debug("Player with UUID {} not found for physics sync", msg.playerUUID);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error applying other player physics sync: {}", e.getMessage());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}