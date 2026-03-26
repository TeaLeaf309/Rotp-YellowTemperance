package com.TheChaYe.rotp_yellowtemperance;

import com.TheChaYe.rotp_yellowtemperance.capability.entity.CapabilityHandler;
import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.events.DamageHandler;
import com.TheChaYe.rotp_yellowtemperance.events.PlayerLoginEventHandler;
import com.TheChaYe.rotp_yellowtemperance.init.*;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.registry.EntityDisguiseRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 黄色节制模组主类 / Yellow Temperance Addon Main Class
 * 模组的入口点和核心初始化逻辑
 */
@Mod(RotPYellowTemperanceAddon.MOD_ID)
public class RotPYellowTemperanceAddon {
    /**
     * 模组 ID / Mod ID
     */
    public static final String MOD_ID = "rotp_yellowtemperance";
    /**
     * 模组日志记录器 / Mod logger
     */
    public static final Logger LOGGER = LogManager.getLogger();

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
            // 初始化实体伪装注册表 / Initialize entity disguise registry
            EntityDisguiseRegistry.init();
        });
    }

    /**
     * 设置阶段 / Setup phase
     * 执行模组的主要初始化逻辑
     *
     * @param event FML 通用设置事件 / FML common setup event
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
}
