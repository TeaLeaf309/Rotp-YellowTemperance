package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.client.render.DisguiseLayer;
import com.TheChaYe.rotp_yellowtemperance.client.render.ErosionLayer;
import com.TheChaYe.rotp_yellowtemperance.client.render.YellowTemperanceRenderer;
import com.TheChaYe.rotp_yellowtemperance.init.AddonStands;
import com.TheChaYe.rotp_yellowtemperance.init.InitStands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客户端初始化 / Client Initialization
 * 处理客户端特定的初始化任务，包括渲染层和事件订阅
 */
@EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInit {

    /** 侵蚀效果渲染层集合 / Erosion effect render layers collection */
    private static final Set<ErosionLayer<?, ?>> MOLD_LAYERS = new HashSet<>();

    /**
     * FML客户端设置事件 / FML client setup event
     * 注册实体渲染器和添加渲染层
     */
    @SubscribeEvent
    public static void onFMLClientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                AddonStands.YELLOW_TEMPERANCE.getEntityType(),
                YellowTemperanceRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                InitStands.STAND_YELLOW_TEMPERANCE.getEntityType(),
                YellowTemperanceRenderer::new
        );

        // 伪装渲染通过Layer处理，不需要额外的渲染处理器 / Disguise rendering handled through Layer, no additional render handlers needed

        event.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Map<String, PlayerRenderer> skinMap = mc.getEntityRenderDispatcher().getSkinMap();

            addErosionLayers(skinMap.get("default"), false);
            addErosionLayers(skinMap.get("slim"), true);

            addDisguiseLayers(skinMap.get("default"), false);
            addDisguiseLayers(skinMap.get("slim"), true);

            mc.getEntityRenderDispatcher().renderers.values().forEach(ClientInit::addLayersToEntities);
            
            // AppleSkin饱和度纹理替换通过YTSaturationOverlayHandler自动处理 / AppleSkin saturation texture replacement automatically handled by YTSaturationOverlayHandler
        });
    }

    /**
     * 添加侵蚀效果渲染层 / Add erosion effect render layers
     * 为玩家渲染器添加侵蚀效果层
     */
    private static void addErosionLayers(PlayerRenderer renderer, boolean slim) {
        ErosionLayer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> erosionLayer =
                new ErosionLayer<>(renderer, ErosionLayer.BIPED_PATH);
        renderer.addLayer(erosionLayer);
        MOLD_LAYERS.add(erosionLayer);
    }

    /**
     * 添加伪装渲染层 / Add disguise render layers
     * 为玩家渲染器添加伪装效果层
     */
    private static void addDisguiseLayers(PlayerRenderer renderer, boolean slim) {
        renderer.addLayer(new DisguiseLayer<>(renderer));
    }

    /**
     * 为实体添加渲染层 / Add render layers to entities
     * 为所有实体渲染器添加侵蚀和伪装效果层
     */
    private static <T extends LivingEntity, M extends BipedModel<T>> void addLayersToEntities(EntityRenderer<?> renderer) {
        if (renderer instanceof LivingRenderer<?, ?>) {
            LivingRenderer<T, M> livingRenderer = (LivingRenderer<T, M>) renderer;

            ErosionLayer<T, M> erosionLayer;
            if (livingRenderer.getModel() instanceof BipedModel<?>) {
                erosionLayer = new ErosionLayer<>(livingRenderer, ErosionLayer.BIPED_PATH);
                livingRenderer.addLayer(erosionLayer);
                MOLD_LAYERS.add(erosionLayer);
            } else {
                erosionLayer = new ErosionLayer<>(livingRenderer, ErosionLayer.NON_BIPED_PATH);
                livingRenderer.addLayer(erosionLayer);
                MOLD_LAYERS.add(erosionLayer);
            }

            if (renderer instanceof PlayerRenderer) {
                livingRenderer.addLayer(new DisguiseLayer<>(livingRenderer));
            }
        }
    }

    /**
     * 切换侵蚀效果可见性 / Toggle erosion effect visibility
     * 控制所有侵蚀效果层的显示状态
     */
    public static void toggleErosionVisibility(boolean visible) {
        for (ErosionLayer<?, ?> layer : MOLD_LAYERS) {
            layer.setVisible(visible);
        }
    }

    /**
     * 粒子工厂注册事件 / Particle factory register event
     * 粒子系统的初始化处理
     */
    @SubscribeEvent
    public static void onMcConstructor(ParticleFactoryRegisterEvent event) {
    }
}