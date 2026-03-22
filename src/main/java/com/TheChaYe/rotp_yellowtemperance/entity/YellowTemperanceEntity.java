package com.TheChaYe.rotp_yellowtemperance.entity;

import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.RemoveDisguisePacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.YellowTemperanceRenderPacket;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandRelativeOffset;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.init.power.stand.ModStandsInit;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;


/**
 * 黄色节制替身实体 / Yellow Temperance Stand Entity
 * 核心替身实体类，处理伪装、保护效果和动画同步
 */
public class YellowTemperanceEntity extends StandEntity {
    /**
     * 默认偏移量 / Default offset
     */
    private final StandRelativeOffset offsetDefault = StandRelativeOffset.withYOffset(0, 0, 0);
    /**
     * 客户端肢体摆动值 / Client limb swing value
     */
    @OnlyIn(Dist.CLIENT)
    public float clientLimbSwing;
    /**
     * 客户端肢体摆动幅度 / Client limb swing amount
     */
    @OnlyIn(Dist.CLIENT)
    public float clientLimbSwingAmount;
    /**
     * 肢体摆动值 / Limb swing value
     */
    public float limbSwing;
    /**
     * 肢体摆动幅度 / Limb swing amount
     */
    public float limbSwingAmount;
    /**
     * 上一帧肢体摆动幅度 / Previous limb swing amount
     */
    public float prevLimbSwingAmount;
    /**
     * 替身变形时间戳 / Stand shapeshift timestamp
     */
    private float LAST_SHAPESHIFT = 0f;
    /**
     * 替身是否被伪装 / Whether stand is disguised
     */
    private boolean DISGUISED = false;
    /**
     * 替身伪装实体类型 / Stand disguise entity type
     */
    private Optional<EntityType<?>> DISGUISE_ENTITY = Optional.empty();
    /**
     * 用户是否被伪装 / Whether user is disguised
     */
    private boolean USER_DISGUISED = false;
    /**
     * 用户伪装实体类型 / User disguise entity type
     */
    private Optional<EntityType<?>> USER_DISGUISE_ENTITY = Optional.empty();
    /**
     * 伪装清除延迟 / Disguise clear delay
     */
    private int disguiseClearDelay = 0;

    /**
     * 构造函数 / Constructor
     * 初始化黄色节制替身实体
     */
    public YellowTemperanceEntity(StandEntityType<YellowTemperanceEntity> type, World world) {
        super(type, world);
        unsummonOffset = getDefaultOffsetFromUser().copy();
    }

    /**
     * 设置伪装清除延迟 / Set disguise clear delay
     *
     * @param ticks 延迟的tick数 / Delay in ticks
     */
    public void setDisguiseClearDelay(int ticks) {
        this.disguiseClearDelay = ticks;
    }

    /**
     * 实体tick方法 / Entity tick method
     * 处理替身的每帧逻辑，包括隐身、保护效果和伪装同步
     */
    @Override
    public void tick() {
        super.tick();
        LivingEntity user = getUser();


        // 服务端逻辑 / Server-side logic
        if (!level.isClientSide) {
            // 添加自身隐身效果 / Add self invisibility effect
            this.addEffect(new EffectInstance(ModStatusEffects.FULL_INVISIBILITY.get(), 20, 0, false, false));

            if (user != null) {
                // 根据用户饥饿值添加保护效果 / Add protection effect based on user hunger
                addProtectionEffectBasedOnHunger(user);

                // 替身解除时清除伪装 - 使用与召唤替身时相同的逻辑 / Clear disguise when stand is dismissed - using same logic as summon
                if (this.getCurrentTaskAction() == ModStandsInit.UNSUMMON_STAND_ENTITY.get()) {
                    removeProtectionEffect(user);
                    // 使用与召唤替身时相同的伪装清除逻辑 / Use same disguise clearing logic as summon
                    if (isUserDisguised() || getUserDisguiseEntity().isPresent()) {
                        clearBothDisguises(); // 使用现有的清除伪装方法 / Use existing disguise clearing method

                    }
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
        if (!level.isClientSide) {
            if (disguiseClearDelay > 0) {
                disguiseClearDelay--;
                if (disguiseClearDelay == 0) {
                    clearDisguises();// 延迟后再次清除伪装 / Clear disguise again after delay
                }
            }
        }
        // 更新limbSwing相关属性 / Update limbSwing related properties
        LivingEntity userEntity = this.getUser();
        if (!this.level.isClientSide && userEntity != null) {
            this.limbSwing = userEntity.animationPosition;
            this.limbSwingAmount = userEntity.animationSpeed;
            this.prevLimbSwingAmount = userEntity.animationSpeedOld;
        }
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
    public void syncDisguiseStateToClients() {
        if (!level.isClientSide) {
            // 获取伪装资源（可能为null） / Get disguise resource (may be null)
            ResourceLocation disguiseRes = getUserDisguiseEntity()
                    .map(entityType -> ForgeRegistries.ENTITIES.getKey(entityType))
                    .orElse(null);

            // 始终发送网络包到所有追踪实体的客户端，即使没有伪装 / Always send network packet to all tracking clients, even without disguise
            PacketHandler.sendToClientsTrackingAndSelf(
                    new YellowTemperanceRenderPacket(
                            disguiseRes,
                            getId(),
                            clientLimbSwing,
                            clientLimbSwingAmount,
                            yHeadRot,
                            xRot
                    ),
                    this
            );
        }
    }

    /**
     * 移除保护效果 / Remove protection effect
     * 清除用户的黄色节制保护效果
     */
    private void removeProtectionEffect(LivingEntity user) {
        user.removeEffect(InitEffects.YELLOW_TEMPERANCE_PROTECTION.get());
    }

    /**
     * 判断实体是否可被选取 / Determine if entity is pickable
     * 替身实体不可被玩家点击选取
     */
    @Override
    public boolean isPickable() {
        return false;
    }

    /**
     * 获取相对于用户的默认偏移量 / Get default offset from user
     *
     * @return 相对于用户的站立位置偏移 / Offset relative to user's standing position
     */
    public StandRelativeOffset getDefaultOffsetFromUser() {
        return offsetDefault;
    }

    /**
     * 替身伪装状态管理 / Stand disguise state management
     * 处理替身自身的伪装状态
     */
    public boolean isDisguisedOnce() {
        return this.DISGUISED;
    }

    public void setDisguisedOnce(boolean value) {
        this.DISGUISED = value;
    }

    /**
     * 获取替身伪装实体类型 / Get stand disguise entity type
     *
     * @return 伪装的实体类型 / Disguised entity type
     */
    public Optional<EntityType<?>> getDisguiseEntity() {
        return this.DISGUISE_ENTITY;
    }

    /**
     * 获取伪装实体的别名方法 / Alias method to get disguise entity
     * 保持向后兼容性 / Maintain backward compatibility
     */
    public Optional<EntityType<?>> getEntityForDisguise() {
        return getDisguiseEntity();
    }

    /**
     * 设置替身伪装实体 / Set stand disguise entity
     *
     * @param entity 要伪装成的实体类型 / Entity type to disguise as
     */
    public void setEntityForDisguise(EntityType<?> entity) {
        this.DISGUISE_ENTITY = Optional.ofNullable(entity);
    }

    /**
     * 获取变形时间戳 / Get shapeshift timestamp
     *
     * @return 上次变形的时间tick / Timestamp of last shapeshift
     */
    public float getShapeshiftTick() {
        return this.LAST_SHAPESHIFT;
    }

    /**
     * 设置变形时间戳 / Set shapeshift timestamp
     *
     * @param tick 时间戳tick值 / Timestamp tick value
     */
    public void setShapeshiftTick(float tick) {
        this.LAST_SHAPESHIFT = tick;
    }

    /**
     * 计算变形时间差 / Calculate shapeshift time difference
     *
     * @param now 当前时间tick / Current time tick
     * @return 时间差值 / Time difference value
     */
    public float shapeshiftTickDifference(float now) {
        return now - this.getShapeshiftTick();
    }

    /**
     * 用户伪装状态管理 / User disguise state management
     * 处理玩家被伪装的状态
     */
    public boolean isUserDisguised() {
        return this.USER_DISGUISED;
    }

    public void setUserDisguised(boolean value) {
        this.USER_DISGUISED = value;
    }

    /**
     * 获取用户伪装实体类型 / Get user disguise entity type
     *
     * @return 用户伪装的实体类型 / User disguised entity type
     */
    public Optional<EntityType<?>> getUserDisguiseEntity() {
        return this.USER_DISGUISE_ENTITY;
    }

    /**
     * 设置用户伪装实体 / Set user disguise entity
     *
     * @param entity 要伪装成的实体类型 / Entity type to disguise as
     */
    public void setUserDisguiseEntity(EntityType<?> entity) {
        this.USER_DISGUISE_ENTITY = Optional.ofNullable(entity);
    }

    /**
     * 伪装控制方法 / Disguise control methods
     * 统一管理伪装状态的设置和清除
     */
    public void disguiseBothAs(EntityType<?> entityType) {
        // 设置玩家伪装 / Set player disguise
        setUserDisguised(true);
        setUserDisguiseEntity(entityType);

        // 清除替身伪装 / Clear stand disguise
        setDisguisedOnce(false);
        setEntityForDisguise(null);

        // 立即同步到客户端 / Immediately sync to clients
        syncDisguiseStateToClients();
    }

    /**
     * 清除所有伪装状态 / Clear all disguise states
     * 重置替身和用户的伪装状态
     */
    public void clearDisguises() {
        setDisguisedOnce(false);
        setUserDisguised(false);
        setUserDisguiseEntity(null);
        setEntityForDisguise(null);
        USER_DISGUISED = false;
        USER_DISGUISE_ENTITY = Optional.empty();
        if (!level.isClientSide() && getUser() instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) getUser();
            com.TheChaYe.rotp_yellowtemperance.server.ServerDisguiseHandler.restorePlayerPhysicsOnServer(serverPlayer);
            syncDisguiseStateToClients();

        }
//        if (getUser() instanceof ServerPlayerEntity) {
//            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) getUser();
//            serverPlayer.displayClientMessage(new StringTextComponent("1"), false);
//        }
    }

    /**
     * 延迟清除伪装 / Clear disguises with delay
     * 先清除伪装然后设置延迟
     */
    public void clearDisguisesDouble() {
        clearDisguises();
        setDisguiseClearDelay(2);

    }

    /**
     * 清除双方伪装 / Clear both disguises
     * 客户端发送移除伪装包，服务端执行清除逻辑
     */
    public void clearBothDisguises() {
        clearDisguisesDouble();

        PacketHandler.CHANNEL.sendToServer(new RemoveDisguisePacket());
        PlayerEntity player = (PlayerEntity) getUser();
        CompoundNBT nbt = player.getPersistentData();
        nbt.remove(InitTags.YT_HAS_DISGUISE_TAG);
        nbt.remove(InitTags.YT_DISGUISE_NAME_TAG);
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