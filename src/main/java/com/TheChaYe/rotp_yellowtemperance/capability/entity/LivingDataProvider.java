package com.TheChaYe.rotp_yellowtemperance.capability.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 生物数据提供者 / Living Data Provider
 * 实现ICapabilitySerializable接口，为生物实体提供LivingData能力
 * Implements ICapabilitySerializable interface, providing LivingData capability for living entities
 */
public class LivingDataProvider implements ICapabilitySerializable<INBT> {
    
    /**
     * 注入的LivingData能力 / Injected LivingData capability
     */
    @CapabilityInject(LivingData.class)
    public static Capability<LivingData> CAPABILITY = null;
    
    /**
     * LivingData实例的懒加载包装 / LazyOptional wrapper for LivingData instance
     */
    private final LazyOptional<LivingData> instance;

    /**
     * 构造函数 / Constructor
     * @param player 关联的玩家实体 / Associated player entity
     */
    public LivingDataProvider(LivingEntity player) {
        this.instance = LazyOptional.of(() -> new LivingData(player));
    }

    /**
     * 获取指定能力 / Get specified capability
     * @param cap 能力类型 / Capability type
     * @param side 方向 / Direction
     * @return 能力的可选项包装 / Optional wrapper for the capability
     */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return CAPABILITY.orEmpty(cap, instance);
    }

    /**
     * 序列化为NBT / Serialize to NBT
     * @return 序列化的NBT数据 / Serialized NBT data
     */
    @Override
    public INBT serializeNBT() {
        return CAPABILITY.getStorage().writeNBT(CAPABILITY, instance.orElseThrow(
                () -> new IllegalArgumentException("Player capability LazyOptional is not attached.")), null);
    }

    /**
     * 从NBT反序列化 / Deserialize from NBT
     * @param nbt NBT数据 / NBT data
     */
    @Override
    public void deserializeNBT(INBT nbt) {
        CAPABILITY.getStorage().readNBT(CAPABILITY, instance.orElseThrow(
                () -> new IllegalArgumentException("Player capability LazyOptional is not attached.")), null, nbt);
    }
}
