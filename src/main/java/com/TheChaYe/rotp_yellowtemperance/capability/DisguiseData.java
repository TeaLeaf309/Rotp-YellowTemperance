package com.TheChaYe.rotp_yellowtemperance.capability;

import net.minecraft.nbt.CompoundNBT;

import java.util.UUID;

/**
 * 伪装数据实现类 / Disguise Data Implementation Class
 * 实现IDisguiseCapability接口，管理实体的伪装状态和相关数据
 * Implements IDisguiseCapability interface, managing entity disguise state and related data
 */
public class DisguiseData implements IDisguiseCapability {

    /**
     * 伪装档案数据 / Disguise profile data
     */
    private CompoundNBT profile = new CompoundNBT();

    /**
     * 伪装实体的UUID / UUID of disguised entity
     */
    private UUID disguiseUUID;

    /**
     * 伪装实体的名称 / Name of disguised entity
     */
    private String disguiseName = "";

    /**
     * 是否具有伪装状态 / Whether has disguise state
     */
    private boolean hasDisguise = false;

    /**
     * 搜索辅助功能是否启用 / Whether search helper is enabled
     */
    private boolean searchHelperEnabled = true; // 默认开启搜索辅助 / Search helper enabled by default

    /**
     * 获取伪装档案 / Get disguise profile
     *
     * @return 伪装档案NBT数据 / Disguise profile NBT data
     */
    @Override
    public CompoundNBT getDisguiseProfile() {
        return profile;
    }

    /**
     * 设置伪装档案 / Set disguise profile
     *
     * @param profile 伪装档案NBT数据 / Disguise profile NBT data
     */
    @Override
    public void setDisguiseProfile(CompoundNBT profile) {
        this.profile = profile;
    }

    /**
     * 获取伪装UUID / Get disguise UUID
     *
     * @return 伪装实体的UUID / UUID of disguised entity
     */
    @Override
    public UUID getDisguiseUUID() {
        return disguiseUUID;
    }

    /**
     * 设置伪装UUID / Set disguise UUID
     *
     * @param uuid 伪装实体的UUID / UUID of disguised entity
     */
    @Override
    public void setDisguiseUUID(UUID uuid) {
        this.disguiseUUID = uuid;
    }

    /**
     * 获取伪装名称 / Get disguise name
     *
     * @return 伪装实体的名称 / Name of disguised entity
     */
    @Override
    public String getDisguiseName() {
        return disguiseName;
    }

    /**
     * 设置伪装名称 / Set disguise name
     *
     * @param name 伪装实体的名称 / Name of disguised entity
     */
    @Override
    public void setDisguiseName(String name) {
        this.disguiseName = name;
    }

    /**
     * 设置伪装状态 / Set disguise state
     *
     * @param hasDisguise 是否具有伪装 / Whether has disguise
     */
    @Override
    public void setHasDisguise(boolean hasDisguise) {
        this.hasDisguise = hasDisguise;
    }

    /**
     * 检查是否具有伪装 / Check if has disguise
     *
     * @return 是否具有伪装状态 / Whether has disguise state
     */
    @Override
    public boolean hasDisguise() {
        return hasDisguise;
    }

    /**
     * 检查搜索辅助是否启用 / Check if search helper is enabled
     *
     * @return 搜索辅助启用状态 / Search helper enabled state
     */
    @Override
    public boolean isSearchHelperEnabled() {
        return searchHelperEnabled;
    }

    /**
     * 设置搜索辅助启用状态 / Set search helper enabled state
     *
     * @param enabled 是否启用搜索辅助 / Whether to enable search helper
     */
    @Override
    public void setSearchHelperEnabled(boolean enabled) {
        this.searchHelperEnabled = enabled;
    }

    /**
     * 序列化为NBT格式 / Serialize to NBT format
     *
     * @return 序列化的NBT数据 / Serialized NBT data
     */
    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        if (hasDisguise) {
            nbt.put("Profile", profile);
            if (disguiseUUID != null) {
                nbt.putUUID("UUID", disguiseUUID);
            }
            nbt.putString("Name", disguiseName);
            nbt.putBoolean("HasDisguise", true);
        }
        // 保存搜索辅助设置 / Save search helper settings
        nbt.putBoolean("SearchHelperEnabled", searchHelperEnabled);
        return nbt;
    }

    /**
     * 从NBT反序列化 / Deserialize from NBT
     *
     * @param nbt NBT数据 / NBT data
     */
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        if (nbt.contains("HasDisguise") && nbt.getBoolean("HasDisguise")) {
            profile = nbt.getCompound("Profile");
            disguiseUUID = nbt.hasUUID("UUID") ? nbt.getUUID("UUID") : null;
            disguiseName = nbt.getString("Name");
            hasDisguise = true;
        } else {
            hasDisguise = false;
        }
        // 读取搜索辅助设置 / Read search helper settings
        searchHelperEnabled = !nbt.contains("SearchHelperEnabled") || nbt.getBoolean("SearchHelperEnabled");
    }
}