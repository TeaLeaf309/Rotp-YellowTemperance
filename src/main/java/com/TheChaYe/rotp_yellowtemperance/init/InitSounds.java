package com.TheChaYe.rotp_yellowtemperance.init;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.util.mc.OstSoundList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * 声音初始化 / Sounds Initialization
 * 注册和管理黄色节制相关的音效
 */
public class InitSounds {
    /** 声音事件注册器 / Sound events registry */
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, RotPYellowTemperanceAddon.MOD_ID); // TODO sounds.json

    /** 黄色节制召唤语音 / Yellow Temperance summon voiceline */
    public static final RegistryObject<SoundEvent> YELLOW_TEMPERANCE_SUMMON_VOICELINE = SOUNDS.register("yt_summon",
            () -> new SoundEvent(new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "yt_summon")));

    /** 黄色节制召唤音效 / Yellow Temperance summon sound */
    public static final Supplier<SoundEvent> YELLOW_TEMPERANCE_SUMMON_SOUND = ModSounds.STAND_SUMMON_DEFAULT;

    /** 白色相簿轻拳音效 / White Album light punch sound */
    public static final Supplier<SoundEvent> WHITE_ALBUM_PUNCH_LIGHT = SOUNDS.register("yt_punch",
            () -> new SoundEvent(new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "yt_punch")));

    /** 黄色节制解除召唤音效 / Yellow Temperance unsummon sound */
    public static final Supplier<SoundEvent> YELLOW_TEMPERANCE_UNSUMMON_SOUND = ModSounds.STAND_UNSUMMON_DEFAULT;

    /** 黄色节制OST音乐列表 / Yellow Temperance OST music list */
    public static final OstSoundList YELLOW_TEMPERANCE_OST = new OstSoundList(
            new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "example_stand_ost"), SOUNDS);
}
