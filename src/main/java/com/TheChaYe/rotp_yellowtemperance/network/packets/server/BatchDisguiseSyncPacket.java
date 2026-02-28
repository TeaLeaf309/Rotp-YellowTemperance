package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 批量伪装同步网络包 / Batch Disguise Sync Network Packet
 * 用于向新加入的玩家发送所有在线玩家的伪装状态
 */
public class BatchDisguiseSyncPacket {
    /** 玩家伪装数据列表 / List of player disguise data */
    private final List<PlayerDisguiseData> disguiseDataList;

    /**
     * 构造函数 / Constructor
     * @param disguiseDataList 玩家伪装数据列表 / List of player disguise data
     */
    public BatchDisguiseSyncPacket(List<PlayerDisguiseData> disguiseDataList) {
        this.disguiseDataList = disguiseDataList;
    }

    /**
     * 编码网络包 / Encode network packet
     * 将批量伪装同步包数据写入网络缓冲区
     * @param msg 要编码的消息 / Message to encode
     * @param buffer 网络缓冲区 / Network buffer
     */
    public static void encode(BatchDisguiseSyncPacket msg, PacketBuffer buffer) {
        buffer.writeInt(msg.disguiseDataList.size());
        for (PlayerDisguiseData data : msg.disguiseDataList) {
            buffer.writeUUID(data.playerId);
            buffer.writeBoolean(data.hasDisguise);
            if (data.hasDisguise) {
                buffer.writeUtf(data.disguiseName);
                buffer.writeNbt(data.profileTag);
            }
        }
    }

    /**
     * 解码网络包 / Decode network packet
     * 从网络缓冲区读取批量伪装同步包数据
     * @param buffer 网络缓冲区 / Network buffer
     * @return 解码后的批量伪装同步包 / Decoded batch disguise sync packet
     */
    public static BatchDisguiseSyncPacket decode(PacketBuffer buffer) {
        int count = buffer.readInt();
        List<PlayerDisguiseData> dataList = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            UUID playerId = buffer.readUUID();
            boolean hasDisguise = buffer.readBoolean();
            
            String disguiseName = "";
            CompoundNBT profileTag = null;
            
            if (hasDisguise) {
                disguiseName = buffer.readUtf(32767);
                profileTag = buffer.readNbt();
            }
            
            dataList.add(new PlayerDisguiseData(playerId, hasDisguise, disguiseName, profileTag));
        }
        
        return new BatchDisguiseSyncPacket(dataList);
    }

    /**
     * 处理网络包 / Handle network packet
     * 客户端处理批量伪装同步逻辑
     * @param msg 接收到的消息 / Received message
     * @param ctx 网络上下文 / Network context
     */
    public static void handle(BatchDisguiseSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    RotPYellowTemperanceAddon.LOGGER.info("Received batch disguise sync packet with {} players", msg.disguiseDataList.size());
                    
                    for (PlayerDisguiseData data : msg.disguiseDataList) {
                        try {
                            // 更新玩家的NBT数据 / Update player's NBT data
                            net.minecraft.entity.player.PlayerEntity player = mc.level.getPlayerByUUID(data.playerId);
                            if (player != null) {
                                CompoundNBT playerNbt = player.getPersistentData();
                                playerNbt.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, data.hasDisguise);
                                
                                if (data.hasDisguise) {
                                    playerNbt.putString(InitTags.YT_DISGUISE_NAME_TAG, data.disguiseName);
                                    if (data.profileTag != null) {
                                        playerNbt.put("disguiseProfile", data.profileTag);
                                        // 缓存伪装数据到客户端 / Cache disguise data to client
                                        ClientDisguiseCache.cachePlayerDisguiseProfile(data.playerId, data.profileTag);
                                        RotPYellowTemperanceAddon.LOGGER.debug("Cached disguise for player {}: {}", data.playerId, data.disguiseName);
                                    }
                                } else {
                                    // 清理伪装数据 / Clear disguise data
                                    playerNbt.remove(InitTags.YT_HAS_DISGUISE_TAG);
                                    playerNbt.remove(InitTags.YT_DISGUISE_NAME_TAG);
                                    playerNbt.remove("disguiseProfile");
                                    ClientDisguiseCache.clearPlayerDisguise(data.playerId);
                                    RotPYellowTemperanceAddon.LOGGER.debug("Cleared disguise for player {}", data.playerId);
                                    // 强制重新渲染以显示原始模型 / Force re-render to show original model
                                }
                                
                                // 强制重新渲染玩家实体 / Force re-render player entity
                                forceEntityRerender(player);
                            }
                        } catch (Exception e) {
                            RotPYellowTemperanceAddon.LOGGER.error("Failed to process disguise data for player {}: {}", data.playerId, e.getMessage());
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 玩家伪装数据结构 / Player disguise data structure
     */
    /**
     * 强制重新渲染玩家实体 / Force re-render player entity
     * @param player 目标玩家实体 / Target player entity
     */
    @OnlyIn(Dist.CLIENT)
    private static void forceEntityRerender(PlayerEntity player) {
        try {
            // 方法1: 使用反射调用私有方法 / Method 1: Use reflection to call private method
            java.lang.reflect.Method setSectionDirtyMethod = WorldRenderer.class.getDeclaredMethod("setSectionDirty", int.class, int.class, int.class, boolean.class);
            setSectionDirtyMethod.setAccessible(true);

            ChunkPos chunkPos = new ChunkPos(player.blockPosition());
            setSectionDirtyMethod.invoke(
                    Minecraft.getInstance().levelRenderer,
                    chunkPos.x,
                    player.getY() / 16, // 计算垂直区块位置 / Calculate vertical chunk position
                    chunkPos.z,
                    true
            );
        } catch (Exception e) {
            // 如果反射失败，使用其他方法 / If reflection fails, use alternative method
            RotPYellowTemperanceAddon.LOGGER.warn("Failed to set section dirty via reflection, using fallback method", e);
            forceEntityRerenderFallback(player);
        }
    }

    /**
     * 强制重新渲染的备用方法 / Fallback method for force re-rendering
     * @param player 目标玩家实体 / Target player entity
     */
    @OnlyIn(Dist.CLIENT)
    private static void forceEntityRerenderFallback(PlayerEntity player) {
        // 方法2: 轻微移动实体位置 / Method 2: Slightly move entity position
        BlockPos originalPos = player.blockPosition();
        double originalX = player.getX();
        double originalY = player.getY();
        double originalZ = player.getZ();

        // 轻微移动实体 / Slightly move entity
        player.setPos(originalX, originalY + 0.0001, originalZ);

        // 立即移回 / Move back immediately
        player.setPos(originalX, originalY, originalZ);

        // 标记实体需要更新 / Mark entity needs update
        player.hasImpulse = true;
    }

    /**
     * 玩家伪装数据类 / Player disguise data class
     * 存储单个玩家的伪装相关信息
     */
    public static class PlayerDisguiseData {
        /** 玩家UUID / Player UUID */
        public final UUID playerId;
        /** 是否有伪装 / Whether has disguise */
        public final boolean hasDisguise;
        /** 伪装名称 / Disguise name */
        public final String disguiseName;
        /** 伪装配置文件标签 / Disguise profile tag */
        public final CompoundNBT profileTag;

        /**
         * 构造函数 / Constructor
         * @param playerId 玩家UUID / Player UUID
         * @param hasDisguise 是否有伪装 / Whether has disguise
         * @param disguiseName 伪装名称 / Disguise name
         * @param profileTag 伪装配置文件标签 / Disguise profile tag
         */
        public PlayerDisguiseData(UUID playerId, boolean hasDisguise, String disguiseName, CompoundNBT profileTag) {
            this.playerId = playerId;
            this.hasDisguise = hasDisguise;
            this.disguiseName = disguiseName;
            this.profileTag = profileTag;
        }
    }
}