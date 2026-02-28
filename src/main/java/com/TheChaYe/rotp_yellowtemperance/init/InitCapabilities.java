package com.TheChaYe.rotp_yellowtemperance.init;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.capability.DisguisePlayerCapability;
import com.TheChaYe.rotp_yellowtemperance.capability.IDisguiseCapability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import javax.annotation.Nullable;

/**
 * 能力系统初始化 / Capabilities System Initialization
 * 管理伪装功能的能力系统注册和事件处理
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID)
public class InitCapabilities {
    /** 伪装能力标识符 / Disguise capability identifier */
    public static final ResourceLocation DISGUISE_CAPABILITY_ID =
            new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "disguise");
    /** 伪装能力注入 / Disguise capability injection */
    @CapabilityInject(IDisguiseCapability.class)
    public static Capability<IDisguiseCapability> DISGUISE_CAPABILITY = null;

    /**
     * 附加能力事件处理 / Attach capabilities event handler
     * 为玩家实体附加伪装能力
     */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            event.addCapability(DISGUISE_CAPABILITY_ID, new DisguisePlayerCapability());
        }
    }

    /**
     * 注册能力系统 / Register capabilities system
     * 初始化伪装能力的序列化和反序列化逻辑
     */
    public static void registerCapabilities(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CapabilityManager.INSTANCE.register(
                    IDisguiseCapability.class,
                    new Capability.IStorage<IDisguiseCapability>() {
                        @Nullable
                        @Override
                        public INBT writeNBT(Capability<IDisguiseCapability> capability,
                                             IDisguiseCapability instance,
                                             Direction side) {
                            return instance.serializeNBT();
                        }

                        @Override
                        public void readNBT(Capability<IDisguiseCapability> capability,
                                            IDisguiseCapability instance,
                                            Direction side,
                                            INBT nbt) {
                            if (nbt instanceof CompoundNBT) {
                                instance.deserializeNBT((CompoundNBT) nbt);
                            }
                        }
                    },
                    DisguisePlayerCapability.DisguiseData::new
            );
        });
    }
}