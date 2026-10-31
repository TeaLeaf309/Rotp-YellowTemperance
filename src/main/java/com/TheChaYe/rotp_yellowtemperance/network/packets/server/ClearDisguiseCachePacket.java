package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 管理员指令 -> 客户端：清理伪装皮肤缓存。
 *
 * <p>服务端缓存管理好办，但真正影响显示的是客户端 {@link ClientDisguiseCache}，
 * 所以清缓存/强制刷新时要通知客户端。</p>
 */
public class ClearDisguiseCachePacket {
    /** 清空所有客户端伪装缓存。 */
    public static final byte MODE_ALL = 0;
    /** 忘掉某个伪装目标（名字）的缓存。 */
    public static final byte MODE_TARGET = 1;
    /** 清某个被伪装玩家的缓存（按 UUID），用于强制刷新。 */
    public static final byte MODE_PLAYER = 2;

    private final byte mode;
    private final String targetName;
    private final UUID playerId;

    private ClearDisguiseCachePacket(byte mode, String targetName, UUID playerId) {
        this.mode = mode;
        this.targetName = targetName == null ? "" : targetName;
        this.playerId = playerId;
    }

    public static ClearDisguiseCachePacket all() {
        return new ClearDisguiseCachePacket(MODE_ALL, "", null);
    }

    public static ClearDisguiseCachePacket target(String targetName) {
        return new ClearDisguiseCachePacket(MODE_TARGET, targetName, null);
    }

    public static ClearDisguiseCachePacket player(UUID playerId) {
        return new ClearDisguiseCachePacket(MODE_PLAYER, "", playerId);
    }

    public static void encode(ClearDisguiseCachePacket msg, PacketBuffer buffer) {
        buffer.writeByte(msg.mode);
        if (msg.mode == MODE_TARGET) {
            buffer.writeUtf(msg.targetName, 256);
        } else if (msg.mode == MODE_PLAYER) {
            buffer.writeUUID(msg.playerId);
        }
    }

    public static ClearDisguiseCachePacket decode(PacketBuffer buffer) {
        byte mode = buffer.readByte();
        if (mode == MODE_TARGET) {
            return new ClearDisguiseCachePacket(mode, buffer.readUtf(256), null);
        }
        if (mode == MODE_PLAYER) {
            return new ClearDisguiseCachePacket(mode, "", buffer.readUUID());
        }
        return new ClearDisguiseCachePacket(MODE_ALL, "", null);
    }

    public static void handle(ClearDisguiseCachePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ClearDisguiseCachePacket msg) {
        switch (msg.mode) {
            case MODE_TARGET:
                ClientDisguiseCache.forgetTarget(msg.targetName);
                break;
            case MODE_PLAYER:
                ClientDisguiseCache.clearPlayerCaches(msg.playerId);
                break;
            case MODE_ALL:
            default:
                ClientDisguiseCache.clearAllCaches();
                break;
        }
    }
}
