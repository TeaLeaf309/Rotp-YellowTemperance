/**
 * 伪装能力接口 / Disguise Capability Interface
 * 定义实体伪装功能的标准接口
 * Defines standard interface for entity disguise functionality
 */
package com.TheChaYe.rotp_yellowtemperance.capability;

import net.minecraft.nbt.CompoundNBT;

import java.util.UUID;

public interface IDisguiseCapability {
    
    /**
     * 获取伪装档案 / Get disguise profile
     * @return 伪装档案NBT数据 / Disguise profile NBT data
     */
    CompoundNBT getDisguiseProfile();

    /**
     * 设置伪装档案 / Set disguise profile
     * @param profile 伪装档案NBT数据 / Disguise profile NBT data
     */
    void setDisguiseProfile(CompoundNBT profile);

    /**
     * 获取伪装UUID / Get disguise UUID
     * @return 伪装实体的UUID / UUID of disguised entity
     */
    UUID getDisguiseUUID();

    /**
     * 设置伪装UUID / Set disguise UUID
     * @param uuid 伪装实体的UUID / UUID of disguised entity
     */
    void setDisguiseUUID(UUID uuid);

    /**
     * 获取伪装名称 / Get disguise name
     * @return 伪装实体的名称 / Name of disguised entity
     */
    String getDisguiseName();

    /**
     * 设置伪装名称 / Set disguise name
     * @param name 伪装实体的名称 / Name of disguised entity
     */
    void setDisguiseName(String name);

    /**
     * 设置伪装状态 / Set disguise state
     * @param hasDisguise 是否具有伪装 / Whether has disguise
     */
    void setHasDisguise(boolean hasDisguise);

    /**
     * 检查是否具有伪装 / Check if has disguise
     * @return 是否具有伪装状态 / Whether has disguise state
     */
    boolean hasDisguise();

    /**
     * 检查搜索辅助是否启用 / Check if search helper is enabled
     * @return 搜索辅助启用状态 / Search helper enabled state
     */
    boolean isSearchHelperEnabled();

    /**
     * 设置搜索辅助启用状态 / Set search helper enabled state
     * @param enabled 是否启用搜索辅助 / Whether to enable search helper
     */
    void setSearchHelperEnabled(boolean enabled);

    /**
     * 序列化为NBT格式 / Serialize to NBT format
     * @return 序列化的NBT数据 / Serialized NBT data
     */
    CompoundNBT serializeNBT();

    /**
     * 从NBT反序列化 / Deserialize from NBT
     * @param nbt NBT数据 / NBT data
     */
    void deserializeNBT(CompoundNBT nbt);
}