package com.TheChaYe.rotp_yellowtemperance.capability.entity;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncDollRenderingPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncErosionActivePacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncErosionLevelPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 生物实体数据类 / Living Entity Data Class
 * 管理生物实体的各种状态数据，包括侵蚀状态和渲染设置
 * Manages various state data for living entities, including erosion state and rendering settings
 */
public class LivingData implements INBTSerializable<CompoundNBT> {
    
    /**
     * 关联的生物实体 / Associated living entity
     */
    private final LivingEntity entity;
    
    /**
     * 最大格挡值 / Maximum block value
     */
    private int maxBlock = 0;
    
    /**
     * 侵蚀状态持续时间 / Duration in erosion state
     */
    private int ticksInErosion = 0;
    
    /**
     * 非侵蚀状态持续时间 / Duration without erosion
     */
    private int ticksWithoutErosion = 0;
    
    /**
     * 侵蚀是否激活 / Whether erosion is activated
     */
    private boolean erosionActivated = false;

    /**
     * 侵蚀等级 / Erosion level
     */
    private int erosionLevel = 0;
    
    /**
     * 侵蚀活动状态 / Erosion active state
     */
    private boolean erosionActive = false;

    /**
     * 纸娃娃渲染状态 / Doll rendering state
     */
    private boolean dollRendering = false;

    /**
     * 默认构造函数 / Default constructor
     */
    public LivingData() {
        this(null);
    }

    /**
     * 带实体参数的构造函数 / Constructor with entity parameter
     * @param entity 关联的生物实体 / Associated living entity
     */
    public LivingData(LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * 获取最大格挡值 / Get maximum block value
     * @return 最大格挡值 / Maximum block value
     */
    public int getMaxBlock() {
        return this.maxBlock;
    }

    /**
     * 获取侵蚀状态持续时间 / Get duration in erosion state
     * @return 侵蚀状态tick数 / Ticks in erosion state
     */
    public int getTicksInErosion() {
        return this.ticksInErosion;
    }

    /**
     * 设置侵蚀状态持续时间 / Set duration in erosion state
     * @param ticksInErosion 侵蚀状态tick数 / Ticks in erosion state
     */
    public void setTicksInErosion(int ticksInErosion) {
        this.ticksInErosion = ticksInErosion;
    }

    /**
     * 获取非侵蚀状态持续时间 / Get duration without erosion
     * @return 非侵蚀状态tick数 / Ticks without erosion
     */
    public int getTicksWithoutErosion() {
        return this.ticksWithoutErosion;
    }

    /**
     * 设置非侵蚀状态持续时间 / Set duration without erosion
     * @param ticksWithoutErosion 非侵蚀状态tick数 / Ticks without erosion
     */
    public void setTicksWithoutErosion(int ticksWithoutErosion) {
        this.ticksWithoutErosion = ticksWithoutErosion;
    }

    /**
     * 检查侵蚀是否激活 / Check if erosion is activated
     * @return 侵蚀激活状态 / Erosion activation state
     */
    public boolean isErosionActivated() {
        return erosionActivated;
    }

    // ======== 侵蚀相关方法 / Erosion Related Methods ========
    
    /**
     * 获取侵蚀等级 / Get erosion level
     * @return 当前侵蚀等级 / Current erosion level
     */
    public int getErosionLevel() {
        return erosionLevel;
    }

    /**
     * 设置侵蚀等级 / Set erosion level
     * @param level 新的侵蚀等级 / New erosion level
     */
    public void setErosionLevel(int level) {
        this.erosionLevel = level;
        // 同步到客户端 / Sync to client
        if (entity instanceof ServerPlayerEntity) {
//            RotPYellowTemperanceAddon.LOGGER.debug("同步侵蚀等级: {}", level);
            PacketHandler.sendToClient(
                    new SyncErosionLevelPacket(entity.getId(), level),
                    (ServerPlayerEntity) entity
            );
        }
    }

    /**
     * 检查侵蚀是否活跃 / Check if erosion is active
     * @return 侵蚀活跃状态 / Erosion active state
     */
    public boolean isErosionActive() {
        return erosionActive;
    }

    /**
     * 设置侵蚀活跃状态 / Set erosion active state
     * @param active 是否活跃 / Whether active
     */
    public void setErosionActive(boolean active) {
        this.erosionActive = active;
        // 同步到客户端 / Sync to client
        if (entity instanceof ServerPlayerEntity) {
//            RotPYellowTemperanceAddon.LOGGER.debug("同步侵蚀激活状态: {}", active);
            PacketHandler.sendToClient(
                    new SyncErosionActivePacket(entity.getId(), active),
                    (ServerPlayerEntity) entity
            );
        }
    }

    // ======== 纸娃娃渲染相关方法 / Doll Rendering Related Methods ========
    
    /**
     * 检查是否启用纸娃娃渲染 / Check if doll rendering is enabled
     * @return 纸娃娃渲染状态 / Doll rendering state
     */
    public boolean isDollRendering() {
        return dollRendering;
    }

    /**
     * 设置纸娃娃渲染状态 / Set doll rendering state
     * @param dollRendering 是否启用纸娃娃渲染 / Whether to enable doll rendering
     */
    public void setDollRendering(boolean dollRendering) {
        this.dollRendering = dollRendering;
        // 同步到客户端 / Sync to client
        if (entity instanceof ServerPlayerEntity) {
            PacketHandler.sendToClient(
                    new SyncDollRenderingPacket(entity.getId(), dollRendering),
                    (ServerPlayerEntity) entity
            );
        }
    }

    /**
     * 与任意玩家同步数据 / Sync data with any player
     * @param player 目标玩家 / Target player
     */
    public void syncWithAnyPlayer(ServerPlayerEntity player) {
        // 同步侵蚀状态 / Sync erosion state
        PacketHandler.sendToClient(
                new SyncErosionLevelPacket(entity.getId(), erosionLevel),
                player
        );
        PacketHandler.sendToClient(
                new SyncErosionActivePacket(entity.getId(), erosionActive),
                player
        );
    }

    /**
     * 序列化为NBT格式 / Serialize to NBT format
     * @return 序列化的NBT数据 / Serialized NBT data
     */
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("MaxBlock", this.maxBlock);
        nbt.putInt("ticksInErosion", this.ticksInErosion);
        nbt.putInt("ticksWithoutErosion", this.ticksWithoutErosion);
        nbt.putBoolean("ErosionActive", this.erosionActivated);

        // 侵蚀状态序列化 / Erosion state serialization
        nbt.putInt("ErosionLevel", this.erosionLevel);
        nbt.putBoolean("ErosionActive", this.erosionActive);

        // 纸娃娃渲染状态序列化 / Doll rendering state serialization
        nbt.putBoolean("DollRendering", this.dollRendering);

        return nbt;
    }

    /**
     * 从NBT反序列化 / Deserialize from NBT
     * @param nbt NBT数据 / NBT data
     */
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.maxBlock = nbt.getInt("MaxBlock");
        this.ticksInErosion = nbt.getInt("ticksInErosion");
        this.ticksWithoutErosion = nbt.getInt("ticksWithoutErosion");
        this.erosionActivated = nbt.getBoolean("ErosionActive");

        // 侵蚀状态反序列化 / Erosion state deserialization
        this.erosionLevel = nbt.getInt("ErosionLevel");
        this.erosionActive = nbt.getBoolean("ErosionActive");

        // 纸娃娃渲染状态反序列化 / Doll rendering state deserialization
        this.dollRendering = nbt.getBoolean("DollRendering");
    }

    /**
     * 能力提供者类 / Capability provider class
     * 实现ICapabilitySerializable接口，提供LivingData能力的序列化功能
     * Implements ICapabilitySerializable interface, providing serialization functionality for LivingData capability
     */
    public static class Provider implements ICapabilitySerializable<CompoundNBT> {
        
        /**
         * LivingData实例 / LivingData instance
         */
        private final LivingData livingData;
        
        /**
         * 懒加载可选项 / Lazy optional wrapper
         */
        private final LazyOptional<LivingData> optional;

        /**
         * 构造函数 / Constructor
         * @param entity 关联的生物实体 / Associated living entity
         */
        public Provider(LivingEntity entity) {
            this.livingData = new LivingData(entity);
            this.optional = LazyOptional.of(() -> livingData);
        }

        /**
         * 获取指定能力 / Get specified capability
         * @param cap 能力类型 / Capability type
         * @param side 方向 / Direction
         * @return 能力的可选项包装 / Optional wrapper for the capability
         */
        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            if (cap == CapabilityHandler.LIVING_DATA_CAPABILITY) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }

        /**
         * 序列化为NBT / Serialize to NBT
         * @return 序列化的NBT数据 / Serialized NBT data
         */
        @Override
        public CompoundNBT serializeNBT() {
            return livingData.serializeNBT();
        }

        /**
         * 从NBT反序列化 / Deserialize from NBT
         * @param nbt NBT数据 / NBT data
         */
        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            livingData.deserializeNBT(nbt);
        }
    }
}