package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家渲染控制器 / Player Render Controller
 * 专门处理玩家模型的显示/隐藏控制，解决渲染一致性问题
 */
@OnlyIn(Dist.CLIENT)
public class PlayerRenderController {
    private static final Logger LOGGER = LogManager.getLogger();

    // 跟踪每个玩家的模型状态
    private static final Map<UUID, ModelVisibilityState> PLAYER_MODEL_STATES = new ConcurrentHashMap<>();

    /**
     * 在渲染前预处理玩家模型可见性
     */
    public static void preprocessPlayerModel(PlayerEntity player, PlayerModel<?> model) {
        UUID playerId = player.getUUID();
        CompoundNBT nbt = player.getPersistentData();

        ModelVisibilityState state = PLAYER_MODEL_STATES.computeIfAbsent(playerId,
                k -> new ModelVisibilityState());

        if (nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
            // 有伪装时隐藏原始模型
            if (shouldHideOriginalModel(nbt)) {
                hidePlayerModel(model, state);
//                LOGGER.debug("[YT RENDER CTRL] Hidden model for disguised player: {} - State: {}",
//                        player.getName().getString(), state);
            } else {
                // 仍在加载中，保持可见但记录状态
                showPlayerModel(model, state);
//                LOGGER.debug("[YT RENDER CTRL] Keeping model visible for loading player: {} - State: {}",
//                        player.getName().getString(), state);
            }
        } else {
            // 无伪装时显示原始模型
            showPlayerModel(model, state);
//            LOGGER.debug("[YT RENDER CTRL] Showing model for non-disguised player: {} - State: {}",
//                    player.getName().getString(), state);
        }

        state.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 判断是否应该隐藏原始模型
     */
    private static boolean shouldHideOriginalModel(CompoundNBT nbt) {
        // 检查是否仍在加载中
        if (nbt.contains(InitTags.YT_DISGUISE_LOADING_TAG) &&
                nbt.getBoolean(InitTags.YT_DISGUISE_LOADING_TAG)) {
            return false;
        }

        // 检查是否有有效的伪装数据（支持新旧键名）
        return nbt.contains("disguiseProfile") || nbt.contains(InitTags.YT_DISGUISE_PROFILE_TAG);
    }

    /**
     * 隐藏玩家模型的所有部件
     */
    private static void hidePlayerModel(PlayerModel<?> model, ModelVisibilityState state) {
        // 保存当前状态
        state.headVisible = model.head.visible;
        state.bodyVisible = model.body.visible;
        state.armsVisible = model.rightArm.visible;
        state.legsVisible = model.rightLeg.visible;

        // 隐藏所有部件
        model.head.visible = false;
        model.hat.visible = false;
        model.body.visible = false;
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        model.rightLeg.visible = false;
        model.leftLeg.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
    }

    /**
     * 显示玩家模型的所有部件
     */
    private static void showPlayerModel(PlayerModel<?> model, ModelVisibilityState state) {
        model.head.visible = true;
        model.hat.visible = true;
        model.body.visible = true;
        model.rightArm.visible = true;
        model.leftArm.visible = true;
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        model.jacket.visible = true;
        model.leftSleeve.visible = true;
        model.rightSleeve.visible = true;
        model.leftPants.visible = true;
        model.rightPants.visible = true;

        state.headVisible = true;
        state.bodyVisible = true;
        state.armsVisible = true;
        state.legsVisible = true;
    }

    /**
     * 清理指定玩家的状态
     */
    public static void clearPlayerState(UUID playerId) {
        PLAYER_MODEL_STATES.remove(playerId);
//        LOGGER.debug("[YT RENDER CTRL] Cleared state for player: {}", playerId);
    }

    /**
     * 清理所有状态（游戏退出时调用）
     */
    public static void clearAllStates() {
        PLAYER_MODEL_STATES.clear();
//        LOGGER.debug("[YT RENDER CTRL] Cleared all player states");
    }

    /**
     * 获取玩家当前的模型状态（用于调试）
     */
    public static ModelVisibilityState getPlayerModelState(UUID playerId) {
        return PLAYER_MODEL_STATES.get(playerId);
    }

    /**
     * 模型可见性状态
     */
    public static class ModelVisibilityState {
        public boolean headVisible = true;
        public boolean bodyVisible = true;
        public boolean armsVisible = true;
        public boolean legsVisible = true;
        public long lastUpdateTime = System.currentTimeMillis();

        @Override
        public String toString() {
            return String.format("Head: %s, Body: %s, Arms: %s, Legs: %s",
                    headVisible, bodyVisible, armsVisible, legsVisible);
        }
    }
}