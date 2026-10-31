package com.TheChaYe.rotp_yellowtemperance.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * 皮肤模型判断工具（common，服务端可用）。
 * 原代码在服务端调用了 net.minecraft.client.resources.DefaultPlayerSkin，
 * 在专用服务器上会触发 RuntimeDistCleaner 崩溃；这里复刻同样的算法，只依赖 authlib。
 */
public final class SkinModelUtil {

    private SkinModelUtil() {}

    /** 从 GameProfile 的 textures 属性判断是否为 slim(Alex) 模型。 */
    public static boolean isSlimFromProfile(GameProfile profile) {
        if (profile == null) return false;
        try {
            if (!profile.getProperties().containsKey("textures")) return false;
            Property property = profile.getProperties().get("textures").iterator().next();
            String json = new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8);
            if (json.contains("\"model\" : \"slim\"") || json.contains("\"model\":\"slim\"")
                    || json.contains("\"model\" :\"slim\"") || json.contains("\"model\": \"slim\"")) {
                return true;
            }
            if (json.contains("\"model\"")) return false;
        } catch (Exception ignored) {
            // 属性损坏时按默认模型处理
        }
        return false;
    }

    /**
     * profile 里是否有"真正的"皮肤贴图（SKIN 指向 textures.minecraft.net 的贴图资源）。
     *
     * <p>用来区分"确实拿到了玩家皮肤"和"没拿到、只是不要伪造一个默认皮肤糊弄渲染"。
     * 合成出来的默认皮肤（例如 .../texture/default 或直接用 Steve/Alex 贴图）不算。</p>
     */
    public static boolean hasSkinTexture(GameProfile profile) {
        if (profile == null) return false;
        try {
            if (!profile.getProperties().containsKey("textures")) return false;
            Property property = profile.getProperties().get("textures").iterator().next();
            String json = new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8);
            return json.contains("\"SKIN\"") && json.contains("textures.minecraft.net/texture/");
        } catch (Exception ignored) {
            return false;
        }
    }

    /** 与 DefaultPlayerSkin.isAlexDefault 完全一致的算法（UUID hash 的奇偶）。 */
    public static boolean isAlexDefault(UUID uuid) {
        return uuid != null && (uuid.hashCode() & 1) == 1;
    }
}
