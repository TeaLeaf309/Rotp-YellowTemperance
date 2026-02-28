package com.TheChaYe.rotp_yellowtemperance.util;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

import java.util.Map;
import java.util.UUID;

/**
 * 游戏档案序列化器 / Game Profile Serializer
 * 处理GameProfile对象与NBT数据之间的序列化和反序列化
 * Handles serialization and deserialization between GameProfile objects and NBT data
 */
public class GameProfileSerializer {
    private static final String PROPERTIES_KEY = "Properties";
    private static final String PROP_NAME_KEY = "Name";
    private static final String PROP_VALUE_KEY = "Value";
    private static final String PROP_SIG_KEY = "Signature";

    /**
     * 序列化游戏档案 / Serialize Game Profile
     * 将GameProfile对象转换为NBT数据以便网络传输和存储
     * @param profile 游戏档案 / Game profile
     * @return 序列化的NBT数据 / Serialized NBT data
     */
    public static CompoundNBT serialize(GameProfile profile) {
        CompoundNBT nbt = new CompoundNBT();

        // 序列化UUID / Serialize UUID
        if (profile.getId() != null) {
            nbt.putString("Id", profile.getId().toString()); // 1.16.5兼容版本
        }

        // 序列化用户名 / Serialize username
        if (profile.getName() != null) {
            nbt.putString("Name", profile.getName());
        }

        // 序列化属性 / Serialize properties
        CompoundNBT propertiesNbt = new CompoundNBT();
        for (Map.Entry<String, Property> entry : profile.getProperties().entries()) {
            ListNBT propList = new ListNBT();

            CompoundNBT propNbt = new CompoundNBT();
            propNbt.putString(PROP_NAME_KEY, entry.getValue().getName()); // 1.16.5兼容方法
            propNbt.putString(PROP_VALUE_KEY, entry.getValue().getValue()); // 1.16.5兼容方法

            if (entry.getValue().hasSignature()) {
                propNbt.putString(PROP_SIG_KEY, entry.getValue().getSignature()); // 1.16.5兼容方法
            }

            propList.add(propNbt);
            propertiesNbt.put(entry.getKey(), propList);
        }

        nbt.put(PROPERTIES_KEY, propertiesNbt);
        return nbt;
    }

    /**
     * 反序列化游戏档案 / Deserialize Game Profile
     * 将NBT数据转换回GameProfile对象
     * @param nbt NBT数据 / NBT data
     * @return 反序列化的游戏档案 / Deserialized game profile
     */
    public static GameProfile deserialize(CompoundNBT nbt) {
        // 反序列化UUID - 1.16.5兼容版本 / Deserialize UUID - 1.16.5 compatible version
        UUID uuid = null;
        if (nbt.contains("Id", Constants.NBT.TAG_STRING)) {
            try {
                uuid = UUID.fromString(nbt.getString("Id"));
            } catch (IllegalArgumentException e) {
                RotPYellowTemperanceAddon.LOGGER.error("Invalid UUID format: {}", nbt.getString("Id"));
            }
        }

        // 反序列化用户名 / Deserialize username
        String name = nbt.contains("Name", Constants.NBT.TAG_STRING) ?
                nbt.getString("Name") : null;

        GameProfile profile = new GameProfile(uuid, name);

        // 反序列化属性 / Deserialize properties
        if (nbt.contains(PROPERTIES_KEY, Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT propertiesNbt = nbt.getCompound(PROPERTIES_KEY);

            for (String key : propertiesNbt.getAllKeys()) {
                if (propertiesNbt.contains(key, Constants.NBT.TAG_LIST)) {
                    ListNBT propList = propertiesNbt.getList(key, Constants.NBT.TAG_COMPOUND);

                    for (int i = 0; i < propList.size(); i++) {
                        CompoundNBT propNbt = propList.getCompound(i);
                        String propName = propNbt.getString(PROP_NAME_KEY);
                        String value = propNbt.getString(PROP_VALUE_KEY);
                        String signature = propNbt.contains(PROP_SIG_KEY) ?
                                propNbt.getString(PROP_SIG_KEY) : null;

                        profile.getProperties().put(key, new Property(propName, value, signature));
                    }
                }
            }
        }

        return profile;
    }
}