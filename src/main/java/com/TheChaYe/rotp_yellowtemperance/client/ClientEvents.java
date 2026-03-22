package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端事件处理类 / Client Events Handler
 * 专门处理所有客户端专用的事件，确保服务端不会加载客户端代码
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

}
