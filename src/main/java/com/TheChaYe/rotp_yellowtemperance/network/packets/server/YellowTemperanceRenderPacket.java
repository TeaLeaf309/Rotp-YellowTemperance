package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.client.DisguiseRenderHandler;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
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
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class YellowTemperanceRenderPacket {
    private final ResourceLocation playerDisguiseRes; // 可以为 null
    private final int standId;
    private final float limbSwing;
    private final float limbSwingAmount;
    private final float headYaw;
    private final float headPitch;
    private final boolean clearUserDisguise; // 是否清除用户伪装
    private final boolean clearStandDisguise; // 是否清除替身伪装

    public YellowTemperanceRenderPacket(ResourceLocation playerDisguiseRes, int standId,
                                        float limbSwing, float limbSwingAmount,
                                        float headYaw, float headPitch) {
        this(playerDisguiseRes, standId, limbSwing, limbSwingAmount, headYaw, headPitch, false, false);
    }

    public YellowTemperanceRenderPacket(ResourceLocation playerDisguiseRes, int standId,
                                        float limbSwing, float limbSwingAmount,
                                        float headYaw, float headPitch,
                                        boolean clearUserDisguise, boolean clearStandDisguise) {
        this.playerDisguiseRes = playerDisguiseRes;
        this.standId = standId;
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.headYaw = headYaw;
        this.headPitch = headPitch;
        this.clearUserDisguise = clearUserDisguise;
        this.clearStandDisguise = clearStandDisguise;
    }

    public static class Handler implements IModPacketHandler<YellowTemperanceRenderPacket> {
        // 客户端专用方法
        @OnlyIn(Dist.CLIENT)
        private static void handleClient(YellowTemperanceRenderPacket msg) {
            Entity entity = Minecraft.getInstance().level.getEntity(msg.standId);
            if (entity instanceof AbstractDisguiseStandEntity) {
                AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity) entity;
                updateDisguise(disguiseStand, msg.playerDisguiseRes, msg);

//                disguiseStand.clientLimbSwing = msg.limbSwing;
//                disguiseStand.clientLimbSwingAmount = msg.limbSwingAmount;
//                disguiseStand.yHeadRot = msg.headYaw;
//                disguiseStand.xRot = msg.headPitch;

                if (msg.playerDisguiseRes != null) {
                    LivingEntity user = disguiseStand.getUser();
                    if (user instanceof PlayerEntity) {
                        EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(msg.playerDisguiseRes);
                        if (entityType != null) {
                            DisguiseRenderHandler.CURRENT_DISGUISE_TYPES.put(user.getUUID(), entityType);
                            DisguiseRenderHandler.DISGUISE_REMOVAL_TIMERS.remove(user.getUUID());

                            Minecraft.getInstance().tell(() -> {
                                DisguiseRenderHandler.updatePlayerPhysics((PlayerEntity) user);
                            });
                        }
                    }
                }
            }
        }

        // 服务端处理（原 else 分支）
        private static void handleServer(YellowTemperanceRenderPacket msg, PlayerEntity sender) {
            if (sender != null) {
                Entity stand = sender.level.getEntity(msg.standId);
                if (stand instanceof AbstractDisguiseStandEntity) {
                    AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity) stand;
                    updateDisguise(disguiseStand, msg.playerDisguiseRes, msg);

                    // 服务端不处理客户端专用的动画字段
//                     disguiseStand.clientLimbSwing = msg.limbSwing;
//                     disguiseStand.clientLimbSwingAmount = msg.limbSwingAmount;
//                     disguiseStand.yHeadRot = msg.headYaw;
//                     disguiseStand.xRot = msg.headPitch;
                }
            }
        }

        private static void updateDisguise(AbstractDisguiseStandEntity entity, ResourceLocation disguiseRes, YellowTemperanceRenderPacket msg) {
            if (disguiseRes != null && !disguiseRes.toString().isEmpty()) {
                EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(disguiseRes);
                if (entityType != null) {
                    entity.setUserDisguiseEntity(entityType);
                    entity.setUserDisguised(true);
                } else {
                    // 如果实体类型无效，清除用户伪装状态
                    clearUserDisguiseState(entity);
                }
            } else {
                // 当收到空伪装信息时，根据标志位清除对应的伪装
                if (msg.clearUserDisguise) {
                    clearUserDisguiseState(entity);
                }
                if (msg.clearStandDisguise) {
                    clearStandDisguiseState(entity);
                }
                // 如果没有指定清除哪种伪装，默认清除用户伪装（向后兼容）
                if (!msg.clearUserDisguise && !msg.clearStandDisguise) {
                    clearUserDisguiseState(entity);
                }
            }
        }

        private static void clearUserDisguiseState(AbstractDisguiseStandEntity entity) {
            // 只清除用户伪装状态
            entity.setUserDisguised(false);
            entity.setUserDisguiseEntity(null);

            // 如果是在服务器端，广播更新伪装状态
            if (!entity.level.isClientSide && entity.getId() != -1) {
                // 创建一个新的 YellowTemperanceRenderPacket，其中 playerDisguiseRes 为 null
                YellowTemperanceRenderPacket packet = new YellowTemperanceRenderPacket(
                        null, // playerDisguiseRes 为 null
                        entity.getId(),
                        entity.clientLimbSwing,
                        entity.clientLimbSwingAmount,
                        entity.yHeadRot,
                        entity.xRot,
                        true, // clearUserDisguise
                        false // clearStandDisguise
                );
                // 广播这个包给所有客户端
                PacketHandler.sendToClientsTrackingAndSelf(packet, entity);
            }
        }

        private static void clearStandDisguiseState(AbstractDisguiseStandEntity entity) {
            // 只清除替身伪装状态
            entity.setDisguisedOnce(false);
            entity.setEntityForDisguise(null);

            // 如果是在服务器端，广播更新伪装状态
            if (!entity.level.isClientSide && entity.getId() != -1) {
                // 创建一个新的 YellowTemperanceRenderPacket，其中 playerDisguiseRes 为 null
                YellowTemperanceRenderPacket packet = new YellowTemperanceRenderPacket(
                        null, // playerDisguiseRes 为 null
                        entity.getId(),
                        entity.clientLimbSwing,
                        entity.clientLimbSwingAmount,
                        entity.yHeadRot,
                        entity.xRot,
                        false, // clearUserDisguise
                        true  // clearStandDisguise
                );
                // 广播这个包给所有客户端
                PacketHandler.sendToClientsTrackingAndSelf(packet, entity);
            }
        }

        @Override
        public void encode(YellowTemperanceRenderPacket msg, PacketBuffer buf) {
            // 修复方法混淆：func_192572_a -> writeResourceLocation
            // 当 playerDisguiseRes 为 null 时，写入空字符串作为资源位置
            // 写入资源位置，如果为 null 则使用"minecraft:air"作为占位符
            buf.writeResourceLocation(msg.playerDisguiseRes != null ? msg.playerDisguiseRes : new ResourceLocation("minecraft", "air"));
            buf.writeInt(msg.standId);
            buf.writeFloat(msg.limbSwing);
            buf.writeFloat(msg.limbSwingAmount);
            buf.writeFloat(msg.headYaw);
            buf.writeFloat(msg.headPitch);
            buf.writeBoolean(msg.clearUserDisguise);
            buf.writeBoolean(msg.clearStandDisguise);
        }

        @Override
        public YellowTemperanceRenderPacket decode(PacketBuffer buf) {
            // 修复方法混淆：func_192575_l -> readResourceLocation
            // 读取资源位置，如果为空字符串则设置为 null
            // 读取资源位置，如果为"minecraft:air"则设置为 null
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
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
        }

        @Override
        public void handle(YellowTemperanceRenderPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    // 只在客户端执行，服务端跳过
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
                } else {
                    // 服务端处理（发送者逻辑）
                    handleServer(msg, ctx.get().getSender());
                }
            });
            ctx.get().setPacketHandled(true);
        }

        @Override
        public Class<YellowTemperanceRenderPacket> getPacketClass() {
            return YellowTemperanceRenderPacket.class;
        }
    }
}