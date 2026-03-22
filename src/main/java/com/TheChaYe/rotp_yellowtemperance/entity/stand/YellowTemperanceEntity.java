// 原代码来自：https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// 修改日期：2026-03-19
// Original code from: https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// on 2026-03-19
package com.TheChaYe.rotp_yellowtemperance.entity.stand;

import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.init.power.stand.ModStandsInit;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.World;

import java.util.Optional;


/**
 * 黄色节制替身实体 / Yellow Temperance Stand Entity
 * 核心替身实体类，处理伪装、保护效果和动画同步
 */
public class YellowTemperanceEntity extends AbstractDisguiseStandEntity {
    public YellowTemperanceEntity(StandEntityType<YellowTemperanceEntity> type, World world) {
        super(type, world);
        unsummonOffset = getDefaultOffsetFromUser().copy();
    }

    /**
     * 实体tick方法 / Entity tick method
     * 处理替身的每帧逻辑，包括隐身、保护效果和伪装同步
     */
    @Override
    public void tick() {
        super.tick();
        LivingEntity user = getUser();
        if (!isDISGUISED()) {
            this.addEffect(new EffectInstance(ModStatusEffects.FULL_INVISIBILITY.get(), 20, 0, false, false));
        }
        // 服务端逻辑 / Server-side logic
        if (!level.isClientSide && user != null) {
            // 根据用户饥饿值添加保护效果 / Add protection effect based on user hunger
            addProtectionEffectBasedOnHunger(user);

            // 替身解除时清除伪装 - 使用与召唤替身时相同的逻辑 / Clear disguise when stand is dismissed - using same logic as summon
            if (this.getCurrentTaskAction() == ModStandsInit.UNSUMMON_STAND_ENTITY.get()) {
                removeProtectionEffect(user);
            }
        }
        // 定期向客户端同步伪装状态，但只在玩家被伪装时才同步 / Periodically sync disguise state to clients, only when player is disguised
        if (tickCount % 20 == 0) { // 每秒同步一次 / Sync every second
            // 只在玩家被伪装时才同步，避免发送空数据 / Only sync when player is disguised to avoid sending empty data
            if (isUserDisguised() && getUserDisguiseEntity().isPresent()) {
                syncDisguiseStateToClients();
            }
        }
    }


    /**
     * 设置变形时间戳 / Set shapeshift timestamp
     *
     * @param tick 时间戳tick值 / Timestamp tick value
     */
    public void setShapeshiftTick(float tick) {
        setLAST_SHAPESHIFT(tick);
    }

    /**
     * 根据饥饿值添加保护效果 / Add protection effect based on hunger
     * 根据玩家饥饿值动态调整保护效果等级
     */
    private void addProtectionEffectBasedOnHunger(LivingEntity user) {
        // 只有玩家才有饥饿值概念 / Only players have hunger concept
        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            int foodLevel = player.getFoodData().getFoodLevel();
            final int maxFoodLevel = 20; // Minecraft中玩家最大饥饿值为20 / Max hunger level in Minecraft is 20

            // 计算保护等级 - 使用百分比 / Calculate protection level - using percentage
            int protectionLevel;
            float foodPercentage = (float) foodLevel / maxFoodLevel;

            if (foodPercentage > 0.75f) { // 大于四分之三 / Greater than three quarters
                protectionLevel = 3;
            } else if (foodPercentage > 0.5f) { // 大于二分之一 / Greater than half
                protectionLevel = 2;
            } else if (foodPercentage > 0.25f) { // 大于四分之一 / Greater than one quarter
                protectionLevel = 1;
            } else if (foodLevel > 0) { // 大于0 / Greater than 0
                protectionLevel = 0;
            } else { // 小于等于0 / Less than or equal to 0
                // 移除保护效果而不是添加 / Remove protection effect instead of adding
                if (user.hasEffect(InitEffects.YELLOW_TEMPERANCE_PROTECTION.get())) {
                    user.removeEffect(InitEffects.YELLOW_TEMPERANCE_PROTECTION.get());
                }
                return;
            }

            // 添加或更新保护效果 / Add or update protection effect
            user.addEffect(new EffectInstance(
                    InitEffects.YELLOW_TEMPERANCE_PROTECTION.get(),
                    2, // 持续时间保持不变 / Duration remains unchanged
                    protectionLevel,
                    false, false, true
            ));
        } else {
            // 对于非玩家实体，保留原来的行为 / For non-player entities, retain original behavior
            if (!user.hasEffect(InitEffects.YELLOW_TEMPERANCE_PROTECTION.get())) {
                user.addEffect(new EffectInstance(
                        InitEffects.YELLOW_TEMPERANCE_PROTECTION.get(),
                        2,
                        2,
                        false, false, true
                ));
            }
        }
    }

    /**
     * 同步伪装状态到客户端 / Sync disguise state to clients
     * 向所有追踪客户端发送伪装状态更新
     */
    @Override
    public void syncDisguiseStateToClients() {
        super.syncDisguiseStateToClients();
    }

    /**
     * 同步伪装状态到客户端（带清除标志）/ Sync disguise state to clients with clear flags
     * 向所有追踪客户端发送伪装状态更新，可以指定清除哪种伪装
     *
     * @param clearUserDisguise  是否清除用户伪装 / Whether to clear user disguise
     * @param clearStandDisguise 是否清除替身伪装 / Whether to clear stand disguise
     */
    @Override
    public void syncDisguiseStateToClients(boolean clearUserDisguise, boolean clearStandDisguise) {
        super.syncDisguiseStateToClients(clearUserDisguise, clearStandDisguise);
    }

    /**
     * 移除保护效果 / Remove protection effect
     * 清除用户的黄色节制保护效果
     */
    private void removeProtectionEffect(LivingEntity user) {
        user.removeEffect(InitEffects.YELLOW_TEMPERANCE_PROTECTION.get());
    }

    /**
     * 获取伪装实体的别名方法 / Alias method to get disguise entity
     * 保持向后兼容性 / Maintain backward compatibility
     */
    @Override
    public void setEntityForDisguise(EntityType<?> entity) {
        super.setEntityForDisguise(entity);
    }

    /**
     * 清除用户伪装状态 / Clear user disguise state
     * 可以选择是否立即同步到客户端
     *
     * @param syncImmediately 是否立即同步到客户端 / Whether to sync to clients immediately
     */
    public void clearDisguises(boolean syncImmediately) {
        setUserDisguised(false);
        setUserDisguiseEntity(null);
        setUSER_DISGUISED(false);
        setUSER_DISGUISE_ENTITY(Optional.empty());
        if (!level.isClientSide() && getUser() instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) getUser();
            com.TheChaYe.rotp_yellowtemperance.server.ServerDisguiseHandler.restorePlayerPhysicsOnServer(serverPlayer);
            if (syncImmediately) {
                syncDisguiseStateToClients(true, false);
            } else {
                syncDisguiseStateToClients();
            }
        }
//        if (getUser() instanceof ServerPlayerEntity) {
//            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) getUser();
//            serverPlayer.displayClientMessage(new StringTextComponent("1"), false);
//        }
    }

    /**
     * 移除实体 / Remove entity
     * 实体被销毁时的清理逻辑
     */
    @Override
    public void remove() {
        // 替身被移除时清除效果和伪装状态 / Clear effects and disguise state when stand is removed
        if (getUser() != null) {
            removeProtectionEffect(getUser());
            clearBothDisguises();
        }

        super.remove();
    }

}