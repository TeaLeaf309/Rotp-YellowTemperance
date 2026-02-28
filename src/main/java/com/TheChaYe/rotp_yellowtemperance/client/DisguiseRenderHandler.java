// 文件路径: src/main/java/com/TheChaYe/rotp_yellowtemperance/client/DisguiseRenderHandler.java
package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.entity.YellowTemperanceEntity;
import com.TheChaYe.rotp_yellowtemperance.network.ModNetwork;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 伪装渲染处理器 / Disguise Render Handler
 * 处理玩家伪装效果的核心渲染逻辑和实体管理
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DisguiseRenderHandler {
    public static final Map<UUID, LivingEntity> PERSISTENT_DISGUISE_ENTITIES = new HashMap<>();
    public static final Map<UUID, EntityType<?>> CURRENT_DISGUISE_TYPES = new HashMap<>();
    public static final Map<UUID, Integer> DISGUISE_REMOVAL_TIMERS = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<UUID, Float> ORIGINAL_EYE_HEIGHTS = new HashMap<>();
    private static final Map<UUID, EntitySize> ORIGINAL_DIMENSIONS = new HashMap<>();
    private static final Map<UUID, Float> ORIGINAL_SHADOW_SIZES = new HashMap<>();
    private static final int REMOVAL_DELAY = 100; // 100 ticks = 5秒
    private static final Map<UUID, AbstractClientPlayerEntity> PLAYER_DOLLS = new HashMap<>();
    // 攻击动画计时器映射
    private static final Map<UUID, Integer> ATTACK_ANIMATION_TIMERS = new HashMap<>();
    // 虚拟世界坐标
    private static final Vector3d VOID_POSITION = new Vector3d(0, -1000, 0);
    // 上次发送的物理属性值缓存，用于阈值检查
    private static final Map<UUID, Float> LAST_EYE_HEIGHT_SCALES = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAST_HITBOX_WIDTH_SCALES = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAST_HITBOX_HEIGHT_SCALES = new ConcurrentHashMap<>();
    // 缓存反射字段以提高性能
    private static Field cachedShadowRadiusField = null;
    private static Field ironGolemAttackTimerField = null;
    
    // 查找shadowRadius字段的辅助方法
    private static Field findShadowRadiusField() {
        if (cachedShadowRadiusField != null) {
            return cachedShadowRadiusField;
        }
        
        // 可能的字段名列表（包括开发环境和混淆环境）
        String[] possibleNames = {
            "shadowRadius",           // 开发环境
            "field_76989_e",         // 常见的SRG名称
            "shadowRadius_$",        // 其他可能的混淆名称
            "field_1385"             // 1.16.5版本的映射
        };
        
        cachedShadowRadiusField = findField(EntityRenderer.class, possibleNames);
        
        if (cachedShadowRadiusField == null) {
//            LOGGER.warn("无法找到shadowRadius字段，影子大小调整可能失效");
        } else {
            cachedShadowRadiusField.setAccessible(true);
//            LOGGER.debug("成功找到shadowRadius字段: {}", cachedShadowRadiusField.getName());
        }
        
        return cachedShadowRadiusField;
    }
    // 动画相关反射字段
    private static Field limbSwingField = null;
    private static Field limbSwingAmountField = null;
    private static Field lastHurtField = null;
    private static Field eyeHeightField = null;
    // 用于访问setLivingEntityFlag方法
    private static Method setLivingEntityFlagMethod = null;

    // 冷却时间映射，用于限制网络包发送频率

    // 初始化反射字段
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        try {
            // 初始化铁傀儡攻击计时器字段
            ironGolemAttackTimerField = findField(IronGolemEntity.class,
                    new String[]{"attackAnimationTick", "field_70768_au"});

            // 初始化动画字段
            limbSwingField = findField(LivingEntity.class,
                    new String[]{"limbSwing", "field_70701_bs"});
            limbSwingAmountField = findField(LivingEntity.class,
                    new String[]{"limbSwingAmount", "field_70702_br"});
            lastHurtField = findField(LivingEntity.class,
                    new String[]{"lastHurt", "field_70718_bc"});

            // 眼部高度字段
            eyeHeightField = findField(LivingEntity.class,
                    new String[]{"eyeHeight", "field_213330_aS"});

            // 修复setLivingEntityFlag反射初始化
            try {
                setLivingEntityFlagMethod = LivingEntity.class
                        .getDeclaredMethod("setLivingEntityFlag", int.class, boolean.class);
                setLivingEntityFlagMethod.setAccessible(true);
            } catch (Exception e) {
//                LOGGER.error("初始化setLivingEntityFlag失败", e);
            }

            // 设置可访问性
            if (setLivingEntityFlagMethod != null) setLivingEntityFlagMethod.setAccessible(true);
            if (limbSwingField != null) limbSwingField.setAccessible(true);
            if (limbSwingAmountField != null) limbSwingAmountField.setAccessible(true);
            if (lastHurtField != null) lastHurtField.setAccessible(true);
            if (eyeHeightField != null) eyeHeightField.setAccessible(true);

        } catch (Exception e) {
//            LOGGER.error("初始化反射字段失败", e);
        }
    }

    // 辅助方法：尝试多个字段名找到可用的字段
    private static Field findField(Class<?> clazz, String[] fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                // 直接使用反射获取字段
                Field field = clazz.getDeclaredField(fieldName);
                if (field != null) {
                    return field;
                }
            } catch (NoSuchFieldException e) {
                // 继续尝试下一个字段名
            }
        }
        return null;
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPreRenderLiving(RenderLivingEvent.Pre<? extends LivingEntity, ? extends EntityModel<?>> event) {
        LivingEntity entity = event.getEntity();

        // 仅处理玩家实体
        if (!(entity instanceof PlayerEntity)) return;

        PlayerEntity player = (PlayerEntity) entity;
        UUID playerId = player.getUUID();

        IStandPower.getStandPowerOptional(player).ifPresent(standPower -> {
            if (!(standPower.getStandManifestation() instanceof YellowTemperanceEntity)) {
                handleNonStandPlayer(playerId);
                return;
            }

            YellowTemperanceEntity stand = (YellowTemperanceEntity) standPower.getStandManifestation();
            handlePlayerDisguise(player, stand, event);
        });
    }

    private static void handleNonStandPlayer(UUID playerId) {
        CURRENT_DISGUISE_TYPES.remove(playerId);
        DISGUISE_REMOVAL_TIMERS.putIfAbsent(playerId, REMOVAL_DELAY);
    }

    private static void handlePlayerDisguise(PlayerEntity player, YellowTemperanceEntity stand,
                                             RenderLivingEvent.Pre<?, ?> event) {
        UUID playerId = player.getUUID();

        boolean inSpecialGui = isSpecialGuiOpen();

        if (stand.isUserDisguised() && stand.getUserDisguiseEntity().isPresent()) {
            EntityType<?> currentDisguiseType = stand.getUserDisguiseEntity().get();

            if (isDisguiseTypeChanged(playerId, currentDisguiseType)) {

                removeDisguiseEntity(playerId);
            }

            LivingEntity disguiseEntity = ensureDisguiseEntity(playerId, currentDisguiseType, player.level);

            if (disguiseEntity != null) {
                CURRENT_DISGUISE_TYPES.put(playerId, currentDisguiseType);

                if (inSpecialGui) {
                    // GUI模式：只同步装备和基础姿势，禁止同步动态状态
                    synchronizeBaseState(player, disguiseEntity);
                    // GUI模式下取消原始渲染
                    event.setCanceled(true);
                    // 调用静态模型渲染
                    renderStaticDisguiseModel(disguiseEntity, event.getMatrixStack(), event.getBuffers(), event.getLight());
                    // GUI模式下玩家也保持可见

                } else {
                    // 正常模式：完全同步状态
                    synchronizeEntityState(player, disguiseEntity);
                    // 取消原始渲染
                    event.setCanceled(true);
                    // 渲染伪装实体
                    renderDisguisedEntity(disguiseEntity, event.getMatrixStack(), event.getBuffers(), event.getLight());
                    // 调整玩家影子大小来匹配伪装实体
                    adjustPlayerShadowSize(player, disguiseEntity);

                }
                DISGUISE_REMOVAL_TIMERS.remove(playerId);

            }
        } else {
            CURRENT_DISGUISE_TYPES.remove(playerId);
            DISGUISE_REMOVAL_TIMERS.putIfAbsent(playerId, REMOVAL_DELAY);
            restorePlayerEyeHeight(player);
            restorePlayerBoundingBox(player);
            restorePlayerShadowSize(player);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderStaticDisguiseModel(
            LivingEntity disguiseEntity,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int light) {

        Minecraft mc = Minecraft.getInstance();

        // 重置动画状态（避免异常动作）
        disguiseEntity.animationPosition = 0;
        disguiseEntity.animationSpeed = 0;
        disguiseEntity.swingTime = 0;
        disguiseEntity.attackAnim = 0;
        disguiseEntity.hurtTime = 0;

        // 反射重置旧受伤时间（兼容性处理）
        try {
            Field oHurtTimeField = LivingEntity.class.getDeclaredField("oHurtTime");
            oHurtTimeField.setAccessible(true);
            oHurtTimeField.set(disguiseEntity, 0);
        } catch (Exception ignore) {
        }

        matrixStack.pushPose();
        EntityRendererManager renderManager = mc.getEntityRenderDispatcher();

        // 核心修改：禁用阴影的简化实现
        renderManager.setRenderShadow(false); // 全局禁用影子
        renderManager.render(
                disguiseEntity,
                0, 0, 0,
                0, 1,
                matrixStack,
                buffer,
                light
        );
        renderManager.setRenderShadow(true);  // 恢复阴影设置（安全措施）

        matrixStack.popPose();
    }

    private static void adjustPlayerEyeHeight(PlayerEntity player, LivingEntity disguise) {
        try {
            ORIGINAL_EYE_HEIGHTS.putIfAbsent(player.getUUID(), player.getEyeHeight());
            float disguiseEyeHeight = getDisguiseEyeHeight(disguise, player.getPose());
            float playerEyeHeight = player.getEyeHeight();
            if (disguiseEyeHeight != playerEyeHeight) {
                ScaleData eyeHeightData = ScaleTypes.EYE_HEIGHT.getScaleData(player);
                float scale = disguiseEyeHeight / playerEyeHeight;
                eyeHeightData.setBaseScale(scale);
                eyeHeightData.markForSync(true); // 强制同步到服务端
            }
        } catch (Exception e) {
//            LOGGER.error("调整眼部高度失败", e);
        }

    }

    // 新增：调整玩家影子大小来匹配伪装实体
    private static void adjustPlayerShadowSize(PlayerEntity player, LivingEntity disguiseEntity) {
        try {
            UUID playerId = player.getUUID();

            // 保存原始影子大小（只在第一次伪装时保存）
            if (!ORIGINAL_SHADOW_SIZES.containsKey(playerId)) {
                Minecraft mc = Minecraft.getInstance();
                EntityRenderer<? super PlayerEntity> playerRenderer = mc.getEntityRenderDispatcher().getRenderer(player);
                Field shadowRadiusField = findShadowRadiusField();
                if (shadowRadiusField != null) {
                    shadowRadiusField.setAccessible(true);
                    float originalShadowSize = shadowRadiusField.getFloat(playerRenderer);
                    ORIGINAL_SHADOW_SIZES.put(playerId, originalShadowSize);

                    if (LOGGER.isDebugEnabled()) {
/*                        LOGGER.debug("[影子调整] 保存玩家 {} 的原始影子大小: {}",
                                player.getName().getString(), originalShadowSize);*/
                    }
                }
            }

            // 计算伪装实体应有的影子大小
            EntitySize disguiseSize = disguiseEntity.getDimensions(disguiseEntity.getPose());
            float disguiseShadowSize = calculateAccurateShadowSize(disguiseEntity, disguiseSize);

            // 获取玩家渲染器
            Minecraft mc = Minecraft.getInstance();
            EntityRenderer<? super PlayerEntity> playerRenderer = mc.getEntityRenderDispatcher().getRenderer(player);

            // 通过反射修改PlayerRenderer的shadowRadius字段
            Field shadowRadiusField = findShadowRadiusField();
            if (shadowRadiusField != null) {
                shadowRadiusField.setAccessible(true);
                shadowRadiusField.setFloat(playerRenderer, disguiseShadowSize);

                if (LOGGER.isDebugEnabled()) {
/*                    LOGGER.debug("[影子调整] 玩家: {}, 伪装实体: {}, 设置影子大小: {}",
                            player.getName().getString(),
                            disguiseEntity.getType().getRegistryName(),
                            disguiseShadowSize);*/
                }
            } else {
//                LOGGER.warn("无法调整玩家 {} 的影子大小：找不到shadowRadius字段", player.getName().getString());
            }
        } catch (Exception e) {
//            LOGGER.error("调整玩家影子大小失败", e);
//            LOGGER.error("注意：反射调用在生产环境中可能因字段名混淆而失效");
        }
    }

    // 修改：调整碰撞箱方法
    private static void adjustPlayerBoundingBox(PlayerEntity player, LivingEntity disguise) {

        try {
            if (!ORIGINAL_DIMENSIONS.containsKey(player.getUUID())) {
                ORIGINAL_DIMENSIONS.put(player.getUUID(), player.getDimensions(player.getPose()));
            }

            EntitySize disguiseSize = disguise.getDimensions(player.getPose());
            EntitySize playerSize = player.getDimensions(player.getPose());
            boolean needsSync = false;

            if (disguiseSize.width != playerSize.width) {
                ScaleData hitboxWidthData = ScaleTypes.HITBOX_WIDTH.getScaleData(player);
                float widthScale = disguiseSize.width / playerSize.width;
                hitboxWidthData.setBaseScale(widthScale);
                hitboxWidthData.markForSync(true); // 强制同步到服务端
                needsSync = true;
            }

            if (disguiseSize.height != playerSize.height) {
                ScaleData hitboxHeightData = ScaleTypes.HITBOX_HEIGHT.getScaleData(player);
                float heightScale = disguiseSize.height / playerSize.height;
                hitboxHeightData.setBaseScale(heightScale);
                hitboxHeightData.markForSync(true); // 强制同步到服务端
                needsSync = true;
            }

            // 强制更新玩家的边界框以确保碰撞检测正常工作
            if (needsSync) {
                player.setBoundingBox(disguiseSize.makeBoundingBox(player.position()));
            }
        } catch (Exception e) {
//            LOGGER.error("调整碰撞箱失败", e);
        }
    }

    public static void updatePlayerPhysics(PlayerEntity player) {
        if (CURRENT_DISGUISE_TYPES.containsKey(player.getUUID())) {
            LivingEntity disguise = PERSISTENT_DISGUISE_ENTITIES.get(player.getUUID());
            if (disguise != null) {
                // 获取玩家的替身能力
                IStandPower.getStandPowerOptional(player).ifPresent(standPower -> {
                    if (standPower.getStandManifestation() instanceof YellowTemperanceEntity) {
                        YellowTemperanceEntity stand = (YellowTemperanceEntity) standPower.getStandManifestation();
                        // 确保物理属性更新
                        adjustPlayerPhysics(player, disguise, stand);
                    }
                });
            }
        } else {
            // 恢复原始物理属性
            restorePlayerPhysics(player);
        }
    }

    private static void restorePlayerPhysics(PlayerEntity player) {
        restorePlayerEyeHeight(player);
        restorePlayerBoundingBox(player);

        // +++ 新增：强制姿势以更新眼高 +++
        if (player.level.isClientSide) {
            refreshPlayerPose(player);
        }

        if (player == Minecraft.getInstance().player) {
            Minecraft.getInstance().gameRenderer.tick();

        }
    }

    private static void refreshPlayerPose(PlayerEntity player) {
        if (player.getPose() == Pose.SLEEPING || player.isPassenger() || player.getPose() == Pose.DYING) {
            // 排除无法姿势的状态
        }


    }

    private static void adjustPlayerPhysics(PlayerEntity player, LivingEntity disguise, YellowTemperanceEntity stand) {

        // 先保存原始尺寸
        if (!ORIGINAL_DIMENSIONS.containsKey(player.getUUID())) {
            ORIGINAL_DIMENSIONS.put(player.getUUID(), player.getDimensions(player.getPose()));
        }

        adjustPlayerEyeHeight(player, disguise);
        adjustPlayerBoundingBox(player, disguise);


        // 立即同步物理属性到服务端（仅对本地玩家）
        if (player.level.isClientSide && player == Minecraft.getInstance().player) {
            // 直接同步，不延迟
            syncPlayerPhysicsToServer(player);
        }
    }

    // 同步玩家物理属性到服务端
    public static void syncPlayerPhysicsToServer(PlayerEntity player) {
        try {
            UUID playerId = player.getUUID();

            // 获取当前的缩放值
            ScaleData eyeHeightData = ScaleTypes.EYE_HEIGHT.getScaleData(player);
            ScaleData hitboxWidthData = ScaleTypes.HITBOX_WIDTH.getScaleData(player);
            ScaleData hitboxHeightData = ScaleTypes.HITBOX_HEIGHT.getScaleData(player);


            float eyeHeightScale = MathHelper.clamp(eyeHeightData.getBaseScale(), 0.1f, 10.0f);
            float hitboxWidthScale = MathHelper.clamp(hitboxWidthData.getBaseScale(), 0.1f, 10.0f);
            float hitboxHeightScale = MathHelper.clamp(hitboxHeightData.getBaseScale(), 0.1f, 10.0f);

            // 发送到服务端
            ModNetwork.sendPlayerPhysicsToServer(playerId, eyeHeightScale, hitboxWidthScale, hitboxHeightScale);
//            LOGGER.debug("Sent physics sync packet for player {} - Eye: {}, Width: {}, Height: {}",
//                    player.getName().getString(), eyeHeightScale, hitboxWidthScale, hitboxHeightScale);
        } catch (Exception e) {
//            LOGGER.error("Failed to sync player physics to server", e);
        }
    }

    private static void restorePlayerEyeHeight(PlayerEntity player) {
        UUID uuid = player.getUUID();
        if (ORIGINAL_EYE_HEIGHTS.containsKey(uuid)) {
            try {
                float originalEyeHeight = ORIGINAL_EYE_HEIGHTS.get(uuid);
                boolean success = false;

                // 使用反射设置玩家的眼部高度
                if (eyeHeightField != null) {
                    try {
                        eyeHeightField.setAccessible(true);
                        eyeHeightField.setFloat(player, originalEyeHeight);
                        success = true;
/*                        LOGGER.debug("通过eyeHeightField成功恢复玩家 {} 的眼高", player.getName().getString());*/
                    } catch (Exception e) {
//                        LOGGER.error("通过eyeHeightField恢复眼高失败", e);
                    }
                }

                // 如果主要方法失败，尝试备用方法
                if (!success) {
                    try {
                        // 尝试直接通过Entity类获取字段
                        Field field = Entity.class.getDeclaredField("eyeHeight");
                        field.setAccessible(true);
                        field.setFloat(player, originalEyeHeight);
                        success = true;
/*                        LOGGER.debug("通过备用方法成功恢复玩家 {} 的眼高", player.getName().getString());*/
                    } catch (Exception e) {
//                        LOGGER.error("备用方法恢复眼高失败", e);
                    }
                }

                // 强制玩家的维度以确保眼高更新生效
                if (success) {

//                    LOGGER.debug("成功恢复玩家 {} 的眼高为: {}", player.getName().getString(), originalEyeHeight);
                } else {
//                    LOGGER.error("所有方法都未能成功恢复玩家 {} 的眼高", player.getName().getString());
                }

                ORIGINAL_EYE_HEIGHTS.remove(uuid);
            } catch (Exception e) {
//                LOGGER.error("恢复眼部高度失败", e);
            }
        }
    }

    private static void restorePlayerBoundingBox(PlayerEntity player) {
        UUID uuid = player.getUUID();
        if (ORIGINAL_DIMENSIONS.containsKey(uuid)) {
            try {
                EntitySize originalSize = ORIGINAL_DIMENSIONS.get(uuid);

                // 使用Pehkui API恢复原始尺寸
                ScaleData widthData = ScaleTypes.HITBOX_WIDTH.getScaleData(player);
                ScaleData heightData = ScaleTypes.HITBOX_HEIGHT.getScaleData(player);
                ScaleData eyeHeightData = ScaleTypes.EYE_HEIGHT.getScaleData(player);

                widthData.setBaseScale(1.0f);
                widthData.markForSync(true); // 强制同步到服务端
                heightData.setBaseScale(1.0f);
                heightData.markForSync(true); // 强制同步到服务端
                eyeHeightData.setBaseScale(1.0f);
                eyeHeightData.markForSync(true); // 强制同步到服务端

                ORIGINAL_DIMENSIONS.remove(uuid);

            } catch (Exception e) {
//                LOGGER.error("恢复碰撞箱失败", e);
            }
        }
    }

    // 新增：恢复玩家原始影子大小
    private static void restorePlayerShadowSize(PlayerEntity player) {
        UUID playerId = player.getUUID();
        if (ORIGINAL_SHADOW_SIZES.containsKey(playerId)) {
            try {
                float originalShadowSize = ORIGINAL_SHADOW_SIZES.get(playerId);

                // 获取玩家渲染器
                Minecraft mc = Minecraft.getInstance();
                EntityRenderer<? super PlayerEntity> playerRenderer = mc.getEntityRenderDispatcher().getRenderer(player);

                // 通过反射恢复PlayerRenderer的shadowRadius字段
                Field shadowRadiusField = findShadowRadiusField();
                if (shadowRadiusField != null) {
                    shadowRadiusField.setAccessible(true);
                    shadowRadiusField.setFloat(playerRenderer, originalShadowSize);

                    ORIGINAL_SHADOW_SIZES.remove(playerId);

                    if (LOGGER.isDebugEnabled()) {
/*                        LOGGER.debug("[影子恢复] 恢复玩家 {} 的原始影子大小: {}",
                                player.getName().getString(), originalShadowSize);*/
                    }
                } else {
                    LOGGER.warn("无法恢复玩家 {} 的影子大小：找不到shadowRadius字段", player.getName().getString());
                }
            } catch (Exception e) {
//                LOGGER.error("恢复玩家影子大小失败", e);
//                LOGGER.error("注意：反射调用在生产环境中可能因字段名混淆而失效");
            }
        }
    }

    // 新增：获取玩家当前物理属性缩放值
    public static float[] getPlayerPhysicsScales(PlayerEntity player) {
        UUID playerId = player.getUUID();

        float eyeHeightScale = LAST_EYE_HEIGHT_SCALES.getOrDefault(playerId, 1.0f);
        float hitboxWidthScale = LAST_HITBOX_WIDTH_SCALES.getOrDefault(playerId, 1.0f);
        float hitboxHeightScale = LAST_HITBOX_HEIGHT_SCALES.getOrDefault(playerId, 1.0f);

        return new float[]{eyeHeightScale, hitboxWidthScale, hitboxHeightScale};
    }

    // 新增：更新物理属性缓存
    public static void updatePhysicsCache(PlayerEntity player, float eyeHeightScale, float hitboxWidthScale, float hitboxHeightScale) {
        UUID playerId = player.getUUID();
        LAST_EYE_HEIGHT_SCALES.put(playerId, eyeHeightScale);
        LAST_HITBOX_WIDTH_SCALES.put(playerId, hitboxWidthScale);
        LAST_HITBOX_HEIGHT_SCALES.put(playerId, hitboxHeightScale);
    }

    private static float getDisguiseEyeHeight(LivingEntity disguise, Pose pose) {


        // 默认使用实体的眼部高度
        return disguise.getEyeHeight(pose);
    }

    private static boolean isDisguiseTypeChanged(UUID playerId, EntityType<?> newType) {
        return CURRENT_DISGUISE_TYPES.containsKey(playerId) &&
                CURRENT_DISGUISE_TYPES.get(playerId) != newType;

    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                // 当玩家退出世界时，清理所有缓存数据
                clearAllCaches();
                return;
            }

            // 获取当前客户端玩家
            PlayerEntity clientPlayer = Minecraft.getInstance().player;

            // 更新攻击动画计时器
            ATTACK_ANIMATION_TIMERS.replaceAll((uuid, timer) -> timer > 0 ? timer - 1 : 0);

            // 更新伪装实体状态
            PERSISTENT_DISGUISE_ENTITIES.values().forEach(DisguiseRenderHandler::updateEntityState);

            // === 修复并发修改问题 ===
            // 安全处理伪装实体移除计时器
            Set<UUID> toRemove = new HashSet<>();
            Map<UUID, Integer> updatedTimers = new HashMap<>();

            for (Map.Entry<UUID, Integer> entry : DISGUISE_REMOVAL_TIMERS.entrySet()) {
                int ticksLeft = entry.getValue() - 1;
                if (ticksLeft <= 0) {
                    toRemove.add(entry.getKey());
                } else {
                    updatedTimers.put(entry.getKey(), ticksLeft);
                }
            }

            // 批量更新计时器
            DISGUISE_REMOVAL_TIMERS.clear();
            DISGUISE_REMOVAL_TIMERS.putAll(updatedTimers);

            // 批量移除实体
            toRemove.forEach(DisguiseRenderHandler::removeDisguiseEntity);

            // === 关键修复：统一物理属性更新 ===
            if (clientPlayer != null) {
                // 更新玩家的物理属性（眼高和碰撞箱）
                updatePlayerPhysics(clientPlayer);

            }
        }
    }

    /**
     * 清理所有缓存数据
     */
    private static void clearAllCaches() {
        PERSISTENT_DISGUISE_ENTITIES.clear();
        CURRENT_DISGUISE_TYPES.clear();
        DISGUISE_REMOVAL_TIMERS.clear();
        ORIGINAL_EYE_HEIGHTS.clear();
        ORIGINAL_DIMENSIONS.clear();
        PLAYER_DOLLS.clear();
        ATTACK_ANIMATION_TIMERS.clear();
        LAST_EYE_HEIGHT_SCALES.clear();
        LAST_HITBOX_WIDTH_SCALES.clear();
        LAST_HITBOX_HEIGHT_SCALES.clear();
    }

    private static void updateEntityState(LivingEntity entity) {
        if (entity != null) {
            // 保存上一帧的位置和旋转
            entity.xo = entity.getX();
            entity.yo = entity.getY();
            entity.zo = entity.getZ();
            entity.yRotO = entity.yRot;
            entity.xRotO = entity.xRot;

            // 更新动画参数以保持流畅
            entity.animationSpeedOld = entity.animationSpeed;

            // 模拟实体的基本tick行为以维持动画
            if (entity.isOnGround() && entity.animationSpeed > 0) {
                entity.animationSpeed *= 0.9F; // 地面摩擦减速
            }

            // 确保受伤动画正确衰减
            if (entity.hurtTime > 0) {
                entity.hurtTime--;
            }

            // 确保攻击动画正确衰减
            if (entity.attackAnim > 0) {
                entity.attackAnim -= 1.0F;
            }

            // 更新摆动动画
            if (entity.swingTime > 0) {
                entity.swingTime--;
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onWorldUnload(WorldEvent.Unload event) {
        PERSISTENT_DISGUISE_ENTITIES.values().forEach(Entity::remove);
        PERSISTENT_DISGUISE_ENTITIES.clear();
        CURRENT_DISGUISE_TYPES.clear();
        DISGUISE_REMOVAL_TIMERS.clear();
        ATTACK_ANIMATION_TIMERS.clear();
        ORIGINAL_EYE_HEIGHTS.clear();
        ORIGINAL_DIMENSIONS.clear();
    }

    private static LivingEntity ensureDisguiseEntity(UUID playerId, EntityType<?> disguiseType, World world) {
        LivingEntity current = PERSISTENT_DISGUISE_ENTITIES.get(playerId);
        if (current != null && current.getType() == disguiseType) {
            return current;
        }
        return createNewDisguiseEntity(playerId, disguiseType, world);
    }

    private static LivingEntity createNewDisguiseEntity(UUID playerId, EntityType<?> disguiseType, World world) {
        removeDisguiseEntity(playerId);

        Entity entity = disguiseType.create(world);
        if (entity instanceof LivingEntity) {
            LivingEntity disguise = (LivingEntity) entity;
            initializeEntityPosition(disguise);
            PERSISTENT_DISGUISE_ENTITIES.put(playerId, disguise);

            // 初始化攻击动画状态
            if (hasAttackAnimationField(disguise)) {
                setEntityAggressive(disguise, false);
                ATTACK_ANIMATION_TIMERS.put(playerId, 0);
            }

            return disguise;
        }
        return null;
    }

    private static void initializeEntityPosition(LivingEntity entity) {
        entity.setPos(VOID_POSITION.x, VOID_POSITION.y, VOID_POSITION.z);
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.yRotO = entity.yRot;
        entity.xRotO = entity.xRot;
    }

    /**
     * 初始化实体动画状态，确保流畅的初始渲染
     */
    private static void initializeEntityAnimations(LivingEntity entity) {
        // 初始化动画参数
        entity.animationPosition = 0.0F;
        entity.animationSpeed = 0.0F;
        entity.animationSpeedOld = 0.0F;

        // 初始化攻击相关动画
        entity.attackAnim = 0.0F;
        entity.oAttackAnim = 0.0F;
        entity.swingTime = 0;
        entity.hurtTime = 0;
        entity.hurtDuration = 0;

        // 初始化移动动画参数
        try {
            if (limbSwingField != null) limbSwingField.setFloat(entity, 0.0F);
            if (limbSwingAmountField != null) limbSwingAmountField.setFloat(entity, 0.0F);
        } catch (Exception ignored) {
        }

        // 确保实体处于站立状态
        entity.setPose(Pose.STANDING);
        entity.setOnGround(true);
    }

    public static void removeDisguiseEntity(UUID playerId) {
        LivingEntity disguise = PERSISTENT_DISGUISE_ENTITIES.remove(playerId);
        if (disguise != null) {
            disguise.remove();

            // 清理攻击动画状态
            if (hasAttackAnimationField(disguise)) {
                setEntityAggressive(disguise, false);
                try {
                    if (ironGolemAttackTimerField != null) {
                        ironGolemAttackTimerField.setInt(disguise, 0);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        CURRENT_DISGUISE_TYPES.remove(playerId);
        DISGUISE_REMOVAL_TIMERS.remove(playerId);
        ATTACK_ANIMATION_TIMERS.remove(playerId);
    }

    public static boolean isDisguisedEntity(Entity entity) {
        return PERSISTENT_DISGUISE_ENTITIES.containsValue(entity);
    }

    private static boolean isSpecialGuiOpen() {
        Minecraft mc = Minecraft.getInstance();

        return mc.screen instanceof InventoryScreen || mc.screen instanceof CreativeScreen;
    }

    private static void synchronizeBaseState(PlayerEntity player, LivingEntity disguise) {
        if (disguise == null) return;

        disguise.setItemSlot(EquipmentSlotType.MAINHAND, player.getMainHandItem());
        disguise.setItemSlot(EquipmentSlotType.OFFHAND, player.getOffhandItem());
        disguise.setItemSlot(EquipmentSlotType.HEAD, player.getItemBySlot(EquipmentSlotType.HEAD));
        disguise.setItemSlot(EquipmentSlotType.CHEST, player.getItemBySlot(EquipmentSlotType.CHEST));
        disguise.setItemSlot(EquipmentSlotType.LEGS, player.getItemBySlot(EquipmentSlotType.LEGS));
        disguise.setItemSlot(EquipmentSlotType.FEET, player.getItemBySlot(EquipmentSlotType.FEET));

        disguise.swingTime = player.swingTime;
        disguise.attackAnim = player.attackAnim;
        disguise.oAttackAnim = player.oAttackAnim;
        disguise.swinging = player.swinging;
        disguise.hurtTime = player.hurtTime;
        disguise.hurtDuration = player.hurtDuration;
        disguise.setOnGround(player.isOnGround());
        disguise.setPose(player.getPose());

        disguise.setSwimming(player.isSwimming());

        disguise.yRot = player.yRot;
        disguise.xRot = player.xRot;
        disguise.yBodyRot = player.yBodyRot;
        disguise.yHeadRot = player.yHeadRot;
    }


    private static void synchronizeEntityState(PlayerEntity player, LivingEntity disguise) {
        if (disguise == null) return;


        Vector3d playerPos = player.position();
        disguise.setPos(playerPos.x, playerPos.y, playerPos.z);
        disguise.setDeltaMovement(player.getDeltaMovement());

        disguise.setItemSlot(EquipmentSlotType.MAINHAND, player.getMainHandItem());
        disguise.setItemSlot(EquipmentSlotType.OFFHAND, player.getOffhandItem());
        disguise.setItemSlot(EquipmentSlotType.HEAD, player.getItemBySlot(EquipmentSlotType.HEAD));
        disguise.setItemSlot(EquipmentSlotType.CHEST, player.getItemBySlot(EquipmentSlotType.CHEST));
        disguise.setItemSlot(EquipmentSlotType.LEGS, player.getItemBySlot(EquipmentSlotType.LEGS));
        disguise.setItemSlot(EquipmentSlotType.FEET, player.getItemBySlot(EquipmentSlotType.FEET));

        // 同步使用物品状态（如拉弓）
        synchronizeUsingItemState(player, disguise);

        disguise.swingTime = player.swingTime;
        disguise.attackAnim = player.attackAnim;
        disguise.oAttackAnim = player.oAttackAnim;

        // 同步通用攻击动画
        if (player.swinging) {
            Hand hand = player.getUsedItemHand();
            if (hand == null) hand = Hand.MAIN_HAND;

            // 如果有攻击动画字段则不触发通用动画（由Mixin处理）
            if (!hasAttackAnimationField(disguise)) {
                disguise.swing(hand);
            }
        }
        disguise.swinging = player.swinging;
        disguise.attackAnim = player.attackAnim;
        disguise.oAttackAnim = player.oAttackAnim;
        disguise.hurtTime = player.hurtTime;
        disguise.hurtDuration = player.hurtDuration;

        if (player.getLastHurtByMob() != null) {
            disguise.setLastHurtByMob(player.getLastHurtByMob());
        }

        disguise.yRot = player.yRot;
        disguise.xRot = player.xRot;
        disguise.yBodyRot = player.yBodyRot;
        disguise.yHeadRot = player.yHeadRot;

        disguise.xo = player.xo;
        disguise.yo = player.yo;
        disguise.zo = player.zo;
        disguise.yRotO = player.yRotO;
        disguise.xRotO = player.xRotO;
        disguise.yBodyRotO = player.yBodyRotO;
        disguise.yHeadRotO = player.yHeadRotO;

        disguise.animationPosition = player.animationPosition;
        disguise.animationSpeed = player.animationSpeed;
        disguise.animationSpeedOld = player.animationSpeedOld;

        // 同步行走动画参数
        try {
            if (limbSwingField != null && limbSwingAmountField != null) {
                float limbSwing = (float) limbSwingField.get(player);
                float limbSwingAmount = (float) limbSwingAmountField.get(player);
                limbSwingField.set(disguise, limbSwing);
                limbSwingAmountField.set(disguise, limbSwingAmount);
            }
        } catch (Exception e) {
            disguise.animationPosition = player.animationPosition;
            disguise.animationSpeed = player.animationSpeed;
            disguise.animationSpeedOld = player.animationSpeedOld;
        }

        disguise.setOnGround(player.isOnGround());
        disguise.verticalCollision = player.verticalCollision;
        disguise.horizontalCollision = player.horizontalCollision;

        disguise.setPose(player.getPose());

        disguise.setSwimming(player.isSwimming());

        // ===== 攻击动画处理 =====
        if (hasAttackAnimationField(disguise)) {
            handleAttackAnimation(player, disguise);
        }


        // 同步受伤时间戳
        try {
            if (lastHurtField != null) {
                int playerLastHurt = (int) lastHurtField.get(player);
                lastHurtField.set(disguise, playerLastHurt);
            }
        } catch (Exception ignored) {
        }
    }

    private static void handleAttackAnimation(PlayerEntity player, LivingEntity entity) {
        try {
            if (ironGolemAttackTimerField == null) {
                ironGolemAttackTimerField = findField(IronGolemEntity.class,
                        new String[]{"attackAnimationTick", "field_70768_au"});
                if (ironGolemAttackTimerField != null) {
                    ironGolemAttackTimerField.setAccessible(true);
                }
            }

            if (ironGolemAttackTimerField == null) return;

            Integer timer = ATTACK_ANIMATION_TIMERS.computeIfAbsent(player.getUUID(), k -> 0);

            // 仅在玩家攻击开始时且当前没有动画时触发
            if (player.swinging && timer <= 0) {
                timer = 10; // 设置完整动画时长
                // 防止动画期间重复触发
                player.swingTime = 0;
                player.swinging = false;
            }

            // 同步计时器值到实体
            ironGolemAttackTimerField.setInt(entity, timer);

            // 严格根据动画状态设置攻击姿态
            setEntityAggressive(entity, timer > 0);

            // 更新计时器映射
            ATTACK_ANIMATION_TIMERS.put(player.getUUID(), timer);

        } catch (Exception e) {
//            LOGGER.error("处理攻击动画失败", e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderDisguisedEntityWithCorrectShadow(
            LivingEntity disguiseEntity,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int light,
            PlayerEntity player) {

        Minecraft mc = Minecraft.getInstance();
        float partialTicks = mc.getFrameTime();

        // 预先计算位置插值，确保动画流畅
        double interpX = MathHelper.lerp(partialTicks, disguiseEntity.xo, disguiseEntity.getX());
        double interpY = MathHelper.lerp(partialTicks, disguiseEntity.yo, disguiseEntity.getY());
        double interpZ = MathHelper.lerp(partialTicks, disguiseEntity.zo, disguiseEntity.getZ());

        // 保存原始位置
        double originalX = disguiseEntity.getX();
        double originalY = disguiseEntity.getY();
        double originalZ = disguiseEntity.getZ();

        // 临时设置插值位置用于渲染
        disguiseEntity.setPosRaw(interpX, interpY, interpZ);

        // 获取正确的旋转角度
        float renderYaw = disguiseEntity.yRot;

        matrixStack.pushPose();
        EntityRendererManager renderManager = mc.getEntityRenderDispatcher();

        // 获取渲染器
        EntityRenderer<? super LivingEntity> renderer = renderManager.getRenderer(disguiseEntity);

        // 更精确的阴影大小计算
        EntitySize disguiseSize = disguiseEntity.getDimensions(disguiseEntity.getPose());
        float correctShadowSize = calculateAccurateShadowSize(disguiseEntity, disguiseSize);

        // 只在DEBUG模式下输出详细日志，避免影响性能
        if (LOGGER.isDebugEnabled()) {
/*            LOGGER.debug("[阴影调试] 伪装实体: {}, 尺寸: {}x{}, 计算阴影大小: {}",
                    disguiseEntity.getType().getRegistryName(), disguiseSize.width, disguiseSize.height, correctShadowSize);*/
        }

        // 先使用renderManager渲染影子（使用原始位置）
        disguiseEntity.setPosRaw(originalX, originalY, originalZ);
        try {
            Method renderShadowMethod = EntityRendererManager.class.getDeclaredMethod("renderShadow", MatrixStack.class, IRenderTypeBuffer.class, Entity.class, float.class, float.class, int.class, float.class);
            renderShadowMethod.setAccessible(true);
            renderShadowMethod.invoke(renderManager, matrixStack, buffer, disguiseEntity, 1.0f, 1.0f, light, 1.0f);
        } catch (Exception e) {
            LOGGER.warn("无法渲染影子: {}", e.getMessage());
        }

        // 再使用renderer.render渲染实体本身（使用插值位置保证流畅）
        disguiseEntity.setPosRaw(interpX, interpY, interpZ);
        renderer.render(disguiseEntity, renderYaw, partialTicks, matrixStack, buffer, light);

        matrixStack.popPose();

        // 恢复原始位置
        disguiseEntity.setPosRaw(originalX, originalY, originalZ);
    }

    private static float calculateAccurateShadowSize(LivingEntity disguiseEntity, EntitySize size) {
        try {
            // 获取目标生物的渲染器
            Minecraft mc = Minecraft.getInstance();
            EntityRenderer<? super LivingEntity> renderer = mc.getEntityRenderDispatcher().getRenderer(disguiseEntity);
            
            // 通过反射获取该生物渲染器的shadowRadius字段
            Field shadowRadiusField = findShadowRadiusField();
            if (shadowRadiusField != null) {
                shadowRadiusField.setAccessible(true);
                float actualShadowSize = shadowRadiusField.getFloat(renderer);
                
                if (LOGGER.isDebugEnabled()) {
/*                    LOGGER.debug("[影子大小] 实体: {}, 实际影子大小: {}",
                            disguiseEntity.getType().getRegistryName(), actualShadowSize);*/
                }
                
                return actualShadowSize;
            }
        } catch (Exception e) {
            LOGGER.warn("无法获取实体 {} 的影子大小，使用默认计算",
                    disguiseEntity.getType().getRegistryName(), e);
        }
        // 备用方案：基于宽度的0.6倍
        return Math.max(size.width * 0.6f, 0.2f);
    }

    // 检查实体是否具有攻击动画字段
    private static boolean hasAttackAnimationField(LivingEntity entity) {
        try {
            if (entity instanceof IronGolemEntity) {
                // 对于铁傀儡，我们已经知道它有这个字段
                return true;
            }

            // 尝试获取攻击动画字段
            Field attackField = findField(entity.getClass(),
                    new String[]{"attackAnimationTick", "field_70768_au"});
            return attackField != null;
        } catch (Exception e) {
            return false;
        }
    }

    // 设置实体的攻击状态
    private static void setEntityAggressive(LivingEntity entity, boolean aggressive) {
        if (entity instanceof IronGolemEntity) {
            ((IronGolemEntity) entity).setAggressive(aggressive);
        }
        // 可以根据需要添加其他具有攻击状态的实体类型
    }

    // 检查实体是否正在使用物品（如拉弓）
    private static boolean isEntityUsingItem(LivingEntity entity) {
        return entity.getUseItemRemainingTicks() > 0;
    }

    // 同步使用物品的状态（如拉弓）
    private static void synchronizeUsingItemState(PlayerEntity player, LivingEntity disguise) {
        // 同步拉弓等使用物品的状态
        if (player.isUsingItem()) {
            disguise.startUsingItem(player.getUsedItemHand());
            // 同步使用物品的剩余ticks，这对于弓和弩的动画非常重要
            try {
                // 使用反射设置实体标志位
                if (setLivingEntityFlagMethod != null) {
                    setLivingEntityFlagMethod.invoke(disguise, 5, true); // Active item flag
                }
            } catch (Exception e) {
                LOGGER.warn("无法设置实体标志位: {}", e.getMessage());
            }
        } else {
            if (disguise.isUsingItem()) {
                disguise.stopUsingItem();
            }
            try {
                // 使用反射设置实体标志位
                if (setLivingEntityFlagMethod != null) {
                    setLivingEntityFlagMethod.invoke(disguise, 5, false); // Active item flag
                }
            } catch (Exception e) {
                LOGGER.warn("无法设置实体标志位: {}", e.getMessage());
            }
        }

        // 同步使用物品的tick计数
        try {
            if (player.isUsingItem()) {
                // 获取玩家的使用物品剩余ticks
                int useItemRemainingTicks = player.getUseItemRemainingTicks();
                // 使用反射设置伪装实体的使用物品剩余ticks
                Field useItemRemainingTicksField = findField(LivingEntity.class,
                        new String[]{"useItemRemainingTicks", "field_184628_bn"});
                if (useItemRemainingTicksField != null) {
                    useItemRemainingTicksField.setAccessible(true);
                    useItemRemainingTicksField.setInt(disguise, useItemRemainingTicks);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("无法同步使用物品的ticks: {}", e.getMessage());
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static <T extends Entity> void renderDisguisedEntity(
            T disguiseEntity,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int light) {


        UUID playerId = null;
        for (Map.Entry<UUID, LivingEntity> entry : PERSISTENT_DISGUISE_ENTITIES.entrySet()) {
            if (entry.getValue().getUUID().equals(disguiseEntity.getUUID())) {
                playerId = entry.getKey();
                break;
            }
        }
        if (playerId != null && DISGUISE_REMOVAL_TIMERS.containsKey(playerId)) {
            return;
        }

        if (disguiseEntity == null || isInVoidPosition(disguiseEntity)) return;

        float partialTicks = Minecraft.getInstance().getFrameTime();
        double interpolatedX = MathHelper.lerp(partialTicks, disguiseEntity.xo, disguiseEntity.getX());
        double interpolatedY = MathHelper.lerp(partialTicks, disguiseEntity.yo, disguiseEntity.getY());
        double interpolatedZ = MathHelper.lerp(partialTicks, disguiseEntity.zo, disguiseEntity.getZ());

        double origX = disguiseEntity.getX();
        double origY = disguiseEntity.getY();
        double origZ = disguiseEntity.getZ();

        disguiseEntity.setPos(interpolatedX, interpolatedY, interpolatedZ);

        boolean wasInvisible = disguiseEntity.isInvisible();
        disguiseEntity.setInvisible(false);

        // 应用模型高度偏移
        matrixStack.pushPose();


        EntityRendererManager manager = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super T> renderer = manager.getRenderer(disguiseEntity);
        renderer.render(disguiseEntity, disguiseEntity.yRot, partialTicks, matrixStack, buffer, light);

        matrixStack.popPose();

        disguiseEntity.setInvisible(wasInvisible);
        disguiseEntity.setPos(origX, origY, origZ);
    }


    private static boolean isInVoidPosition(Entity entity) {
        return entity.distanceToSqr(VOID_POSITION) < 0.1;
    }
}