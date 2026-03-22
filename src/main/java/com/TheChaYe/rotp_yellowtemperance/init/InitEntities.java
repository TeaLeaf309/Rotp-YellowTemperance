package com.TheChaYe.rotp_yellowtemperance.init;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import net.minecraft.entity.EntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 实体初始化 / Entities Initialization
 * 管理模组中自定义实体的注册
 */
public class InitEntities {
    /**
     * 实体注册器 / Entities registry
     */
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.ENTITIES, RotPYellowTemperanceAddon.MOD_ID);

}
