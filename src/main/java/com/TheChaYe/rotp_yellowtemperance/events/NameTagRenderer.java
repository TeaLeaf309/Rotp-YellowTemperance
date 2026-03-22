package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 名称标签渲染器 / Name Tag Renderer
 * 处理伪装玩家的名称标签显示逻辑
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class NameTagRenderer {

    /**
     * 名称标签渲染事件处理 / Name tag render event handler
     * 显示伪装玩家的真实名称或伪装名称
     */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameplateEvent event) {
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            CompoundNBT nbt = player.getPersistentData();

            // 检查玩家是否正在伪装 / Check if player is disguised
            if (nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
                // 优先从客户端缓存的完整Profile中获取真实名称 / Prioritize getting real name from client cache full Profile
                String displayName;

                // 检查是否启用了搜索辅助功能 / Check if search helper is enabled
                // 通过Capability获取搜索辅助功能状态 / Get search helper status through Capability
                boolean searchHelperEnabled = player.getCapability(InitCapabilities.DISGUISE_CAPABILITY)
                        .map(cap -> cap.isSearchHelperEnabled())
                        .orElse(true); // 默认开启 / Default enabled
                // 如果缓存中没有找到，则回退到NBT存储的名称 / Fall back to NBT stored name if not found in cache

                if (!searchHelperEnabled) {
                    displayName = nbt.getString(InitTags.YT_DISGUISE_NAME_TAG);
                } else {
                    displayName = getRealPlayerNameFromCache(player);
                    if (displayName == null || displayName.isEmpty()) {
                        String disguiseName = nbt.getString(InitTags.YT_DISGUISE_NAME_TAG);

                        if (!disguiseName.isEmpty()) {
                            displayName = disguiseName;
                        } else {
                            return; // 没有有效的伪装名称 / No valid disguise name
                        }
                    }
                }
                // 创建自定义名称组件 / Create custom name component
                ITextComponent customName = new StringTextComponent(displayName);


                // 设置自定义名称 / Set custom name
                event.setContent(customName);
            }
        }
    }

    /**
     * 从客户端缓存中获取玩家的真实名称 / Get player's real name from client cache
     *
     * @param player 伪装的玩家 / Disguised player
     * @return 真实的玩家名称，如果找不到则返回null / Real player name, null if not found
     */
    private static String getRealPlayerNameFromCache(PlayerEntity player) {
        try {
            // 从客户端缓存获取完整的GameProfile / Get complete GameProfile from client cache
            CompoundNBT profileTag = ClientDisguiseCache.getCachedProfile(player.getUUID());
            if (profileTag != null) {
                GameProfile profile = com.TheChaYe.rotp_yellowtemperance.util.GameProfileSerializer.deserialize(profileTag);
                if (profile != null && profile.getName() != null) {
                    return profile.getName();
                }
            }

            // 如果缓存中没有，尝试从NBT中的profile数据获取 / If not in cache, try getting from profile data in NBT
            if (player.getPersistentData().contains("disguiseProfile")) {
                CompoundNBT nbtProfile = player.getPersistentData().getCompound("disguiseProfile");
                GameProfile profile = com.TheChaYe.rotp_yellowtemperance.util.GameProfileSerializer.deserialize(nbtProfile);
                if (profile != null && profile.getName() != null) {
                    return profile.getName();
                }
            }

            return null;

        } catch (Exception e) {
//            RotPYellowTemperanceAddon.LOGGER.error("[NameTag] Error getting real player name from cache: {}", e.getMessage());
            return null;
        }
    }
}