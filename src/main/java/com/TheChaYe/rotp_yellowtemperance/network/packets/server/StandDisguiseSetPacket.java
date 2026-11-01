package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.registry.EntityDisguiseRegistry;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.network.packets.IModPacketHandler;
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

/**
 * 替身伪装设置网络包（服务端 -> 客户端）/ Stand Disguise Set Packet (Server -> Client)
 * 用于客户端选择替身伪装后通知服务端，并由服务端广播给所有客户端
 */
public class StandDisguiseSetPacket {
    private final ResourceLocation disguiseRes;
    private final int standId;

    public StandDisguiseSetPacket(ResourceLocation disguiseRes, int standId) {
        this.disguiseRes = disguiseRes;
        this.standId = standId;
    }

    public static class Handler implements IModPacketHandler<StandDisguiseSetPacket> {
        @OnlyIn(Dist.CLIENT)
        private static void handleClient(StandDisguiseSetPacket msg) {
            Entity stand = ClientUtil.getEntityById(msg.standId);
            if (stand instanceof AbstractDisguiseStandEntity) {
                AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity) stand;
                disguiseStand.setEntityForDisguise(ForgeRegistries.ENTITIES.getValue(msg.disguiseRes));
                disguiseStand.setDisguisedOnce(true);
                disguiseStand.setShapeshiftTick(disguiseStand.tickCount);
            }
        }

        private static void handleServer(StandDisguiseSetPacket msg, PlayerEntity sender) {
            if (sender == null || msg.disguiseRes == null) {
                return;
            }
            // 通过世界对象获取实体
            Entity stand = sender.level.getEntity(msg.standId);
            if (!(stand instanceof AbstractDisguiseStandEntity)) {
                return;
            }
            AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity) stand;

            // 只接受白名单内的替身类型：未知 id / 非生物类型会让客户端渲染时崩
            EntityType<?> disguiseType = ForgeRegistries.ENTITIES.getValue(msg.disguiseRes);
            if (!isAllowedStandDisguise(disguiseType, sender)) {
                return;
            }

            // 设置伪装状态
            disguiseStand.setEntityForDisguise(disguiseType);
            disguiseStand.setDisguisedOnce(true);
            disguiseStand.setShapeshiftTick(disguiseStand.tickCount);

            // 广播给所有玩家（包括自己）
            PacketHandler.sendGlobally(new StandDisguiseSetPacket(msg.disguiseRes, disguiseStand.getId()), sender.level.dimension());
        }

        /**
         * 校验替身伪装目标：必须在替身伪装白名单里，且必须能造出生物实体
         * （渲染端会把它当 LivingEntity 用，非生物类型强转会崩客户端）。
         */
        private static boolean isAllowedStandDisguise(EntityType<?> disguiseType, PlayerEntity sender) {
            if (disguiseType == null) {
                return false;
            }
            if (!EntityDisguiseRegistry.getEntitiesForStandDisguise().contains(disguiseType)) {
                return false;
            }
            try {
                Entity probe = disguiseType.create(sender.level);
                return probe instanceof LivingEntity;
            } catch (Throwable t) {
                // 造不出来就当作不支持，绝不把异常抛进网络线程
                return false;
            }
        }

        @Override
        public void encode(StandDisguiseSetPacket msg, PacketBuffer buf) {
            // 写入资源位置，如果为 null 则使用"minecraft:air"作为占位符
            buf.writeResourceLocation(msg.disguiseRes != null ? msg.disguiseRes : new ResourceLocation("minecraft", "air"));
            buf.writeInt(msg.standId);
        }

        @Override
        public StandDisguiseSetPacket decode(PacketBuffer buf) {
            return new StandDisguiseSetPacket(buf.readResourceLocation(), buf.readInt());
        }

        @Override
        public void handle(StandDisguiseSetPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
                } else {
                    handleServer(msg, ctx.get().getSender());
                }
            });
            ctx.get().setPacketHandled(true);
        }

        @Override
        public Class<StandDisguiseSetPacket> getPacketClass() {
            return StandDisguiseSetPacket.class;
        }
    }
}
