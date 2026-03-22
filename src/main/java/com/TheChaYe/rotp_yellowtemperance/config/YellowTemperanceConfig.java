package com.TheChaYe.rotp_yellowtemperance.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * 黄色节制配置类 / Yellow Temperance Config Class
 * 管理模组的配置选项和同步逻辑
 */
public class YellowTemperanceConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // EffectEventHandler 配置项 / EffectEventHandler config values
    /**
     * 侵蚀效果触发升级的时间阈值（ticks） / Erosion effect trigger upgrade time threshold (ticks)
     */
    public static final ForgeConfigSpec.IntValue erosionTriggerTicks;
    /**
     * 标准重置持续时间（ticks） / Standard reset duration (ticks)
     */
    public static final ForgeConfigSpec.IntValue erosionResetDuration;
    /**
     * 着火状态下的重置持续时间（ticks） / Fire state reset duration (ticks)
     */
    public static final ForgeConfigSpec.IntValue erosionFireResetDuration;
    /**
     * 侵蚀效果最大等级限制 / Maximum level limit for erosion effect
     */
    public static final ForgeConfigSpec.IntValue erosionMaxLevel;

    // 伤害和保护配置项 / Damage and protection config values
    /**
     * 侵蚀效果基础伤害 / Erosion effect base damage
     */
    public static final ForgeConfigSpec.DoubleValue erosionBaseDamage;
    /**
     * 侵蚀效果每级伤害增量 / Erosion effect damage increase per level
     */
    public static final ForgeConfigSpec.DoubleValue erosionDamagePerLevel;
    /**
     * 侵蚀效果最大伤害上限 / Erosion effect maximum damage cap
     */
    public static final ForgeConfigSpec.DoubleValue erosionMaxDamage;
    /**
     * 保护效果每级减免比例 / Protection effect damage reduction per level
     */
    public static final ForgeConfigSpec.DoubleValue protectionReductionPerLevel;
    /**
     * 保护效果最大减免比例 / Protection effect maximum reduction cap
     */
    public static final ForgeConfigSpec.DoubleValue protectionMaxReduction;

    // 客户端功能配置项 / Client feature config values
    /**
     * 是否启用饥饿值条覆盖功能 / Whether to enable food bar overlay feature
     */
    public static final ForgeConfigSpec.BooleanValue enableFoodBarOverlay;
    /**
     * 是否启用 AppleSkin 集成功能 / Whether to enable AppleSkin integration
     */
    public static final ForgeConfigSpec.BooleanValue enableAppleSkinIntegration;

    // 侵蚀效果等级配置项 / Erosion effect level config values
    /**
     * 侵蚀效果最大等级限制 / Maximum erosion effect level limit
     */
    public static final ForgeConfigSpec.IntValue maxErosionLevel;
    /**
     * 每次攻击增加的侵蚀等级 / Erosion level increase per attack
     */
    public static final ForgeConfigSpec.IntValue erosionLevelIncreasePerAttack;

    // 防御Buff配置项 / Defense Buff config values
    /**
     * 格挡时保护效果的等级 / Protection effect level when blocking
     */
    public static final ForgeConfigSpec.IntValue blockProtectionLevel;

    // 实体尺寸配置项 / Entity size config values
    /**
     * 实体最小高度 / Minimum entity height
     */
    public static final ForgeConfigSpec.DoubleValue minEntityHeight;
    /**
     * 实体最大高度 / Maximum entity height
     */
    public static final ForgeConfigSpec.DoubleValue maxEntityHeight;
    /**
     * 实体最小宽度 / Minimum entity width
     */
    public static final ForgeConfigSpec.DoubleValue minEntityWidth;
    /**
     * 实体最大宽度 / Maximum entity width
     */
    public static final ForgeConfigSpec.DoubleValue maxEntityWidth;

    // 实体伪装黑名单配置项 / Entity disguise blacklist config values
    /**
     * 实体伪装黑名单列表 / Entity disguise blacklist
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> entityDisguiseBlacklist;
    /**
     * 替身伪装黑名单列表 / Stand disguise blacklist
     */
    public static final ForgeConfigSpec.ConfigValue<List<?>> standDisguiseBlacklist;
    /**
     * 替身伪装时需要等待的 ticks 数 / Ticks to wait for stand disguise trigger
     */
    public static final ForgeConfigSpec.DoubleValue DisguiseStandNeedTicks;
    /**
     * 替身头像贴图命名空间列表 / Stand portrait texture namespaces
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> standPortraitNamespaces;

    static {
        BUILDER.push("YellowTemperance erosion effect");
        erosionTriggerTicks = BUILDER
                .comment("触发侵蚀效果升级的时间阈值（ticks），默认 10（0.5 秒） / Trigger time threshold for erosion effect upgrade (ticks), default 10 (0.5 seconds)")
                .defineInRange("erosionTriggerTicks", 10, 1, 1000);

        erosionResetDuration = BUILDER
                .comment("标准重置持续时间（ticks），默认 210（10.5 秒） / Standard reset duration (ticks), default 210 (10.5 seconds)")
                .defineInRange("erosionResetDuration", 210, 1, 10000);

        erosionFireResetDuration = BUILDER
                .comment("着火状态下的重置持续时间（ticks），默认 110（5.5 秒） / Fire state reset duration (ticks), default 110 (5.5 seconds)")
                .defineInRange("erosionFireResetDuration", 110, 1, 10000);

        erosionMaxLevel = BUILDER
                .comment("侵蚀效果最大等级限制，默认 255 / Maximum level limit for erosion effect, default 255")
                .defineInRange("erosionMaxLevel", 255, 1, 255);
        BUILDER.pop();

        BUILDER.push("YellowTemperance damage and protection");
        erosionBaseDamage = BUILDER
                .comment("侵蚀效果基础伤害，默认 0.5 / Erosion effect base damage, default 0.5")
                .defineInRange("erosionBaseDamage", 0.5, 0.1, 100.0);

        erosionDamagePerLevel = BUILDER
                .comment("侵蚀效果每级伤害增量，默认 0.5 / Erosion effect damage increase per level, default 0.5")
                .defineInRange("erosionDamagePerLevel", 0.5, 0.1, 50.0);

        erosionMaxDamage = BUILDER
                .comment("侵蚀效果最大伤害上限，默认 20.0 / Erosion effect maximum damage cap, default 20.0")
                .defineInRange("erosionMaxDamage", 20.0, 1.0, 1000.0);

        protectionReductionPerLevel = BUILDER
                .comment("保护效果每级减免比例，默认 0.3 (30%) / Protection effect damage reduction per level, default 0.3 (30%)")
                .defineInRange("protectionReductionPerLevel", 0.3, 0.01, 1);

        protectionMaxReduction = BUILDER
                .comment("保护效果最大减免比例，默认 0.98 (98%) / Protection effect maximum reduction cap, default 0.98 (98%)")
                .defineInRange("protectionMaxReduction", 0.98, 0.1, 1);
        BUILDER.pop();

        BUILDER.push("YellowTemperance client features");
        enableFoodBarOverlay = BUILDER
                .comment("是否启用饥饿值条覆盖功能，默认开启 / Whether to enable food bar overlay feature, enabled by default")
                .define("enableFoodBarOverlay", true);

        enableAppleSkinIntegration = BUILDER
                .comment("是否启用 AppleSkin 集成功能，默认开启 / Whether to enable AppleSkin integration, enabled by default")
                .define("enableAppleSkinIntegration", true);
        BUILDER.pop();

        BUILDER.push("YellowTemperance erosion levels");
        maxErosionLevel = BUILDER
                .comment("侵蚀效果最大等级限制，默认 5 / Maximum erosion effect level limit, default 5")
                .defineInRange("maxErosionLevel", 5, 1, 255);

        erosionLevelIncreasePerAttack = BUILDER
                .comment("每次攻击增加的侵蚀等级，默认 1 / Erosion level increase per attack, default 1")
                .defineInRange("erosionLevelIncreasePerAttack", 1, 1, 10);
        BUILDER.pop();

        BUILDER.push("YellowTemperance defense buffs");
        blockProtectionLevel = BUILDER
                .comment("格挡时保护效果的等级，默认 4 / Protection effect level when blocking, default 4")
                .defineInRange("blockProtectionLevel", 4, 0, 255);
        BUILDER.pop();

        BUILDER.push("YellowTemperance entity sizes");
        minEntityHeight = BUILDER
                .comment("可伪装实体最小高度，默认 1.0 / Minimum disguise entity height, default 1.0")
                .defineInRange("minEntityHeight", 1.0, 0.1, 10.0);

        maxEntityHeight = BUILDER
                .comment("可伪装实体最大高度，默认 3.0 / Maximum disguise entity height, default 3.0")
                .defineInRange("maxEntityHeight", 3.0, 0.5, 20.0);

        minEntityWidth = BUILDER
                .comment("可伪装实体最小宽度，默认 0.5 / Minimum disguise entity width, default 0.5")
                .defineInRange("minEntityWidth", 0.5, 0.1, 5.0);

        maxEntityWidth = BUILDER
                .comment("可伪装实体最大宽度，默认 2.0 / Maximum disguise entity width, default 2.0")
                .defineInRange("maxEntityWidth", 2.0, 0.3, 10.0);

        entityDisguiseBlacklist = BUILDER
                .comment("实体伪装黑名单列表（资源定位符格式）/ Entity disguise blacklist (resource location format)",
                        "例如：minecraft:fireball, minecraft:arrow, minecraft:ender_dragon 等 / Example: minecraft:fireball, minecraft:arrow, minecraft:ender_dragon, etc.")
                .translation("rotp_yellowtemperance.entity_disguise_blacklist")
                .defineListAllowEmpty(
                        Arrays.asList("entityDisguiseBlacklist"),
                        () -> Arrays.asList(
                                "minecraft:fireball",
                                "minecraft:small_fireball",
                                "minecraft:dragon_fireball",
                                "minecraft:wither_skull",
                                "minecraft:shulker_bullet",
                                "minecraft:arrow",
                                "minecraft:spectral_arrow",
                                "minecraft:trident",
                                "minecraft:snowball",
                                "minecraft:egg",
                                "minecraft:ender_pearl",
                                "minecraft:experience_bottle",
                                "minecraft:potion",
                                "minecraft:llama_spit",
                                "minecraft:end_crystal",
                                "minecraft:boat",
                                "minecraft:minecart"
                        ),
                        s -> s instanceof String);
        standDisguiseBlacklist = BUILDER
                .comment("替身伪装黑名单列表/ Stand disguise blacklist",
                        "例如：minecraft:fireball, minecraft:arrow, minecraft:ender_dragon 等 / Example: minecraft:fireball, minecraft:arrow, minecraft:ender_dragon, etc.")
                .translation("rotp_yellowtemperance.stand_disguise_blacklist")
                .defineListAllowEmpty(
                        Arrays.asList("standDisguiseBlacklist"),
                        () -> Arrays.asList(),
                        s -> s instanceof String);
        DisguiseStandNeedTicks = BUILDER
                .comment("替身伪装时需要等待的 ticks 数，默认 20 / Ticks to wait for stand disguise trigger, default 20")
                .defineInRange("disguiseStandNeedTicks", 10.0, 1, 200);

        standPortraitNamespaces = BUILDER
                .comment("替身头像贴图命名空间列表（按优先级排序）/ Stand portrait texture namespaces (sorted by priority)",
                        "用于查找替身头像贴图的额外命名空间 / Additional namespaces for searching stand portrait textures",
                        "例如：rotp_extra_dg, jojo, rotp_yellowtemperance 等 / Example: rotp_extra_dg, jojo, rotp_yellowtemperance, etc.")
                .translation("rotp_yellowtemperance.stand_portrait_namespaces")
                .defineListAllowEmpty(
                        Arrays.asList("standPortraitNamespaces"),
                        () -> Arrays.asList("rotp_extra_dg"),
                        s -> s instanceof String);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
