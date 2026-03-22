package com.TheChaYe.rotp_yellowtemperance.entity.stand;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.RemoveDisguisePacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.YellowTemperanceRenderPacket;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.init.power.stand.ModStandsInit;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public abstract class AbstractDisguiseStandEntity extends StandEntity {
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
    public float limbSwingAmount;
    /**
     * 上一帧肢体摆动幅度 / Previous limb swing amount
     */
    public float prevLimbSwingAmount;
    /**
     * 伪装清除延迟 / Disguise clear delayKhnumEntity
     */
    public int disguiseClearDelay = 0;
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
     * 构造函数 / Constructor
     * 初始化黄色节制替身实体
     */
    public AbstractDisguiseStandEntity(StandEntityType<? extends AbstractDisguiseStandEntity> type, World world) {
        super(type, world);
        unsummonOffset = getDefaultOffsetFromUser().copy();
    }

    public float getClientLimbSwing() {
        return clientLimbSwing;
    }

    public void setClientLimbSwing(float clientLimbSwing) {
        this.clientLimbSwing = clientLimbSwing;
    }

    public float getClientLimbSwingAmount() {
        return clientLimbSwingAmount;
    }

    public void setClientLimbSwingAmount(float clientLimbSwingAmount) {
        this.clientLimbSwingAmount = clientLimbSwingAmount;
    }

    public float getLimbSwing() {
        return limbSwing;
    }

    public void setLimbSwing(float limbSwing) {
        this.limbSwing = limbSwing;
    }

    public float getLimbSwingAmount() {
        return limbSwingAmount;
    }

    public void setLimbSwingAmount(float limbSwingAmount) {
        this.limbSwingAmount = limbSwingAmount;
    }

    public float getPrevLimbSwingAmount() {
        return prevLimbSwingAmount;
    }

    public void setPrevLimbSwingAmount(float prevLimbSwingAmount) {
        this.prevLimbSwingAmount = prevLimbSwingAmount;
    }

    public float getLAST_SHAPESHIFT() {
        return LAST_SHAPESHIFT;
    }

    public void setLAST_SHAPESHIFT(float LAST_SHAPESHIFT) {
        this.LAST_SHAPESHIFT = LAST_SHAPESHIFT;
    }

    public boolean isDISGUISED() {
        return DISGUISED;
    }

    public void setDISGUISED(boolean DISGUISED) {
        this.DISGUISED = DISGUISED;
    }

    public Optional<EntityType<?>> getDISGUISE_ENTITY() {
        return DISGUISE_ENTITY;
    }

    public void setDISGUISE_ENTITY(Optional<EntityType<?>> DISGUISE_ENTITY) {
        this.DISGUISE_ENTITY = DISGUISE_ENTITY;
    }

    public boolean isUSER_DISGUISED() {
        return USER_DISGUISED;
    }

    public void setUSER_DISGUISED(boolean USER_DISGUISED) {
        this.USER_DISGUISED = USER_DISGUISED;
    }

    public Optional<EntityType<?>> getUSER_DISGUISE_ENTITY() {
        return USER_DISGUISE_ENTITY;
    }

    public void setUSER_DISGUISE_ENTITY(Optional<EntityType<?>> USER_DISGUISE_ENTITY) {
        this.USER_DISGUISE_ENTITY = USER_DISGUISE_ENTITY;
    }

    public int getDisguiseClearDelay() {
        return disguiseClearDelay;
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
     * 获取替身伪装实体类型 / Get stand disguise entity type
     *
     * @return 伪装的实体类型 / Disguised entity type
     */
    public Optional<EntityType<?>> getDisguiseEntity() {
        return getDISGUISE_ENTITY();
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
            if (!DISGUISED) {
                this.addEffect(new EffectInstance(ModStatusEffects.FULL_INVISIBILITY.get(), 20, 0, false, false));
            }
            if (user != null) {
                // 替身解除时清除伪装 - 使用与召唤替身时相同的逻辑 / Clear disguise when stand is dismissed - using same logic as summon
                if (this.getCurrentTaskAction() == ModStandsInit.UNSUMMON_STAND_ENTITY.get()) {
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
     * 同步伪装状态到客户端 / Sync disguise state to clients
     * 向所有追踪客户端发送伪装状态更新
     */
    public void syncDisguiseStateToClients() {
        syncDisguiseStateToClients(false, false);
    }

    /**
     * 同步伪装状态到客户端（带清除标志）/ Sync disguise state to clients with clear flags
     * 向所有追踪客户端发送伪装状态更新，可以指定清除哪种伪装
     *
     * @param clearUserDisguise  是否清除用户伪装 / Whether to clear user disguise
     * @param clearStandDisguise 是否清除替身伪装 / Whether to clear stand disguise
     */
    public void syncDisguiseStateToClients(boolean clearUserDisguise, boolean clearStandDisguise) {
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
                            limbSwing,
                            limbSwingAmount,
                            yHeadRot,
                            xRot,
                            clearUserDisguise,
                            clearStandDisguise
                    ),
                    this
            );
        }
    }

    public void setDisguisedOnce(boolean value) {
        this.DISGUISED = value;
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
     * 清除所有伪装状态 / Clear all disguise states
     * 重置替身和用户的伪装状态
     */
    public void clearDisguises() {
        clearDisguises(false);
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
        USER_DISGUISED = false;
        USER_DISGUISE_ENTITY = Optional.empty();
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
     * 清除替身伪装状态 / Clear stand disguise state
     * 只清除替身自身的伪装，不影响用户伪装
     */
    public void clearDisguisesStand() {
        setDisguisedOnce(false);
        setEntityForDisguise(null);
        if (!level.isClientSide()) {
            syncDisguiseStateToClients(false, true);
        }
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
        clearDisguisesStand();
        clearDisguisePlayer();

    }

    public void clearDisguisePlayer() {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> clearDisguisePlayerClient());
        } else {
            PlayerEntity player = (PlayerEntity) getUser();
            if (player != null) {
                CompoundNBT nbt = player.getPersistentData();
                nbt.remove(InitTags.YT_HAS_DISGUISE_TAG);
                nbt.remove(InitTags.YT_DISGUISE_NAME_TAG);
                RotPYellowTemperanceAddon.LOGGER.info("Clearing disguise for player: " + player.getName().getString());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void clearDisguisePlayerClient() {
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = (PlayerEntity) getUser();
        if (mc.player != null && player != null && mc.player.getUUID().equals(player.getUUID())) {
            PacketHandler.CHANNEL.sendToServer(new RemoveDisguisePacket());
        }
    }

    /**
     * 移除实体 / Remove entity
     * 实体被销毁时的清理逻辑
     */
    @Override
    public void remove() {
        // 替身被移除时清除效果和伪装状态 / Clear effects and disguise state when stand is removed
        if (getUser() != null) {
            clearBothDisguises();
        }
        super.remove();
    }
}
