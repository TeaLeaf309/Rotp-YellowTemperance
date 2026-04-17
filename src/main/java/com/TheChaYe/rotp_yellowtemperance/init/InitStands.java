package com.TheChaYe.rotp_yellowtemperance.init;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.action.Disguise.*;
import com.TheChaYe.rotp_yellowtemperance.action.*;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.KhnumEntity;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.YellowTemperanceEntity;
import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.init.power.stand.EntityStandRegistryObject;
import com.github.standobyte.jojo.init.power.stand.ModStandsInit;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.power.impl.stand.type.EntityStandType;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/**
 * 替身初始化 / Stands Initialization
 * 注册和管理黄色节制替身及其相关动作
 */
public class InitStands {
    /**
     * 动作注册器 / Actions registry
     */
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<Action<?>> ACTIONS = DeferredRegister.create(
            (Class<Action<?>>) ((Class<?>) Action.class), RotPYellowTemperanceAddon.MOD_ID);
    /**
     * 替身类型注册器 / Stand types registry
     */
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<StandType<?>> STANDS = DeferredRegister.create(
            (Class<StandType<?>>) ((Class<?>) StandType.class), RotPYellowTemperanceAddon.MOD_ID);

    // ======================================== 黄色节制替身 ========================================


    /**
     * 黄色节制拳击动作 / Yellow Temperance punch attack
     */
    public static final RegistryObject<StandEntityLightAttack> YELLOW_TEMPERANCE_PUNCH = ACTIONS.register("yellow_temperance_punch",
            () -> new YellowTemperancePunch(new StandEntityLightAttack.Builder().staminaCost(10)
                    .swingHand()));


    /**
     * 黄色节制格挡动作 / Yellow Temperance block action
     */
    public static final RegistryObject<StandAction> YELLOW_TEMPERANCE_BLOCK = ACTIONS.register("yellow_temperance_block",
            () -> new YellowTemperanceBlock(new StandAction.Builder()
                    .holdType()
                    .cooldown(10)
            ));
    /**
     * 黄色节制吸收动作 / Yellow Temperance absorption action
     */
    public static final RegistryObject<StandAction> YELLOW_TEMPERANCE_ABSORPTION = ACTIONS.register("yellow_temperance_absorption",
            () -> new YellowTemperanceAbsorption(new StandAction.Builder()
                    .holdType()
                    .cooldown(10)
                    .resolveLevelToUnlock(1)
            ));

    /**
     * 黄色节制清除Debuff动作 / Yellow Temperance clear debuffs action
     */
    public static final RegistryObject<StandAction> YELLOW_TEMPERANCE_CLEAR_DEBUFFS = ACTIONS.register("yellow_temperance_clear_debuffs",
            () -> new YellowTemperanceClearDebuffs(new StandAction.Builder()
                    .cooldown(100) // 5秒冷却 / 5 second cooldown
                    .staminaCost(200) // 消耗200点体力 / Consume 200 stamina points
                    .holdToFire(20, false)
                    .shiftVariationOf(YELLOW_TEMPERANCE_ABSORPTION)
                    .resolveLevelToUnlock(3)
            ));

    /**
     * 黄色节制伪装动作 / Yellow Temperance disguise action
     */
    public static final RegistryObject<Disguise> YELLOW_TEMPERANCE_DISGUISE = ACTIONS.register("yellow_temperance_disguise",
            () -> new Disguise(new Disguise.Builder()
                    .resolveLevelToUnlock(2)
            ));
    /**
     * 黄色节制取消伪装动作 / Yellow Temperance disable disguise action
     */
    public static final RegistryObject<DisguiseDisable> YELLOW_TEMPERANCE_DISGUISE_DISABLE = ACTIONS.register("yellow_temperance_disguise_disable",
            () -> new DisguiseDisable(new DisguiseDisable.Builder()
                    .shiftVariationOf(YELLOW_TEMPERANCE_DISGUISE)
            ));
    /**
     * 黄色节制玩家伪装动作 / Yellow Temperance player disguise action
     */
    public static final RegistryObject<DisguisePlayer> YELLOW_TEMPERANCE_DISGUISE_PLAYER = ACTIONS.register("yellow_temperance_disguise_player",
            () -> new DisguisePlayer(new DisguisePlayer.Builder()
                    .resolveLevelToUnlock(3)
            ));
    /**
     * 黄色节制取消玩家伪装动作 / Yellow Temperance disable player disguise action
     */
    public static final RegistryObject<DisguisePlayerDisable> YELLOW_TEMPERANCE_DISGUISE_PLAYER_DISABLE = ACTIONS.register("yellow_temperance_disguise_player_disable",
            () -> new DisguisePlayerDisable(new DisguiseDisable.Builder()
                    .shiftVariationOf(YELLOW_TEMPERANCE_DISGUISE_PLAYER)
            ));
    /**
     * 黄色节制替身伪装动作 / Yellow Temperance stand disguise action
     */
    public static final RegistryObject<DisguiseStand> YELLOW_TEMPERANCE_DISGUISE_STAND = ACTIONS.register("yellow_temperance_disguise_stand",
            () -> new DisguiseStand(new DisguiseStand.Builder()
                    .resolveLevelToUnlock(3)
            ));
    /**
     * 黄色节制取消伪装动作 / Yellow Temperance disable disguise action
     */
    public static final RegistryObject<DisguiseStandDisable> YELLOW_TEMPERANCE_DISGUISE_STAND_DISABLE = ACTIONS.register("yellow_temperance_disguise_stand_disable",
            () -> new DisguiseStandDisable(new DisguiseStandDisable.Builder()
                    .shiftVariationOf(YELLOW_TEMPERANCE_DISGUISE_STAND)
            ));
    /**
     * 黄色节制纸娃娃切换动作 / Yellow Temperance doll toggle action
     */
    public static final RegistryObject<StandEntityAction> YELLOW_TEMPERANCE_TOGGLE_DOLL = ACTIONS.register("yellow_temperance_toggle_doll",
            () -> new ToggleDoll(new ToggleDoll.Builder()
                    .resolveLevelToUnlock(2)
            ));
    /**
     * 黄色节制替身类型注册 / Yellow Temperance stand type registration
     */
    public static final EntityStandRegistryObject<EntityStandType<StandStats>, StandEntityType<YellowTemperanceEntity>> STAND_YELLOW_TEMPERANCE =
            new EntityStandRegistryObject<>("yellow_temperance",
                    STANDS,
                    () -> new EntityStandType.Builder<StandStats>()
                            .color(0XC39915)
                            .storyPartName(ModStandsInit.PART_3_NAME)
                            .leftClickHotbar(
                                    YELLOW_TEMPERANCE_PUNCH.get(),
                                    YELLOW_TEMPERANCE_ABSORPTION.get()


                            )
                            .rightClickHotbar(
                                    YELLOW_TEMPERANCE_BLOCK.get(),
                                    YELLOW_TEMPERANCE_DISGUISE.get(),
                                    YELLOW_TEMPERANCE_DISGUISE_PLAYER.get(),
                                    YELLOW_TEMPERANCE_DISGUISE_STAND.get(),
                                    YELLOW_TEMPERANCE_TOGGLE_DOLL.get()
                            )
                            .defaultStats(StandStats.class, new StandStats.Builder()
                                    .tier(6)
                                    .power(6)
                                    .speed(10)
                                    .range(2.0)
                                    .durability(20)
                                    .precision(1)
                                    .randomWeight(1)
                                    .build()
                            )
                            .addSummonShout(InitSounds.YELLOW_TEMPERANCE_SUMMON_VOICELINE)
                            .addOst(InitSounds.YELLOW_TEMPERANCE_OST)
                            .disableManualControl()
                            .build(),

                    InitEntities.ENTITIES,
                    () -> new StandEntityType<YellowTemperanceEntity>(YellowTemperanceEntity::new, 0.7F, 2.1F)
                            .summonSound(InitSounds.YELLOW_TEMPERANCE_SUMMON_SOUND)
                            .unsummonSound(InitSounds.YELLOW_TEMPERANCE_UNSUMMON_SOUND))
                    .withDefaultStandAttributes();
    /**
     * 黄色节制伪装动作 / Yellow Temperance disguise action
     */
    public static final RegistryObject<Disguise> KHNUM_DISGUISE = ACTIONS.register("khnum_disguise",
            () -> new Disguise(new Disguise.Builder()
            ));
    /**
     * Khnum取消伪装动作 / Khnum disable disguise action
     */
    public static final RegistryObject<DisguiseDisable> KHNUM_DISGUISE_DISABLE = ACTIONS.register("khnum_disguise_disable",
            () -> new DisguiseDisable(new DisguiseDisable.Builder()
                    .shiftVariationOf(KHNUM_DISGUISE)
            ));
    /**
     * Khnum玩家伪装动作 / Khnum player disguise action
     */
    public static final RegistryObject<DisguisePlayer> KHNUM_DISGUISE_PLAYER = ACTIONS.register("khnum_disguise_player",
            () -> new DisguisePlayer(new DisguisePlayer.Builder()
                    .resolveLevelToUnlock(1)
            ));
    /**
     * Khnum取消玩家伪装动作 / Khnum disable player disguise action
     */
    public static final RegistryObject<DisguisePlayerDisable> KHNUM_DISGUISE_PLAYER_DISABLE = ACTIONS.register("khnum_disguise_player_disable",
            () -> new DisguisePlayerDisable(new DisguisePlayerDisable.Builder()
                    .shiftVariationOf(KHNUM_DISGUISE_PLAYER)
            ));
    /**
     * Khnum替身类型注册 / Khnum stand type registration
     */
    public static final EntityStandRegistryObject<EntityStandType<StandStats>, StandEntityType<KhnumEntity>> STAND_KHNUM =
            new EntityStandRegistryObject<>("khnum",
                    STANDS,
                    () -> new EntityStandType.Builder<StandStats>()
                            .color(0XC39915)
                            .storyPartName(ModStandsInit.PART_3_NAME)
                            .leftClickHotbar()
                            .rightClickHotbar(
                                    KHNUM_DISGUISE.get(),
                                    KHNUM_DISGUISE_PLAYER.get()
                            )
                            .defaultStats(StandStats.class, new StandStats.Builder()
                                    .tier(6)
                                    .power(2)
                                    .speed(2)
                                    .range(2.0)
                                    .durability(20)
                                    .precision(2)
                                    .randomWeight(1)
                                    .build()
                            )
                            .disableManualControl()
                            .build(),

                    InitEntities.ENTITIES,
                    () -> new StandEntityType<KhnumEntity>(KhnumEntity::new, 0.7F, 2.1F)
                            .summonSound(InitSounds.YELLOW_TEMPERANCE_SUMMON_SOUND)
                            .unsummonSound(InitSounds.YELLOW_TEMPERANCE_UNSUMMON_SOUND))
                    .withDefaultStandAttributes();


    // ======================================== 结束 ========================================


}