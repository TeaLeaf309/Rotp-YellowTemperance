package com.TheChaYe.rotp_yellowtemperance;

import com.TheChaYe.rotp_yellowtemperance.capability.entity.CapabilityHandler;
import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.events.DamageHandler;
import com.TheChaYe.rotp_yellowtemperance.events.PlayerLoginEventHandler;
import com.TheChaYe.rotp_yellowtemperance.init.*;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 黄色节制模组主类 / Yellow Temperance Addon Main Class
 * 模组的入口点和核心初始化逻辑
 */
@Mod(RotPYellowTemperanceAddon.MOD_ID)
public class RotPYellowTemperanceAddon {
    /**
     * 模组ID / Mod ID
     */
    public static final String MOD_ID = "rotp_yellowtemperance";
    /**
     * 模组日志记录器 / Mod logger
     */
    public static final Logger LOGGER = LogManager.getLogger();
    /**
     * 可用于伪装的实体列表（线程安全） / Entities available for disguise (thread-safe)
     */
    private static final CopyOnWriteArrayList<EntityType<?>> ENTITIES_FOR_DISGUISE = new CopyOnWriteArrayList<>();
    /**
     * 可用于替身伪装的实体列表（只包含替身实体） / Entities available for stand disguise (stand entities only)
     */
    private static final CopyOnWriteArrayList<EntityType<?>> ENTITIES_FOR_STAND_DISGUISE = new CopyOnWriteArrayList<>();
    /**
     * 实体尺寸缓存 / Entity size cache
     */
    private static final Map<EntityType<?>, EntitySize> ENTITY_SIZE_CACHE = new HashMap<>();

    // 修复：确保网络包只注册一次 / Fix: Ensure packets are registered only once
    private static boolean packetsRegistered = false;

    /**
     * 模组构造函数 / Mod constructor
     * 初始化模组并注册各种组件
     */
    public RotPYellowTemperanceAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置文件 / Register config file
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, YellowTemperanceConfig.SPEC);

        // 注册DeferredRegister对象 / Register DeferredRegister objects
        InitEntities.ENTITIES.register(modEventBus);
        InitSounds.SOUNDS.register(modEventBus);
        InitStands.ACTIONS.register(modEventBus);
        InitStands.STANDS.register(modEventBus);
        InitEffects.EFFECTS.register(modEventBus);

        modEventBus.addListener(this::preInit);
        modEventBus.addListener(this::setup);

        // 注册Forge事件总线监听器 / Register Forge event bus listeners
        MinecraftForge.EVENT_BUS.register(new DamageHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerLoginEventHandler());
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 获取可用于伪装的实体列表 / Get entities for disguise
     *
     * @return 实体类型列表 / List of entity types
     */
    public static CopyOnWriteArrayList<EntityType<?>> getEntitiesForDisguise() {
        return ENTITIES_FOR_DISGUISE;
    }

    /**
     * 获取可用于替身伪装的实体列表 / Get entities for stand disguise
     *
     * @return 替身实体类型列表 / List of stand entity types
     */
    public static CopyOnWriteArrayList<EntityType<?>> getEntitiesForStandDisguise() {
        return ENTITIES_FOR_STAND_DISGUISE;
    }

    /**
     * 检查实体尺寸是否有效 / Check if entity size is valid
     * 验证实体是否适合用于伪装
     *
     * @param entityType 实体类型 / Entity type
     * @return 尺寸是否有效 / Whether size is valid
     */
    public static boolean isEntitySizeValid(EntityType<?> entityType) {
        // 从缓存获取尺寸 / Get size from cache
        EntitySize size = ENTITY_SIZE_CACHE.computeIfAbsent(entityType, type -> {
            try {
                EntitySize dimensions = type.getDimensions();
                if (dimensions == null) {
                    getLogger().warn("Entity {} returned null dimensions, using default size", type.getRegistryName());
                    return EntitySize.scalable(0.6f, 1.8f); // 默认玩家尺寸 / Default player size
                }
                return dimensions;
            } catch (NullPointerException e) {
                getLogger().error("Entity {} threw NPE when getting dimensions, skipping", type.getRegistryName(), e);
                throw new RuntimeException("Failed to get dimensions for entity: " + type.getRegistryName(), e);
            } catch (Exception e) {
                getLogger().error("Entity {} threw exception when getting dimensions, skipping", type.getRegistryName(), e);
                throw new RuntimeException("Failed to get dimensions for entity: " + type.getRegistryName(), e);
            }
        });

        // 如果获取尺寸失败，返回 false / If size retrieval failed, return false
        if (size == null) {
            return false;
        }

        // 获取配置的尺寸要求 / Get configured size requirements
        final double MIN_HEIGHT = YellowTemperanceConfig.minEntityHeight.get();
        final double MAX_HEIGHT = YellowTemperanceConfig.maxEntityHeight.get();
        final double MIN_WIDTH = YellowTemperanceConfig.minEntityWidth.get();
        final double MAX_WIDTH = YellowTemperanceConfig.maxEntityWidth.get();

        // 检查尺寸是否符合要求 / Check if size meets requirements
        boolean heightValid = size.height >= MIN_HEIGHT && size.height <= MAX_HEIGHT;
        boolean widthValid = size.width >= MIN_WIDTH && size.width <= MAX_WIDTH;

        return heightValid && widthValid;
    }

    /**
     * 获取日志记录器 / Get logger
     *
     * @return 模组日志记录器 / Mod logger
     */
    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * 预初始化阶段 / Pre-initialization phase
     * 在模组加载早期阶段执行初始化任务
     *
     * @param event FML 通用设置事件 / FML common setup event
     */
    private void preInit(FMLCommonSetupEvent event) {
        InitStatusEffect.afterEffectsRegister();
        event.enqueueWork(() -> {
            // 防止重复填充实体列表 / Prevent duplicate entity list population
            if (ENTITIES_FOR_DISGUISE.isEmpty()) {
                java.util.concurrent.atomic.AtomicInteger addedCount = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.concurrent.atomic.AtomicInteger skippedCount = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.concurrent.atomic.AtomicInteger standCount = new java.util.concurrent.atomic.AtomicInteger(0);

                ForgeRegistries.ENTITIES.getEntries().forEach(entry -> {
                    EntityType<?> entityType = entry.getValue();
                    ResourceLocation registryName = entityType.getRegistryName();

                    // 排除末影龙 / Exclude ender dragon
                    if (entityType.equals(EntityType.ENDER_DRAGON)) {
                        return;
                    }

                    // 关键修改：添加自定义黑名单 / Key modification: add custom blacklist
                    if (isBlacklisted(entityType, registryName)) {
                        return;
                    }
                    // 检查是否为替身实体 / Check if stand entity
                    boolean isStand = isStandEntity(entityType);
                    // 允许生物、怪物和部分 MISC 实体（但不包括替身实体） / Allow creatures, monsters and some MISC entities (but not stand entities)
                    EntityClassification classification = entityType.getCategory();
                    boolean isValidCategory = classification == EntityClassification.CREATURE ||
                            classification == EntityClassification.MONSTER ||
                            (classification == EntityClassification.MISC && isLivingEntityLike(entityType));

                    // 检查实体类别和尺寸 / Check entity category and size
                    if (isValidCategory) {
                        try {
                            if (isEntitySizeValid(entityType)) {
                                EntitySize size = ENTITY_SIZE_CACHE.get(entityType);
                                getLogger().debug("Added valid entity: {} (H: {:.2f}, W: {:.2f})",
                                        entityType.toString(),
                                        size.height,
                                        size.width);

                                ENTITIES_FOR_DISGUISE.add(entityType);
                                addedCount.incrementAndGet();
                            } else {
                                skippedCount.incrementAndGet();
                            }
                        } catch (RuntimeException e) {
                            skippedCount.incrementAndGet();
                        }
                    } else if (isStand && !isStandBlacklisted(entityType, registryName)) {
                        // 替身实体只添加到替身伪装列表 / Stand entities only added to stand disguise list
                        // 替身实体不检查尺寸限制，所有替身都可以加入伪装列表
                        try {
                            ENTITIES_FOR_STAND_DISGUISE.add(entityType);
                        } catch (Exception ignored) {
                            getLogger().error("Error adding stand entity: {}", registryName, ignored);
                        }
                    }
                });
            }
        });
    }

    /**
     * 检查实体是否类似生物实体 / Check if entity is living entity-like
     * 判断MISC分类的实体是否具有生物特征
     *
     * @param entityType 实体类型 / Entity type
     * @return 是否类似生物实体 / Whether entity is living entity-like
     */
    private boolean isLivingEntityLike(EntityType<?> entityType) {
        // 检查实体是否类似于生物实体（如铁傀儡） / Check if entity is similar to living entities (like iron golem)
        // 创建一个实体实例来检查它是否是LivingEntity的子类 / Create entity instance to check if it's subclass of LivingEntity
        try {
            Entity entity = entityType.create(null); // 传入null世界，因为我们只关心类型 / Pass null world since we only care about type
            if (entity instanceof LivingEntity) {
                // 对于LivingEntity实例，进一步检查是否具有生物特征 / For LivingEntity instances, further check if they have living characteristics
                // 检查是否是特定的可接受MISC类型实体（如铁傀儡） / Check if it's specific acceptable MISC type entities (like iron golem)
                return entityType == EntityType.IRON_GOLEM ||
                        entityType == EntityType.SNOW_GOLEM ||
                        entityType == EntityType.VILLAGER ||
                        entityType == EntityType.WANDERING_TRADER;
            }
        } catch (Exception e) {
            // 如果创建实体时出错，回退到基于ID的检查 / If entity creation fails, fall back to ID-based check
            ResourceLocation registryName = entityType.getRegistryName();
            if (registryName != null) {
                String id = registryName.toString();
                // 检查是否包含典型的生物实体关键词 / Check if contains typical living entity keywords
                return id.contains("golem") ||
                        id.contains("villager") ||
                        id.contains("zombie") ||
                        id.contains("skeleton");
            }
        }
        return false;
    }

    /**
     * 检查是否为替身实体 / Check if entity is stand entity
     * 判断 MISC 分类的实体是否为 JoJo 替身实体
     *
     * @param entityType 实体类型 / Entity type
     * @return 是否为替身实体 / Whether entity is stand entity
     */
    private boolean isStandEntity(EntityType<?> entityType) {
        // 检查实体是否是 StandEntityType 的实例
        return entityType instanceof StandEntityType<?>;
    }

    /**
     * 检查实体是否在黑名单中 / Check if entity is blacklisted
     * 判断指定实体是否被禁止用于伪装
     *
     * @param entityType   实体类型 / Entity type
     * @param registryName 注册名 / Registry name
     * @return 是否在黑名单中 / Whether in blacklist
     */
    private boolean isBlacklisted(EntityType<?> entityType, ResourceLocation registryName) {
        if (registryName == null) {
            return false;
        }
        String id = registryName.toString();

        // 从配置文件中获取黑名单列表 / Get blacklist from config file
        java.util.List<String> configBlacklist = (java.util.List<String>) YellowTemperanceConfig.entityDisguiseBlacklist.get();

        // 检查是否在配置黑名单中 / Check if in config blacklist
        if (configBlacklist != null && configBlacklist.contains(id)) {
            return true;
        }

        // 排除所有弹射物 - 通过名称识别（硬编码后备逻辑）
        // Exclude all projectiles - identified by name (hardcoded fallback)
        return id.contains("fireball") ||
                id.contains("bullet") ||
                id.contains("arrow") ||
                id.contains("projectile");
    }

    /**
     * 检查实体是否在黑名单中 / Check if entity is blacklisted
     * 判断指定实体是否被禁止用于伪装
     *
     * @param entityType   实体类型 / Entity type
     * @param registryName 注册名 / Registry name
     * @return 是否在黑名单中 / Whether in blacklist
     */
    private boolean isStandBlacklisted(EntityType<?> entityType, ResourceLocation registryName) {
        if (registryName == null) {
            return false;
        }
        String id = registryName.toString();

        // 从配置文件中获取黑名单列表 / Get blacklist from config file
        java.util.List<String> configBlacklist = (java.util.List<String>) YellowTemperanceConfig.standDisguiseBlacklist.get();

        // 检查是否在配置黑名单中 / Check if in config blacklist
        return configBlacklist != null && configBlacklist.contains(id);
    }

    /**
     * 设置阶段 / Setup phase
     * 执行模组的主要初始化逻辑
     *
     * @param event FML通用设置事件 / FML common setup event
     */
    private void setup(final FMLCommonSetupEvent event) {
        // 确保只执行一次注册 / Ensure registration only happens once
        if (!packetsRegistered) {
            PacketHandler.registerPackets();
            packetsRegistered = true;
            LOGGER.info("Packet registration complete");
        }

        // 确保能力注册只执行一次 / Ensure capability registration only happens once
        InitCapabilities.registerCapabilities(event);

        // 注册LivingData能力 / Register LivingData capability
        CapabilityHandler.register();
        LOGGER.info("Yellow Temperance addon setup complete");
    }

    // ===== 变形菜单系统 ===== / ===== Disguise Menu System =====

    /**
     * 伪装菜单提供者 / Disguise menu provider
     * 为伪装选择界面提供容器
     */
    public static class DisguiseMenuProvider implements INamedContainerProvider {
        @Override
        public ITextComponent getDisplayName() {
            return new StringTextComponent("选择变形");
        }

        @Override
        public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
            return new DisguiseContainer(id, inv);
        }
    }

    /**
     * 伪装容器 / Disguise container
     * 处理伪装选择界面的物品槽位逻辑
     */
    public static class DisguiseContainer extends Container {
        /**
         * 构造函数 / Constructor
         *
         * @param id        容器ID / Container ID
         * @param playerInv 玩家物品栏 / Player inventory
         */
        public DisguiseContainer(int id, PlayerInventory playerInv) {
            super(ContainerType.GENERIC_9x3, id);

            // 添加生物选择按钮 / Add entity selection buttons
            IItemHandler itemHandler = new ItemStackHandler(RotPYellowTemperanceAddon.ENTITIES_FOR_DISGUISE.size()) {
                @Override
                public ItemStack getStackInSlot(int slot) {
                    EntityType<?> entityType = RotPYellowTemperanceAddon.ENTITIES_FOR_DISGUISE.get(slot);
                    EntitySize size = RotPYellowTemperanceAddon.ENTITY_SIZE_CACHE.get(entityType);

                    // 在物品名称中显示尺寸信息 / Display size information in item name
                    String displayName = String.format("%s (%.1f×%.1f)",
                            entityType.getDescription().getString(),
                            size.width,
                            size.height);

                    return new ItemStack(Items.PAPER).setHoverName(
                            new StringTextComponent(displayName)
                    );
                }
            };

            for (int i = 0; i < RotPYellowTemperanceAddon.ENTITIES_FOR_DISGUISE.size(); i++) {
                addSlot(new SlotItemHandler(itemHandler, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18) {
                    @Override
                    public boolean mayPickup(PlayerEntity player) {
                        return true;
                    }
                });
            }

            // 玩家物品栏 / Player inventory
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 9; ++j) {
                    this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
                }
            }

            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInv, k, 8 + k * 18, 142));
            }
        }

        /**
         * 检查容器是否仍然有效 / Check if container is still valid
         *
         * @param player 玩家实体 / Player entity
         * @return 是否有效 / Whether valid
         */
        @Override
        public boolean stillValid(PlayerEntity player) {
            return true;
        }
    }
}