package com.TheChaYe.rotp_yellowtemperance.init;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

/**
 * 标签初始化 / Tags Initialization
 * 管理伪装功能使用的NBT标签常量
 */
public class InitTags {
    /**
     * 玩家是否有伪装标签 / Player has disguise tag
     */
    public static final String YT_HAS_DISGUISE_TAG = "YT_HAS_DISGUISE";
    /**
     * 伪装UUID标签 / Disguise UUID tag
     */
    public static final String YT_DISGUISE_UUID_TAG = "YTDisguiseUUID";
    /**
     * 伪装配置文件标签 / Disguise profile tag
     */
    public static final String YT_DISGUISE_PROFILE_TAG = "YTDisguiseProfile";
    /**
     * 伪装名称标签 / Disguise name tag
     */
    public static final String YT_DISGUISE_NAME_TAG = "YTDisguiseName";
    /**
     * 移除伪装标签 / Remove disguise tag
     */
    public static final String YT_REMOVE_DISGUISE = "YTRemoveDisguise";
    /**
     * 伪装加载中标签 / Disguise loading tag
     */
    public static final String YT_DISGUISE_LOADING_TAG = "YTDisguiseLoading";

    /**
     * 获取玩家伪装名称 / Get player disguise name
     *
     * @param player 玩家实体 / Player entity
     * @return 伪装名称，如果没有则返回空字符串 / Disguise name, empty string if none
     */
    public static String getPlayerDisguiseName(PlayerEntity player) {
        CompoundNBT nbt = player.getPersistentData();
        return nbt.contains(YT_DISGUISE_NAME_TAG) ?
                nbt.getString(YT_DISGUISE_NAME_TAG) : "";
    }
}