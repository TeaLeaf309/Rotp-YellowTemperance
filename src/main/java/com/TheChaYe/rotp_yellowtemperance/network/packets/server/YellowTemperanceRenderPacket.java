package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.client.DisguiseRenderHandler;
import com.TheChaYe.rotp_yellowtemperance.entity.YellowTemperanceEntity;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.github.standobyte.jojo.network.packets.IModPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class YellowTemperanceRenderPacket {
    private final ResourceLocation playerDisguiseRes; // 可以为null
    private final int standId;
    private final float limbSwing;
    private final float limbSwingAmount;
    private final float headYaw;
    private final float headPitch;

    public YellowTemperanceRenderPacket(ResourceLocation playerDisguiseRes, int standId,
                                        float limbSwing, float limbSwingAmount,
                                        float headYaw, float headPitch) {
        this.playerDisguiseRes = playerDisguiseRes;
        this.standId = standId;
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.headYaw = headYaw;
        this.headPitch = headPitch;
    }

    public static class Handler implements IModPacketHandler<YellowTemperanceRenderPacket> {
        @Override
        public void encode(YellowTemperanceRenderPacket msg, PacketBuffer buf) {
            // 修复方法混淆：func_192572_a -> writeResourceLocation
            // 当playerDisguiseRes为null时，写入空字符串作为资源位置
            // 写入资源位置，如果为null则使用"minecraft:air"作为占位符
            buf.writeResourceLocation(msg.playerDisguiseRes != null ? msg.playerDisguiseRes : new ResourceLocation("minecraft", "air"));
            buf.writeInt(msg.standId);
            buf.writeFloat(msg.limbSwing);
            buf.writeFloat(msg.limbSwingAmount);
            buf.writeFloat(msg.headYaw);
            buf.writeFloat(msg.headPitch);
        }

        @Override
        public YellowTemperanceRenderPacket decode(PacketBuffer buf) {
            // 修复方法混淆：func_192575_l -> readResourceLocation
            // 读取资源位置，如果为空字符串则设置为null
            // 读取资源位置，如果为"minecraft:air"则设置为null
            ResourceLocation playerDisguiseRes = buf.readResourceLocation();
            if (playerDisguiseRes == null || "minecraft:air".equals(playerDisguiseRes.toString())) {
                playerDisguiseRes = null;
            }
            return new YellowTemperanceRenderPacket(
                    playerDisguiseRes,
                    buf.readInt(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            );
        }

        @Override
        public void handle(YellowTemperanceRenderPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        // 修复方法混淆：
                        // func_71410_x() -> getInstance()
                        // field_71441_e -> level
                        // func_73045_a() -> getEntity()
                        Entity entity = Minecraft.getInstance().level.getEntity(msg.standId);
                        if (entity instanceof YellowTemperanceEntity) {
                            YellowTemperanceEntity yellowTemperance = (YellowTemperanceEntity) entity;
                            updateDisguise(yellowTemperance, msg.playerDisguiseRes);

                            // 更新动画相关属性
                            yellowTemperance.clientLimbSwing = msg.limbSwing;
                            yellowTemperance.clientLimbSwingAmount = msg.limbSwingAmount;
                            yellowTemperance.yHeadRot = msg.headYaw;
                            yellowTemperance.xRot = msg.headPitch;

                            // 如果有伪装，确保客户端物理属性更新
                            if (msg.playerDisguiseRes != null) {
                                LivingEntity user = yellowTemperance.getUser();
                                if (user instanceof PlayerEntity) {
                                    EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(msg.playerDisguiseRes);
                                    if (entityType != null) {
                                        // 更新客户端伪装状态
                                        DisguiseRenderHandler.CURRENT_DISGUISE_TYPES.put(user.getUUID(), entityType);
                                        DisguiseRenderHandler.DISGUISE_REMOVAL_TIMERS.remove(user.getUUID());

                                        // 确保在下一帧更新物理属性，此时伪装实体已经创建
                                        Minecraft.getInstance().tell(() -> {
                                            DisguiseRenderHandler.updatePlayerPhysics((PlayerEntity) user);
                                        });
                                    }
                                }
                            }
                        }
                    });
                } else {
                    PlayerEntity player = ctx.get().getSender();
                    if (player != null) {
                        // 修复方法混淆：
                        // field_70170_p -> level
                        // func_73045_a() -> getEntity()
                        Entity stand = player.level.getEntity(msg.standId);
                        if (stand instanceof YellowTemperanceEntity) {
                            YellowTemperanceEntity yellowTemperance = (YellowTemperanceEntity) stand;
                            updateDisguise(yellowTemperance, msg.playerDisguiseRes);

                            // 更新动画相关属性
                            yellowTemperance.clientLimbSwing = msg.limbSwing;
                            yellowTemperance.clientLimbSwingAmount = msg.limbSwingAmount;
                            yellowTemperance.yHeadRot = msg.headYaw;
                            yellowTemperance.xRot = msg.headPitch;
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }

        private void updateDisguise(YellowTemperanceEntity entity, ResourceLocation disguiseRes) {
            if (disguiseRes != null && !disguiseRes.toString().isEmpty()) {
                EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(disguiseRes);
                if (entityType != null) {
                    entity.setUserDisguiseEntity(entityType);
                    entity.setUserDisguised(true);
                } else {
                    // 如果实体类型无效，清除伪装状态
                    clearDisguiseState(entity);
                }
            } else {
                // 当收到空伪装信息时，清除伪装状态
                clearDisguiseState(entity);
            }
        }

        private void clearDisguiseState(YellowTemperanceEntity entity) {
            // 清除伪装状态，并确保发送空资源位置
            entity.setUserDisguised(false);
            entity.setUserDisguiseEntity(null);
            entity.setDisguisedOnce(false);
            entity.setEntityForDisguise(null);

            // 如果是在服务器端，广播更新伪装状态
            if (!entity.level.isClientSide && entity.getId() != -1) {
                // 创建一个新的YellowTemperanceRenderPacket，其中playerDisguiseRes为null
                YellowTemperanceRenderPacket packet = new YellowTemperanceRenderPacket(
                        null, // playerDisguiseRes为null
                        entity.getId(),
                        entity.clientLimbSwing,
                        entity.clientLimbSwingAmount,
                        entity.yHeadRot,
                        entity.xRot
                );
                // 广播这个包给所有客户端
                PacketHandler.sendToClientsTrackingAndSelf(packet, entity);
            }
        }

        @Override
        public Class<YellowTemperanceRenderPacket> getPacketClass() {
            return YellowTemperanceRenderPacket.class;
        }
    }
}