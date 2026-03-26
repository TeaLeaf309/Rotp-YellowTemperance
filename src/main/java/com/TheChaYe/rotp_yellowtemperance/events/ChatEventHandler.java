package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.util.GameProfileFetcher;
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
@Mod.EventBusSubscriber
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
                RotPYellowTemperanceAddon.LOGGER.info("Disguise chat - Search helper enabled: {}, disguise name: {}", searchHelperEnabled, disguiseName);

                String displayName = disguiseName;
                // 只有在搜索辅助功能开启时才使用GameProfileFetcher获取正确的玩家名 / Only use GameProfileFetcher when search helper is enabled
                if (searchHelperEnabled) {
                    RotPYellowTemperanceAddon.LOGGER.info("Using GameProfileFetcher to get real player name for: {}", disguiseName);
                    // 使用GameProfileFetcher获取正确的玩家名 / Use GameProfileFetcher to get correct player name
                    GameProfile profile = GameProfileFetcher.getProfile(disguiseName);
                    if (profile != null) {
                        displayName = profile.getName();
                        RotPYellowTemperanceAddon.LOGGER.info("GameProfileFetcher returned name: {}", displayName);
                    }
                } else {
                    RotPYellowTemperanceAddon.LOGGER.info("Not using GameProfileFetcher, using disguise name directly: {}", displayName);
                }
                Style style = null;
                GameProfile profile = GameProfileFetcher.getProfile(disguiseName);
                UUID uuid = profile.getId(); // 这就是获取的 UUID
                MinecraftServer server = event.getPlayer().getServer();
                if (server != null) {
                    PlayerEntity targetPlayer = server.getPlayerList().getPlayer(uuid);
                    if (targetPlayer != null) {
                        // 现在可以获取该玩家的显示名称样式
                        ITextComponent PlayerDisplayName = targetPlayer.getDisplayName();
                        style = PlayerDisplayName.getStyle();
                        // 之后你可以使用这个 style
                    }
                }
                ITextComponent newName = new StringTextComponent(displayName);
                // 创建 EntityHover（注意：EntityType.PLAYER 可直接使用）
                HoverEvent.EntityHover entityHover = new HoverEvent.EntityHover(EntityType.PLAYER, uuid, newName);
                // 创建 HoverEvent
                HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_ENTITY, entityHover);
                newName = new StringTextComponent(displayName)
                        .setStyle(Style.EMPTY.withHoverEvent(hoverEvent));
                if (style != null) {
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