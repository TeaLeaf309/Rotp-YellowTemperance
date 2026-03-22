/**
 * 实体能力处理器 / Entity Capability Handler
 * 管理实体能力的注册、附加和同步逻辑
 * Manages registration, attachment, and synchronization logic for entity capabilities
 *
 * @author TheChaYe
 * @version 1.0
 */
package com.TheChaYe.rotp_yellowtemperance.capability.entity;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * 实体能力处理的核心类 / Core class for entity capability handling
 * 使用Forge的能力系统为实体添加自定义数据存储
 * Uses Forge's capability system to add custom data storage for entities
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID)
public class CapabilityHandler {

    /**
     * 能力ID，用于标识此能力的唯一标识符 / Capability ID for unique identification
     */
    private static final ResourceLocation CAPABILITY_ID =
            new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "living_data");

    /**
     * 注入LivingData能力类型 / Inject LivingData capability type
     * 用于在运行时获取LivingData能力实例
     * Used to get LivingData capability instances at runtime
     */
    @CapabilityInject(LivingData.class)
    public static Capability<LivingData> LIVING_DATA_CAPABILITY;

    /**
     * 注册LivingData能力 / Register LivingData capability
     * 定义能力的数据序列化和反序列化方式
     * Defines data serialization and deserialization methods for the capability
     */
    public static void register() {
        CapabilityManager.INSTANCE.register(LivingData.class, new Capability.IStorage<LivingData>() {

            /**
             * 将LivingData实例序列化为NBT格式 / Serialize LivingData instance to NBT format
             * @param capability 能力对象 / Capability object
             * @param instance 当前实例 / Current instance
             * @param side 方向（可为空） / Direction (can be null)
             * @return 序列化的NBT数据 / Serialized NBT data
             */
            @Override
            public INBT writeNBT(Capability<LivingData> capability, LivingData instance, @Nullable Direction side) {
                return instance.serializeNBT();
            }

            /**
             * 从NBT数据反序列化到LivingData实例 / Deserialize from NBT data to LivingData instance
             * @param capability 能力对象 / Capability object
             * @param instance 目标实例 / Target instance
             * @param side 方向（可为空） / Direction (can be null)
             * @param nbt NBT数据 / NBT data
             */
            @Override
            public void readNBT(Capability<LivingData> capability, LivingData instance, @Nullable Direction side, INBT nbt) {
                if (nbt instanceof CompoundNBT) {
                    instance.deserializeNBT((CompoundNBT) nbt);
                }
            }
        }, LivingData::new);
    }

    /**
     * 为实体附加能力 / Attach capability to entity
     * 当实体被创建或加载时调用，为LivingEntity附加自定义能力
     * Called when entity is created or loaded, attaches custom capability to LivingEntity
     *
     * @param event 附加能力事件 / Attach capability event
     */
    @SubscribeEvent
    public static void onAttachCapabilitiesEntity(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof LivingEntity) {
            event.addCapability(CAPABILITY_ID, new LivingData.Provider((LivingEntity) entity));
        }
    }

    /**
     * 处理玩家克隆事件（如死亡后重生） / Handle player clone event (such as respawning after death)
     * 在玩家死亡并复活时复制原有能力数据，并重置侵蚀状态
     * Copies original capability data and resets erosion state when player dies and respawns
     *
     * @param event 玩家克隆事件 / Player clone event
     */
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // 玩家死亡后复制能力数据 / Copy capability data after player death
            if (LIVING_DATA_CAPABILITY == null) return;
            LivingData originalData = event.getOriginal().getCapability(LIVING_DATA_CAPABILITY).orElse(null);
            LivingData newData = event.getPlayer().getCapability(LIVING_DATA_CAPABILITY).orElse(null);
            if (originalData != null && newData != null) {
                newData.deserializeNBT(originalData.serializeNBT());
                // 死亡后重置侵蚀状态 / Reset erosion state after death
                newData.setErosionActive(false);
                newData.setErosionLevel(0);
            }
        }
    }

    /**
     * 同步新玩家与目标实体的能力数据 / Synchronize capability data between new player and target entity
     * 当玩家开始追踪某个实体时，将该实体的能力数据同步给玩家
     * When player starts tracking an entity, synchronize that entity's capability data to the player
     *
     * @param event 开始追踪事件 / Start tracking event
     */
    @SubscribeEvent
    public static void syncWithNewPlayer(PlayerEvent.StartTracking event) {
        if (LIVING_DATA_CAPABILITY == null) return;
        Entity entityTracked = event.getTarget();
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity trackingPlayer = (ServerPlayerEntity) event.getPlayer();
            if (entityTracked instanceof LivingEntity) {
                entityTracked.getCapability(LIVING_DATA_CAPABILITY).ifPresent(livingData -> {
                    livingData.syncWithAnyPlayer(trackingPlayer);
                });
            }
        }
    }
}
