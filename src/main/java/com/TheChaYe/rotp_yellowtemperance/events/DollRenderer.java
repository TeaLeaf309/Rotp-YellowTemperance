package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.capability.entity.CapabilityHandler;
import com.TheChaYe.rotp_yellowtemperance.capability.entity.LivingData;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 纸娃娃渲染器 / Doll Renderer
 * 在游戏界面中渲染玩家纸娃娃模型
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class DollRenderer {
    /**
     * 水平位置比例（屏幕最右边） / Horizontal position ratio (rightmost of screen)
     */
    private static final float X_POS = 0.5f;
    /**
     * 垂直位置比例（屏幕最底部） / Vertical position ratio (bottom of screen)
     */
    private static final float Y_POS = 0.9f;
    /**
     * 模型的当前缩放值 / Current model scale value
     */
    private static float scale = 0.0f;

    /**
     * 客户端tick事件处理器 / Client tick event handler
     * 更新纸娃娃的动画状态和渲染条件
     *
     * @param event 客户端tick事件对象 / Client tick event object
     */
    @SubscribeEvent(priority = EventPriority.LOW) // 设置较低优先级 / Set lower priority
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            // 只在tick结束时处理（避免每帧处理两次） / Only process at tick end (avoid processing twice per frame)
            if (event.phase == TickEvent.Phase.END) {
                Minecraft mc = Minecraft.getInstance();
                ClientPlayerEntity player = mc.player;

                // 如果玩家不存在或处于旁观模式，跳过更新 / Skip update if player doesn't exist or is in spectator mode
                if (player == null || player.isSpectator()) return;

                // 检查玩家是否拥有黄色节制替身 / Check if player has Yellow Temperance stand
                boolean hasYellowTemperance = false;
                IStandPower standPower = IStandPower.getStandPowerOptional(player).orElse(null);
                if (standPower != null && standPower.getStandManifestation() instanceof AbstractDisguiseStandEntity) {
                    hasYellowTemperance = true;
                }

                // 如果玩家没有黄色节制替身，直接返回 / Return directly if player doesn't have Yellow Temperance
                if (!hasYellowTemperance) {
                    return;
                }

                // 检查是否启用纸娃娃渲染（使用Capability） / Check if doll rendering is enabled (using Capability)
                boolean isDollRendering = false;
                if (CapabilityHandler.LIVING_DATA_CAPABILITY != null) {
                    LivingData cap = player.getCapability(CapabilityHandler.LIVING_DATA_CAPABILITY).orElse(null);
                    if (cap != null) {
                        isDollRendering = cap.isDollRendering();
                    }
                }
                scale = 10.0f;
                // 如果没有启用纸娃娃渲染，将scale设为0而不是直接返回 / Set scale to 0 instead of returning directly if doll rendering is not enabled
                if (!isDollRendering) {
                    scale = 0.0f;

                }

                // 确保缩放值始终为正 / Ensure scale value is always positive
                scale = Math.max(0, scale);
            }
        } catch (Throwable t) {
            // 静默处理异常 / Silently handle exceptions
        }
    }

    /**
     * 游戏界面渲染后事件处理器 / Game overlay post-render event handler
     * 在合适时机渲染纸娃娃模型
     *
     * @param event 游戏界面渲染事件对象 / Game overlay render event object
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        // 只在渲染所有HUD元素完成后渲染纸娃娃 / Only render doll after all HUD elements are rendered
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        try {

            // 获取MC实例和玩家 / Get MC instance and player
            Minecraft mc = Minecraft.getInstance();
            ClientPlayerEntity player = mc.player;

            // 确保玩家存在 / Ensure player exists
            if (player == null || player.isSpectator()) return;

            // 检查玩家是否拥有黄色节制替身 / Check if player has Yellow Temperance stand
            boolean hasYellowTemperance = false;
            IStandPower standPower = IStandPower.getStandPowerOptional(player).orElse(null);
            if (standPower != null && standPower.getStandManifestation() instanceof AbstractDisguiseStandEntity) {
                hasYellowTemperance = true;
            }

            // 如果玩家没有黄色节制替身，不进行渲染 / Don't render if player doesn't have Yellow Temperance
            if (!hasYellowTemperance) {
                return;
            }

            // 检查是否启用纸娃娃渲染（使用Capability） / Check if doll rendering is enabled (using Capability)
            boolean isDollRendering = false;
            if (CapabilityHandler.LIVING_DATA_CAPABILITY != null) {
                LivingData cap = player.getCapability(CapabilityHandler.LIVING_DATA_CAPABILITY).orElse(null);
                if (cap != null) {
                    isDollRendering = cap.isDollRendering();
                }
            }
            scale = 10.0f;
            // 只有在启用纸娃娃渲染且是第一人称视角时才渲染 / Only render when doll rendering is enabled and in first person view
            if (!isDollRendering || !mc.options.getCameraType().isFirstPerson()) scale = 0.0f;

            // 渲染纸娃娃 / Render doll
            renderPlayerDoll(mc, player);
        } catch (Throwable t) {
            // 静默处理渲染异常 / Silently handle rendering exceptions
        }
    }

    /**
     * 实际渲染纸娃娃模型的方法 / Actual method to render player doll
     *
     * @param mc     Minecraft实例 / Minecraft instance
     * @param player 客户端玩家实体 / Client player entity
     */
    private static void renderPlayerDoll(Minecraft mc, ClientPlayerEntity player) {
        if (mc == null || player == null) return;

        try {
            // 创建变换矩阵堆栈（用于位置、旋转、缩放） / Create transformation matrix stack (for position, rotation, scaling)
            MatrixStack matrixStack = new MatrixStack();

            // 获取渲染缓冲区（用于批量提交渲染数据） / Get render buffer (for batch submission of render data)
            IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();

            // 计算位置（基于屏幕尺寸和预设比例） / Calculate position (based on screen size and preset ratios)
            int width = mc.getWindow().getGuiScaledWidth();    // 缩放后的屏幕宽度 / Scaled screen width
            int height = mc.getWindow().getGuiScaledHeight();   // 缩放后的屏幕高度 / Scaled screen height

            // 调整位置到物品栏右侧，与物品栏同高 / Adjust position to right of inventory, same height as inventory
            // 物品栏通常占据屏幕底部 23 像素左右的高度，我们把模型放在那里 / Inventory typically occupies about 23 pixels at bottom of screen, we place model there
            matrixStack.translate(width * X_POS, height * Y_POS, 0); // 放在右下角适当位置 / Place at appropriate position in bottom right corner

            // 应用缩放和旋转 / Apply scaling and rotation
            float scaledSize = scale;
            matrixStack.scale(scaledSize, scaledSize, scaledSize); // 应用缩放 / Apply scaling
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(180)); // 绕Z轴旋转180°（纠正朝向） / Rotate 180° around Z-axis (correct orientation)

            // 使纸娃娃始终面向玩家 / Make doll always face player
            // 纸娃娃需要与玩家朝向相反的旋转，这样它才能始终面对玩家 / Doll needs opposite rotation to player so it always faces player
            // 添加180度偏移以确保纸娃娃始终正对玩家 / Add 180 degree offset to ensure doll always faces player directly
            float playerRotation = player.yRot;
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(playerRotation));

            // 获取实体渲染管理器 / Get entity render manager
            EntityRendererManager renderManager = mc.getEntityRenderDispatcher();
            renderManager.setRenderShadow(false); // 禁用阴影（避免渲染问题） / Disable shadows (avoid rendering issues)

            // 设置光照（确保模型正常显示） / Set lighting (ensure model displays properly)
            int lightLevel = LightTexture.pack(15, 15); // 最大光照值 / Maximum light level

            // 渲染玩家模型的核心调用（使用正常渲染器） / Core call to render player model (using normal renderer)
            renderManager.render(
                    player,                 // 要渲染的实体 / Entity to render
                    0, 0, 0,                // 位置偏移（不使用） / Position offset (not used)
                    mc.getFrameTime(),       // 部分渲染效果（如动画）的插值因子 / Interpolation factor for partial rendering effects (like animations)
                    0.0f,                   // 额外旋转（不使用） / Additional rotation (not used)
                    matrixStack,            // 变换矩阵堆栈 / Transformation matrix stack
                    buffer, // 使用正常的渲染缓冲区（非透明） / Use normal render buffer (non-transparent)
                    lightLevel              // 光照值 / Light level
            );

            // 清理状态，恢复到默认渲染设置 / Clean up state, restore to default render settings
            buffer.endBatch(); // 提交批次并清除缓冲 / Submit batch and clear buffer
            renderManager.setRenderShadow(true); // 重新启用阴影渲染 / Re-enable shadow rendering
        } catch (Throwable t) {
            // 静默处理渲染异常 / Silently handle rendering exceptions
        }
    }
}