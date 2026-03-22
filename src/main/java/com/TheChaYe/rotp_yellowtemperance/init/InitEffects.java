package com.TheChaYe.rotp_yellowtemperance.init;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.effects.YellowTemperanceErosionEffect;
import com.TheChaYe.rotp_yellowtemperance.effects.YellowTemperanceProtectionEffect;
import net.minecraft.potion.Effect;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 效果初始化 / Effects Initialization
 * 注册和管理黄色节制相关的药水效果
 */
public class InitEffects {
    /**
     * 效果注册器 / Effects registry
     */
    public static final DeferredRegister<Effect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.POTIONS, RotPYellowTemperanceAddon.MOD_ID);

    /**
     * 黄色节制保护效果 / Yellow Temperance protection effect
     */
    public static final RegistryObject<Effect> YELLOW_TEMPERANCE_PROTECTION =
            EFFECTS.register("yellow_temperance_protection", YellowTemperanceProtectionEffect::new);

    /**
     * 黄色节制侵蚀效果 / Yellow Temperance erosion effect
     */
    public static final RegistryObject<Effect> YELLOW_TEMPERANCE_EROSION =
            EFFECTS.register("yellow_temperance_erosion", YellowTemperanceErosionEffect::new);

}