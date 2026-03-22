package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.network.ModNetwork;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.SyncOtherPlayerPhysicsPacket;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 用于同步玩家物理属性的网络数据包
 * 包括眼睛高度、碰撞箱宽度和高度的缩放比例
 */
public class SyncPlayerPhysicsPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    // 玩家UUID，用于验证数据包来源
    private final UUID playerUUID;
    // 眼睛高度缩放比例
    private final float eyeHeightScale;
    // 碰撞箱宽度缩放比例
    private final float hitboxWidthScale;
    // 碰撞箱高度缩放比例
    private final float hitboxHeightScale;

    /**
     * 构造函数
     *
     * @param playerUUID        玩家UUID
     * @param eyeHeightScale    眼睛高度缩放比例
     * @param hitboxWidthScale  碰撞箱宽度缩放比例
     * @param hitboxHeightScale 碰撞箱高度缩放比例
     */
    public SyncPlayerPhysicsPacket(UUID playerUUID, float eyeHeightScale, float hitboxWidthScale, float hitboxHeightScale) {
        this.playerUUID = playerUUID;
        this.eyeHeightScale = eyeHeightScale;
        this.hitboxWidthScale = hitboxWidthScale;
        this.hitboxHeightScale = hitboxHeightScale;
    }

    /**
     * 编码数据包，将数据写入缓冲区
     *
     * @param msg 数据包实例
     * @param buf 数据包缓冲区
     */
    public static void encode(SyncPlayerPhysicsPacket msg, PacketBuffer buf) {
        buf.writeUUID(msg.playerUUID);
        buf.writeFloat(msg.eyeHeightScale);
        buf.writeFloat(msg.hitboxWidthScale);
        buf.writeFloat(msg.hitboxHeightScale);
    }

    /**
     * 解码数据包，从缓冲区读取数据
     *
     * @param buf 数据包缓冲区
     * @return 解码后的数据包实例
     */
    public static SyncPlayerPhysicsPacket decode(PacketBuffer buf) {
        return new SyncPlayerPhysicsPacket(
                buf.readUUID(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    /**
     * 处理接收到的数据包
     *
     * @param msg 数据包实例
     * @param ctx 网络上下文
     */
    public static void handle(SyncPlayerPhysicsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.getUUID().equals(msg.playerUUID)) {
                try {
                    // 服务端双重验证：确保缩放值在合理范围内
                    float validatedEyeHeightScale = MathHelper.clamp(msg.eyeHeightScale, 0.1f, 10.0f);
                    float validatedHitboxWidthScale = MathHelper.clamp(msg.hitboxWidthScale, 0.1f, 10.0f);
                    float validatedHitboxHeightScale = MathHelper.clamp(msg.hitboxHeightScale, 0.1f, 10.0f);

                    LOGGER.debug("Received physics sync packet for player {} - Eye: {}, Width: {}, Height: {}",
                            player.getName().getString(), validatedEyeHeightScale, validatedHitboxWidthScale, validatedHitboxHeightScale);

                    // 应用眼部高度缩放
                    ScaleData eyeHeightData = ScaleTypes.EYE_HEIGHT.getScaleData(player);
                    eyeHeightData.setBaseScale(validatedEyeHeightScale);
                    eyeHeightData.markForSync(true);
                    eyeHeightData.setScaleTickDelay(0); // 立即应用
                    LOGGER.debug("Applied eye height scale: {} for player {}", validatedEyeHeightScale, player.getName().getString());

                    // 应用碰撞箱宽度缩放
                    ScaleData hitboxWidthData = ScaleTypes.HITBOX_WIDTH.getScaleData(player);
                    hitboxWidthData.setBaseScale(validatedHitboxWidthScale);
                    hitboxWidthData.markForSync(true);
                    hitboxWidthData.setScaleTickDelay(0); // 立即应用
                    LOGGER.debug("Applied hitbox width scale: {} for player {}", validatedHitboxWidthScale, player.getName().getString());

                    // 应用碰撞箱高度缩放
                    ScaleData hitboxHeightData = ScaleTypes.HITBOX_HEIGHT.getScaleData(player);
                    hitboxHeightData.setBaseScale(validatedHitboxHeightScale);
                    hitboxHeightData.markForSync(true);
                    hitboxHeightData.setScaleTickDelay(0); // 立即应用
                    LOGGER.debug("Applied hitbox height scale: {} for player {}", validatedHitboxHeightScale, player.getName().getString());

                    // 获取玩家原始尺寸
                    EntitySize originalSize = player.getType().getDimensions();
                    // 计算新的尺寸
                    EntitySize newSize = new EntitySize(
                            originalSize.width * validatedHitboxWidthScale,
                            originalSize.height * validatedHitboxHeightScale,
                            originalSize.fixed);
                    // 强制更新玩家边界框
                    player.setBoundingBox(newSize.makeBoundingBox(player.position()));

                    LOGGER.debug("Updated bounding box for player {}", player.getName().getString());

                    // 广播给所有跟踪该玩家的客户端
                    ModNetwork.sendToAllTracking(
                            new SyncOtherPlayerPhysicsPacket(player.getUUID(), validatedEyeHeightScale, validatedHitboxWidthScale, validatedHitboxHeightScale),
                            player
                    );
                } catch (Exception e) {
                    LOGGER.error("Error applying physics sync for player {}: {}", player.getName().getString(), e.getMessage());
                }
            } else {
                LOGGER.warn("Received physics sync packet from invalid player or mismatched UUID");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}