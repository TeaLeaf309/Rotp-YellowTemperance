package com.TheChaYe.rotp_yellowtemperance.config;

import com.TheChaYe.rotp_yellowtemperance.network.packets.client.CommonConfigPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.ResetSyncedCommonConfigPacket;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.network.PacketManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

/**
 * 黄色节制配置类 / Yellow Temperance Config Class
 * 管理模组的配置选项和同步逻辑
 */
@Mod.EventBusSubscriber(modid = "rotp_yellowtemperance", bus = Mod.EventBusSubscriber.Bus.MOD)
public class YellowTemperanceConfig {
    //constants / 常量
    static final ForgeConfigSpec commonSpec;
    private static final Common COMMON_FROM_FILE;
    private static final Common COMMON_SYNCED_TO_CLIENT;

    /**
     * 静态初始化块 / Static initialization block
     * 初始化配置规范和实例
     */
    static {
        Pair<Common, ForgeConfigSpec> specPair = (new ForgeConfigSpec.Builder()).configure(Common::new);
        commonSpec = specPair.getRight();
        COMMON_FROM_FILE = specPair.getLeft();
        Pair<Common, ForgeConfigSpec> syncedSpecPair = (new ForgeConfigSpec.Builder()).configure((builder) -> new Common(builder, "synced"));
        CommentedConfig config = CommentedConfig.of(InMemoryCommentedFormat.defaultInstance());
        ForgeConfigSpec syncedSpec = syncedSpecPair.getRight();
        syncedSpec.correct(config);
        syncedSpec.setConfig(config);
        COMMON_SYNCED_TO_CLIENT = syncedSpecPair.getLeft();
    }

    //builder / 构造器
    /**
     * 构造函数 / Constructor
     */
    public YellowTemperanceConfig() {
    }

    //methods / 方法
    /**
     * 检查元素是否为非负浮点数 / Check if element is non-negative float
     * @param num 数字对象 / Number object
     * @param moreThanZero 是否必须大于零 / Whether must be greater than zero
     * @return 是否为有效的非负浮点数 / Whether is valid non-negative float
     */
    private static boolean isElementNonNegativeFloat(Object num, boolean moreThanZero) {
        if (!(num instanceof Double)) {
            return false;
        } else {
            Double numDouble = (Double) num;
            return (numDouble > (double) 0.0F || !moreThanZero && numDouble == (double) 0.0F) && Float.isFinite(numDouble.floatValue());
        }
    }

    /**
     * 获取通用配置实例 / Get common config instance
     * @param isClientSide 是否为客户端 / Whether is client side
     * @return 配置实例 / Config instance
     */
    public static Common getCommonConfigInstance(boolean isClientSide) {
        return isClientSide && !ClientUtil.isLocalServer() ? COMMON_SYNCED_TO_CLIENT : COMMON_FROM_FILE;
    }

    //events / 事件
    /**
     * 配置加载事件 / Config load event
     * 处理配置文件加载完成后的逻辑
     * @param event 模组配置事件 / Mod config event
     */
    @SubscribeEvent
    public static void onConfigLoad(ModConfig.ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if ("rotp_yellowtemperance".equals(config.getModId()) && config.getType() == ModConfig.Type.COMMON) {
            COMMON_FROM_FILE.onLoadOrReload();
        }
    }

    /**
     * 配置重载事件 / Config reload event
     * 处理配置文件重新加载时的同步逻辑
     * @param event 配置重载事件 / Config reloading event
     */
    @SubscribeEvent
    public static void onConfigReload(ModConfig.Reloading event) {
        ModConfig config = event.getConfig();
        if ("rotp_yellowtemperance".equals(config.getModId()) && config.getType() == ModConfig.Type.COMMON) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.getPlayerList().getPlayers().forEach((player) -> Common.SyncedValues.syncWithClient(player));
            }
        }
    }

    //the whole config / 整个配置
    /**
     * 通用配置类 / Common config class
     * 管理模组的通用配置选项
     */
    public static class Common {
        // EffectEventHandler 配置项 / EffectEventHandler config values
        /** 侵蚀效果触发升级的时间阈值（ticks） / Erosion effect trigger upgrade time threshold (ticks) */
        public final ForgeConfigSpec.IntValue erosionTriggerTicks;
        /** 标准重置持续时间（ticks） / Standard reset duration (ticks) */
        public final ForgeConfigSpec.IntValue erosionResetDuration;
        /** 着火状态下的重置持续时间（ticks） / Fire state reset duration (ticks) */
        public final ForgeConfigSpec.IntValue erosionFireResetDuration;
        /** 最大等级限制 / Maximum level limit */
        public final ForgeConfigSpec.IntValue erosionMaxLevel;
        
        // 伤害和保护配置项 / Damage and protection config values
        /** 侵蚀效果基础伤害 / Erosion effect base damage */
        public final ForgeConfigSpec.DoubleValue erosionBaseDamage;
        /** 侵蚀效果每级伤害增量 / Erosion effect damage increase per level */
        public final ForgeConfigSpec.DoubleValue erosionDamagePerLevel;
        /** 侵蚀效果最大伤害上限 / Erosion effect maximum damage cap */
        public final ForgeConfigSpec.DoubleValue erosionMaxDamage;
        /** 保护效果每级减免比例 / Protection effect damage reduction per level */
        public final ForgeConfigSpec.DoubleValue protectionReductionPerLevel;
        /** 保护效果最大减免比例 / Protection effect maximum reduction cap */
        public final ForgeConfigSpec.DoubleValue protectionMaxReduction;
        
        // 客户端功能配置项 / Client feature config values
        /** 是否启用饥饿值条覆盖功能 / Whether to enable food bar overlay feature */
        public final ForgeConfigSpec.BooleanValue enableFoodBarOverlay;
        /** 是否启用AppleSkin集成功能 / Whether to enable AppleSkin integration */
        public final ForgeConfigSpec.BooleanValue enableAppleSkinIntegration;
        
        // 侵蚀效果等级配置项 / Erosion effect level config values
        /** 侵蚀效果最大等级限制 / Maximum erosion effect level limit */
        public final ForgeConfigSpec.IntValue maxErosionLevel;
        /** 每次攻击增加的侵蚀等级 / Erosion level increase per attack */
        public final ForgeConfigSpec.IntValue erosionLevelIncreasePerAttack;
        
        // 防御Buff配置项 / Defense Buff config values
        /** 格挡时保护效果的等级 / Protection effect level when blocking */
        public final ForgeConfigSpec.IntValue blockProtectionLevel;
        
        // 实体尺寸配置项 / Entity size config values
        /** 实体最小高度 / Minimum entity height */
        public final ForgeConfigSpec.DoubleValue minEntityHeight;
        /** 实体最大高度 / Maximum entity height */
        public final ForgeConfigSpec.DoubleValue maxEntityHeight;
        /** 实体最小宽度 / Minimum entity width */
        public final ForgeConfigSpec.DoubleValue minEntityWidth;
        /** 实体最大宽度 / Maximum entity width */
        public final ForgeConfigSpec.DoubleValue maxEntityWidth;
        
        //constants / 常量
        private boolean loaded;

        //methods / 方法
        /**
         * 构造函数 / Constructor
         * @param builder 配置构建器 / Config builder
         */
        private Common(ForgeConfigSpec.Builder builder) {
            this(builder, null);
        }

        /**
         * 构造函数重载 / Constructor overload
         * @param builder 配置构建器 / Config builder
         * @param mainPath 主路径 / Main path
         */
        private Common(ForgeConfigSpec.Builder builder, @Nullable String mainPath) {
            this.loaded = false;
            if (mainPath != null) builder.push(mainPath);

            builder.push("YellowTemperance erosion effect");
            this.erosionTriggerTicks = builder
                    .comment("触发侵蚀效果升级的时间阈值（ticks），默认10（0.5秒） / Trigger time threshold for erosion effect upgrade (ticks), default 10 (0.5 seconds)")
                    .translation("rotp_yellowtemperance.erosion_trigger_ticks")
                    .defineInRange("erosionTriggerTicks", 10, 1, 1000);
            
            this.erosionResetDuration = builder
                    .comment("标准重置持续时间（ticks），默认210（10.5秒） / Standard reset duration (ticks), default 210 (10.5 seconds)")
                    .translation("rotp_yellowtemperance.erosion_reset_duration")
                    .defineInRange("erosionResetDuration", 210, 1, 10000);
            
            this.erosionFireResetDuration = builder
                    .comment("着火状态下的重置持续时间（ticks），默认110（5.5秒） / Fire state reset duration (ticks), default 110 (5.5 seconds)")
                    .translation("rotp_yellowtemperance.erosion_fire_reset_duration")
                    .defineInRange("erosionFireResetDuration", 110, 1, 10000);
            
            this.erosionMaxLevel = builder
                    .comment("侵蚀效果最大等级限制，默认255 / Maximum level limit for erosion effect, default 255")
                    .translation("rotp_yellowtemperance.erosion_max_level")
                    .defineInRange("erosionMaxLevel", 255, 1, 255);
            builder.pop();
            
            builder.push("YellowTemperance damage and protection");
            this.erosionBaseDamage = builder
                    .comment("侵蚀效果基础伤害，默认0.5 / Erosion effect base damage, default 0.5")
                    .translation("rotp_yellowtemperance.erosion_base_damage")
                    .defineInRange("erosionBaseDamage", 0.5, 0.1, 100.0);
            
            this.erosionDamagePerLevel = builder
                    .comment("侵蚀效果每级伤害增量，默认0.5 / Erosion effect damage increase per level, default 0.5")
                    .translation("rotp_yellowtemperance.erosion_damage_per_level")
                    .defineInRange("erosionDamagePerLevel", 0.5, 0.1, 50.0);
            
            this.erosionMaxDamage = builder
                    .comment("侵蚀效果最大伤害上限，默认20.0 / Erosion effect maximum damage cap, default 20.0")
                    .translation("rotp_yellowtemperance.erosion_max_damage")
                    .defineInRange("erosionMaxDamage", 20.0, 1.0, 1000.0);
            
            this.protectionReductionPerLevel = builder
                    .comment("保护效果每级减免比例，默认0.3 (30%) / Protection effect damage reduction per level, default 0.3 (30%)")
                    .translation("rotp_yellowtemperance.protection_reduction_per_level")
                    .defineInRange("protectionReductionPerLevel", 0.3, 0.01, 1);
            
            this.protectionMaxReduction = builder
                    .comment("保护效果最大减免比例，默认0.98 (98%) / Protection effect maximum reduction cap, default 0.98 (98%)")
                    .translation("rotp_yellowtemperance.protection_max_reduction")
                    .defineInRange("protectionMaxReduction", 0.98, 0.1, 1);
            builder.pop();
            
            builder.push("YellowTemperance client features");
            this.enableFoodBarOverlay = builder
                    .comment("是否启用饥饿值条覆盖功能，默认开启 / Whether to enable food bar overlay feature, enabled by default")
                    .translation("rotp_yellowtemperance.enable_food_bar_overlay")
                    .define("enableFoodBarOverlay", true);
            
            this.enableAppleSkinIntegration = builder
                    .comment("是否启用AppleSkin集成功能，默认开启 / Whether to enable AppleSkin integration, enabled by default")
                    .translation("rotp_yellowtemperance.enable_appleskin_integration")
                    .define("enableAppleSkinIntegration", true);
            builder.pop();
            
            builder.push("YellowTemperance erosion levels");
            this.maxErosionLevel = builder
                    .comment("侵蚀效果最大等级限制，默认5 / Maximum erosion effect level limit, default 5")
                    .translation("rotp_yellowtemperance.max_erosion_level")
                    .defineInRange("maxErosionLevel", 5, 1, 255);
            
            this.erosionLevelIncreasePerAttack = builder
                    .comment("每次攻击增加的侵蚀等级，默认1 / Erosion level increase per attack, default 1")
                    .translation("rotp_yellowtemperance.erosion_level_increase_per_attack")
                    .defineInRange("erosionLevelIncreasePerAttack", 1, 1, 10);
            builder.pop();
            
            builder.push("YellowTemperance defense buffs");
            this.blockProtectionLevel = builder
                    .comment("格挡时保护效果的等级，默认4 / Protection effect level when blocking, default 4")
                    .translation("rotp_yellowtemperance.block_protection_level")
                    .defineInRange("blockProtectionLevel", 4, 0, 255);
            builder.pop();

            builder.push("YellowTemperance entity sizes");
            this.minEntityHeight = builder
                    .comment("可伪装实体最小高度，默认1.0 / Minimum disguise entity height, default 1.0")
                    .translation("rotp_yellowtemperance.min_entity_height")
                    .defineInRange("minEntityHeight", 1.0, 0.1, 10.0);

            this.maxEntityHeight = builder
                    .comment("可伪装实体最大高度，默认3.0 / Maximum disguise entity height, default 3.0")
                    .translation("rotp_yellowtemperance.max_entity_height")
                    .defineInRange("maxEntityHeight", 3.0, 0.5, 20.0);

            this.minEntityWidth = builder
                    .comment("可伪装实体最小宽度，默认0.5 / Minimum disguise entity width, default 0.5")
                    .translation("rotp_yellowtemperance.min_entity_width")
                    .defineInRange("minEntityWidth", 0.5, 0.1, 5.0);

            this.maxEntityWidth = builder
                    .comment("可伪装实体最大宽度，默认2.0 / Maximum disguise entity width, default 2.0")
                    .translation("rotp_yellowtemperance.max_entity_width")
                    .defineInRange("maxEntityWidth", 2.0, 0.3, 10.0);
            builder.pop();

            if (mainPath != null) builder.pop();
        }

        /**
         * 检查配置是否已加载 / Check if config is loaded
         * @return 是否已加载 / Whether loaded
         */
        public boolean isConfigLoaded() {
            return this.loaded;
        }

        /**
         * 配置加载或重载时调用 / Called when config loads or reloads
         */
        private void onLoadOrReload() {
            this.loaded = true;
        }

        /**
         * 同步值类 / Synced values class
         * 处理配置值在客户端和服务端之间的同步
         */
        public static class SyncedValues {
            // EffectEventHandler 同步值 / EffectEventHandler synced values
            private final int erosionTriggerTicks;
            private final int erosionResetDuration;
            private final int erosionFireResetDuration;
            private final int erosionMaxLevel;
            
            // 伤害和保护同步值 / Damage and protection synced values
            private final double erosionBaseDamage;
            private final double erosionDamagePerLevel;
            private final double erosionMaxDamage;
            private final double protectionReductionPerLevel;
            private final double protectionMaxReduction;
            
            // 客户端功能同步值 / Client feature synced values
            private final boolean enableFoodBarOverlay;
            private final boolean enableAppleSkinIntegration;
            
            // 侵蚀效果等级同步值 / Erosion effect level synced values
            private final int maxErosionLevel;
            private final int erosionLevelIncreasePerAttack;
            
            // 防御Buff同步值 / Defense Buff synced values
            private final int blockProtectionLevel;
            
            // 实体尺寸同步值 / Entity size synced values
            private final double minEntityHeight;
            private final double maxEntityHeight;
            private final double minEntityWidth;
            private final double maxEntityWidth;

            /**
             * 从数据包缓冲区构造 / Construct from packet buffer
             * @param buf 数据包缓冲区 / Packet buffer
             */
            public SyncedValues(PacketBuffer buf) {
                this.erosionTriggerTicks = buf.readInt();
                this.erosionResetDuration = buf.readInt();
                this.erosionFireResetDuration = buf.readInt();
                this.erosionMaxLevel = buf.readInt();
                this.erosionBaseDamage = buf.readDouble();
                this.erosionDamagePerLevel = buf.readDouble();
                this.erosionMaxDamage = buf.readDouble();
                this.protectionReductionPerLevel = buf.readDouble();
                this.protectionMaxReduction = buf.readDouble();
                this.enableFoodBarOverlay = buf.readBoolean();
                this.enableAppleSkinIntegration = buf.readBoolean();
                this.maxErosionLevel = buf.readInt();
                this.erosionLevelIncreasePerAttack = buf.readInt();
                this.blockProtectionLevel = buf.readInt();
                this.minEntityHeight = buf.readDouble();
                this.maxEntityHeight = buf.readDouble();
                this.minEntityWidth = buf.readDouble();
                this.maxEntityWidth = buf.readDouble();
            }

            /**
             * 从配置构造 / Construct from config
             * @param config 通用配置 / Common config
             */
            private SyncedValues(Common config) {
                this.erosionTriggerTicks = config.erosionTriggerTicks.get();
                this.erosionResetDuration = config.erosionResetDuration.get();
                this.erosionFireResetDuration = config.erosionFireResetDuration.get();
                this.erosionMaxLevel = config.erosionMaxLevel.get();
                this.erosionBaseDamage = config.erosionBaseDamage.get();
                this.erosionDamagePerLevel = config.erosionDamagePerLevel.get();
                this.erosionMaxDamage = config.erosionMaxDamage.get();
                this.protectionReductionPerLevel = config.protectionReductionPerLevel.get();
                this.protectionMaxReduction = config.protectionMaxReduction.get();
                this.enableFoodBarOverlay = config.enableFoodBarOverlay.get();
                this.enableAppleSkinIntegration = config.enableAppleSkinIntegration.get();
                this.maxErosionLevel = config.maxErosionLevel.get();
                this.erosionLevelIncreasePerAttack = config.erosionLevelIncreasePerAttack.get();
                this.blockProtectionLevel = config.blockProtectionLevel.get();
                this.minEntityHeight = config.minEntityHeight.get();
                this.maxEntityHeight = config.maxEntityHeight.get();
                this.minEntityWidth = config.minEntityWidth.get();
                this.maxEntityWidth = config.maxEntityWidth.get();
            }

            /**
             * 重置配置 / Reset config
             */
            public static void resetConfig() {
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionTriggerTicks.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionResetDuration.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionFireResetDuration.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionMaxLevel.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionBaseDamage.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionDamagePerLevel.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionMaxDamage.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.protectionReductionPerLevel.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.protectionMaxReduction.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.enableFoodBarOverlay.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.enableAppleSkinIntegration.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.maxErosionLevel.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionLevelIncreasePerAttack.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.blockProtectionLevel.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.minEntityHeight.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.maxEntityHeight.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.minEntityWidth.clearCache();
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.maxEntityWidth.clearCache();
            }

            /**
             * 与客户端同步 / Sync with client
             * @param player 目标玩家 / Target player
             */
            public static void syncWithClient(ServerPlayerEntity player) {
                PacketManager.sendToClient(new CommonConfigPacket(new SyncedValues(YellowTemperanceConfig.COMMON_FROM_FILE)), player);
            }

            /**
             * 玩家登出时调用 / Called when player logs out
             * @param player 登出的玩家 / Logging out player
             */
            public static void onPlayerLogout(ServerPlayerEntity player) {
                PacketManager.sendToClient(new ResetSyncedCommonConfigPacket(), player);
            }

            /**
             * 写入缓冲区 / Write to buffer
             * @param buf 目标缓冲区 / Target buffer
             */
            public void writeToBuf(PacketBuffer buf) {
                buf.writeInt(this.erosionTriggerTicks);
                buf.writeInt(this.erosionResetDuration);
                buf.writeInt(this.erosionFireResetDuration);
                buf.writeInt(this.erosionMaxLevel);
                buf.writeDouble(this.erosionBaseDamage);
                buf.writeDouble(this.erosionDamagePerLevel);
                buf.writeDouble(this.erosionMaxDamage);
                buf.writeDouble(this.protectionReductionPerLevel);
                buf.writeDouble(this.protectionMaxReduction);
                buf.writeBoolean(this.enableFoodBarOverlay);
                buf.writeBoolean(this.enableAppleSkinIntegration);
                buf.writeInt(this.maxErosionLevel);
                buf.writeInt(this.erosionLevelIncreasePerAttack);
                buf.writeInt(this.blockProtectionLevel);
                buf.writeDouble(this.minEntityHeight);
                buf.writeDouble(this.maxEntityHeight);
                buf.writeDouble(this.minEntityWidth);
                buf.writeDouble(this.maxEntityWidth);
            }

            /**
             * 更改配置值 / Change config values
             */
            public void changeConfigValues() {
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionTriggerTicks.set(this.erosionTriggerTicks);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionResetDuration.set(this.erosionResetDuration);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionFireResetDuration.set(this.erosionFireResetDuration);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionMaxLevel.set(this.erosionMaxLevel);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionBaseDamage.set(this.erosionBaseDamage);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionDamagePerLevel.set(this.erosionDamagePerLevel);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionMaxDamage.set(this.erosionMaxDamage);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.protectionReductionPerLevel.set(this.protectionReductionPerLevel);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.protectionMaxReduction.set(this.protectionMaxReduction);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.enableFoodBarOverlay.set(this.enableFoodBarOverlay);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.enableAppleSkinIntegration.set(this.enableAppleSkinIntegration);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.maxErosionLevel.set(this.maxErosionLevel);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.erosionLevelIncreasePerAttack.set(this.erosionLevelIncreasePerAttack);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.blockProtectionLevel.set(this.blockProtectionLevel);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.minEntityHeight.set(this.minEntityHeight);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.maxEntityHeight.set(this.maxEntityHeight);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.minEntityWidth.set(this.minEntityWidth);
                YellowTemperanceConfig.COMMON_SYNCED_TO_CLIENT.maxEntityWidth.set(this.maxEntityWidth);
            }
        }
    }
}