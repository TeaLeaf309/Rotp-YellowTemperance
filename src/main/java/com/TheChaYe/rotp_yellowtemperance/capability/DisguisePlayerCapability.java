/**
 * 玩家伪装能力实现类 / Player Disguise Capability Implementation Class
 * 管理实体的伪装状态和数据
 * Manages entity disguise state and data
 * <p>
 * 实现ICapabilitySerializable接口，使伪装能力可以序列化和反序列化
 * Implements ICapabilitySerializable interface, enabling serialization and deserialization of disguise capability
 * 从而能够在游戏保存和加载时保留伪装状态
 * thus preserving disguise state during game save and load operations
 *
 * @author TheChaYe
 * @since 1.0.0
 */
package com.TheChaYe.rotp_yellowtemperance.capability;

import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 玩家伪装能力类 / Player Disguise Capability Class
 * 实现ICapabilitySerializable接口，提供玩家伪装功能
 * Implements ICapabilitySerializable interface, providing player disguise functionality
 */
public class DisguisePlayerCapability implements ICapabilitySerializable<CompoundNBT> {

    /**
     * 伪装能力的懒加载实例 / Lazy-loaded instance of disguise capability
     * 使用LazyOptional确保按需创建 / Using LazyOptional to ensure on-demand creation
     * 使用DisguiseData::new作为供应者，在需要时才创建实际的数据对象
     * Using DisguiseData::new as supplier, creating actual data object only when needed
     */
    private final LazyOptional<IDisguiseCapability> instance = LazyOptional.of(DisguiseData::new);

    /**
     * 获取指定能力的可选包装对象 / Get optional wrapper object for specified capability
     * <p>
     * 实现Minecraft的能力系统接口 / Implements Minecraft's capability system interface
     * 当其他模块请求特定能力时返回相应的实例
     * Returns corresponding instance when other modules request specific capability
     * @param cap 要获取的能力类型 / Capability type to get
     * @param side 方向参数，通常表示连接方向 / Direction parameter, usually representing connection direction
     * @return 返回指定能力类型的LazyOptional包装对象 / Returns LazyOptional wrapper object of specified capability type
     */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return InitCapabilities.DISGUISE_CAPABILITY.orEmpty(cap, instance);
    }

    /**
     * 序列化能力数据到NBT格式 / Serialize capability data to NBT format
     * <p>
     * 将当前伪装能力的状态转换为NBT格式 / Converts current disguise capability state to NBT format
     * 以便保存到游戏存档中 / for saving to game archives
     * @return 包含所有伪装数据的CompoundNBT对象 / CompoundNBT object containing all disguise data
     */
    @Override
    public CompoundNBT serializeNBT() {
        return instance.orElseThrow(IllegalStateException::new).serializeNBT();
    }

    /**
     * 从NBT格式反序列化能力数据 / Deserialize capability data from NBT format
     * <p>
     * 从游戏存档中读取并恢复伪装能力的状态 / Reads from game archives and restores disguise capability state
     * @param nbt 包含伪装数据的CompoundNBT对象 / CompoundNBT object containing disguise data
     */
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        instance.orElseThrow(IllegalStateException::new).deserializeNBT(nbt);
    }

    /**
     * 伪装能力的具体数据实现类 / Specific data implementation class for disguise capability
     * <p>
     * 管理具体的伪装信息 / Manages specific disguise information
     * 包括伪装档案、UUID、名称和是否具有伪装状态
     * including disguise profile, UUID, name, and whether has disguise state
     */
    public static class DisguiseData implements IDisguiseCapability {

        /**
         * 存储伪装档案的NBT数据 / NBT data storing disguise profile
         */
        private CompoundNBT profile = new CompoundNBT();

        /**
         * 伪装实体的UUID标识符 / UUID identifier of disguised entity
         */
        private UUID disguiseUUID;

        /**
         * 伪装实体的显示名称 / Display name of disguised entity
         */
        private String disguiseName = "";

        /**
         * 标识是否已经设置伪装状态 / Indicates whether disguise state has been set
         */
        private boolean hasDisguise = false;

        /**
         * 搜索辅助功能是否启用 / Whether search helper function is enabled
         */
        private boolean searchHelperEnabled = true; // 默认开启 / Enabled by default

        /**
         * 获取当前的伪装档案数据 / Get current disguise profile data
         * @return 当前的伪装档案NBT数据 / Current disguise profile NBT data
         */
        @Override
        public CompoundNBT getDisguiseProfile() {
            return profile;
        }

        /**
         * 设置伪装档案数据 / Set disguise profile data
         * @param profile 要设置的伪装档案NBT数据 / Disguise profile NBT data to set
         */
        @Override
        public void setDisguiseProfile(CompoundNBT profile) {
            this.profile = profile;
        }

        /**
         * 获取伪装实体的UUID / Get UUID of disguised entity
         * @return 伪装实体的UUID / UUID of disguised entity
         */
        @Override
        public UUID getDisguiseUUID() {
            return disguiseUUID;
        }

        /**
         * 设置伪装实体的UUID / Set UUID of disguised entity
         * @param uuid 要设置的伪装实体UUID / UUID of disguised entity to set
         */
        @Override
        public void setDisguiseUUID(UUID uuid) {
            this.disguiseUUID = uuid;
        }

        /**
         * 获取伪装实体的显示名称 / Get display name of disguised entity
         * @return 伪装实体的显示名称 / Display name of disguised entity
         */
        @Override
        public String getDisguiseName() {
            return disguiseName;
        }

        /**
         * 设置伪装实体的显示名称 / Set display name of disguised entity
         * @param name 要设置的显示名称 / Display name to set
         */
        @Override
        public void setDisguiseName(String name) {
            this.disguiseName = name;
        }

        /**
         * 设置是否有伪装状态 / Set whether has disguise state
         * @param hasDisguise 是否有伪装状态 / Whether has disguise state
         */
        @Override
        public void setHasDisguise(boolean hasDisguise) {
            this.hasDisguise = hasDisguise;
        }

        /**
         * 检查是否存在伪装状态 / Check if disguise state exists
         * @return 如果存在伪装返回true，否则返回false / Returns true if disguise exists, false otherwise
         */
        @Override
        public boolean hasDisguise() {
            return hasDisguise;
        }

        /**
         * 检查搜索辅助是否启用 / Check if search helper is enabled
         * @return 搜索辅助启用状态 / Search helper enabled state
         */
        @Override
        public boolean isSearchHelperEnabled() {
            return searchHelperEnabled;
        }

        /**
         * 设置搜索辅助启用状态 / Set search helper enabled state
         * @param enabled 是否启用搜索辅助 / Whether to enable search helper
         */
        @Override
        public void setSearchHelperEnabled(boolean enabled) {
            this.searchHelperEnabled = enabled;
        }

        /**
         * 将伪装数据序列化为NBT格式 / Serialize disguise data to NBT format
         * <p>
         * 只有当存在伪装状态时才会序列化相关数据 / Only serializes related data when disguise state exists
         * @return 包含伪装数据的CompoundNBT对象 / CompoundNBT object containing disguise data
         */
        @Override
        public CompoundNBT serializeNBT() {
            CompoundNBT nbt = new CompoundNBT();
            if (hasDisguise) {
                nbt.put("Profile", profile);           // 添加伪装档案 / Add disguise profile
                if (disguiseUUID != null) {
                    nbt.putUUID("UUID", disguiseUUID);   // 添加UUID / Add UUID
                }
                nbt.putString("Name", disguiseName);   // 添加名称 / Add name
                nbt.putBoolean("HasDisguise", true);   // 标记存在伪装 / Mark existence of disguise
            }
            // 保存搜索辅助设置 / Save search helper settings
            nbt.putBoolean("SearchHelperEnabled", searchHelperEnabled);
            return nbt;
        }

        /**
         * 从NBT格式反序列化伪装数据 / Deserialize disguise data from NBT format
         * <p>
         * 从NBT数据中恢复伪装状态及相关属性 / Restores disguise state and related properties from NBT data
         * @param nbt 包含伪装数据的CompoundNBT对象 / CompoundNBT object containing disguise data
         */
        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            if (nbt.contains("HasDisguise") && nbt.getBoolean("HasDisguise")) {
                profile = nbt.getCompound("Profile");          // 恢复档案 / Restore profile
                disguiseUUID = nbt.hasUUID("UUID") ? nbt.getUUID("UUID") : null;  // 恢复UUID / Restore UUID
                disguiseName = nbt.getString("Name");           // 恢复名称 / Restore name
                hasDisguise = true;                            // 设置标志位 / Set flag
            } else {
                hasDisguise = false;                           // 清除伪装状态 / Clear disguise state
            }
            // 读取搜索辅助设置 / Read search helper settings
            searchHelperEnabled = !nbt.contains("SearchHelperEnabled") || nbt.getBoolean("SearchHelperEnabled");
        }
    }
}
