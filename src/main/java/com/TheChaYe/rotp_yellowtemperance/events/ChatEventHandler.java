package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.util.GameProfileSerializer;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 聊天事件处理器 / Chat Event Handler
 * 处理玩家聊天时的伪装名称显示逻辑
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID)
public class ChatEventHandler {

    /**
     * 服务器聊天事件处理 / Server chat event handler
     * 处理玩家发送聊天消息时的伪装名称替换
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        PlayerEntity player = event.getPlayer();
        CompoundNBT nbt = player.getPersistentData();

        // 检查玩家是否正在伪装 / Check if player is disguised
        if (nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
            // 获取伪装的名字 / Get disguise name
            String disguiseName = nbt.getString(InitTags.YT_DISGUISE_NAME_TAG);
            if (!disguiseName.isEmpty()) {
                // 检查是否启用了搜索辅助功能 / Check if search helper is enabled
                // 通过Capability获取搜索辅助功能状态 / Get search helper status through Capability
                boolean searchHelperEnabled = player.getCapability(InitCapabilities.DISGUISE_CAPABILITY)
                        .map(cap -> cap.isSearchHelperEnabled())
                        .orElse(true); // 默认开启 / Default enabled

                // 添加调试日志 / Add debug logging
                RotPYellowTemperanceAddon.LOGGER.debug("Disguise chat - Search helper enabled: {}, disguise name: {}", searchHelperEnabled, disguiseName);

                String displayName = disguiseName;
                GameProfile profile = null;
                if (searchHelperEnabled) {
                    // 只读服务端已保存的 profile NBT，避免在服务器主线程做同步 HTTP 查询
                    CompoundNBT profileTag = nbt.contains(InitTags.YT_DISGUISE_PROFILE_TAG)
                            ? nbt.getCompound(InitTags.YT_DISGUISE_PROFILE_TAG)
                            : (nbt.contains("disguiseProfile") ? nbt.getCompound("disguiseProfile") : null);
                    if (profileTag != null) {
                        profile = GameProfileSerializer.deserialize(profileTag);
                    }
                    if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
                        displayName = profile.getName();
                    }
                }
                Style style = null;
                UUID uuid = profile != null ? profile.getId() : null;
                MinecraftServer server = event.getPlayer().getServer();
                if (server != null && uuid != null) {
                    PlayerEntity targetPlayer = server.getPlayerList().getPlayer(uuid);
                    if (targetPlayer != null) {
                        // 现在可以获取该玩家的显示名称样式
                        ITextComponent PlayerDisplayName = targetPlayer.getDisplayName();
                        style = PlayerDisplayName.getStyle();
                        // 之后你可以使用这个 style
                    }
                }
                ITextComponent newName = new StringTextComponent(displayName);
                if (style == null && uuid != null) {
                    // 创建 EntityHover（注意：EntityType.PLAYER 可直接使用）
                    HoverEvent.EntityHover entityHover = new HoverEvent.EntityHover(EntityType.PLAYER, uuid, newName);
                    // 创建 HoverEvent
                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_ENTITY, entityHover);
                    newName = new StringTextComponent(displayName)
                            .setStyle(Style.EMPTY.withHoverEvent(hoverEvent));

                } else if (style != null) {
                    newName = new StringTextComponent(displayName)
                            .setStyle(style);
                }

                ITextComponent component = new StringTextComponent("")
                        .append("<")
                        .append(newName)
                        .append("> ")
                        .append(event.getMessage());

                // 修改聊天消息中的玩家名字 / Modify player name in chat message
                event.setComponent(component);
            }
        }
    }
}