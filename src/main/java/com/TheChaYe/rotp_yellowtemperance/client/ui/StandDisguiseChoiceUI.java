// 原代码来自：https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// 修改日期：2026-03-19
// Original code from: https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// on 2026-03-19
package com.TheChaYe.rotp_yellowtemperance.client.ui;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.YellowTemperanceEntity;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.StandDisguiseSetPacket;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 替身伪装选择界面 / Stand Disguise Choice UI
 * 提供替身实体类型选择界面，用于黄色节制替身伪装成其他替身
 */
@OnlyIn(Dist.CLIENT)
public class StandDisguiseChoiceUI extends Screen {
    /**
     * 表单 UI 背景纹理路径 / Form UI background texture path
     */
    public static final ResourceLocation FMCUI_BG_LOCATION = new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/gui/fmcui_bg.png");
    /**
     * 按钮宽度 / Button width
     */
    private final int BUTTON_WIDTH = 24;
    /**
     * 按钮高度 / Button height
     */
    private final int BUTTON_HEIGHT = 24;
    /**
     * 当前悬停的按钮 / Currently hovered button
     */
    private Optional<StandDisguiseButton> HOVERED_BUTTON = Optional.empty();

    /**
     * 构造函数 / Constructor
     * 创建替身伪装选择界面实例
     */
    public StandDisguiseChoiceUI(Minecraft mc) {
        super(NarratorChatListener.NO_TITLE);
    }

    /**
     * 打开界面 / Open UI
     * 静态方法用于打开替身伪装选择界面
     */
    public static void openUI(Minecraft mc) {
        if (mc != null && mc.screen == null) {
            mc.setScreen(new StandDisguiseChoiceUI(mc));
        }
    }

    /**
     * 获取悬停按钮 / Get hovered button
     * 返回当前鼠标悬停的按钮
     */
    public Optional<StandDisguiseButton> getHoveredButton() {
        return HOVERED_BUTTON;
    }

    /**
     * 设置悬停按钮 / Set hovered button
     * 设置当前鼠标悬停的按钮
     */
    public void setHoveredButton(StandDisguiseButton btn) {
        HOVERED_BUTTON = Optional.ofNullable(btn);
    }

    /**
     * 是否暂停游戏 / Is pause screen
     * 控制界面打开时游戏是否暂停
     */
    @Override
    public boolean isPauseScreen() {
        return true;
    }

    /**
     * 初始化界面 / Initialize UI
     * 创建并排列替身实体类型选择按钮
     */
    @Override
    protected void init() {
        super.init();
        final Minecraft mc = Minecraft.getInstance();
        // 获取可用于伪装的替身实体列表
        final CopyOnWriteArrayList<EntityType<?>> tempEntityList = RotPYellowTemperanceAddon.getEntitiesForStandDisguise();

        AtomicInteger i = new AtomicInteger(60);
        AtomicInteger j = new AtomicInteger(48);

        tempEntityList.forEach(entityType -> {
            i.set(i.get() + BUTTON_WIDTH + 1);
            if (i.get() >= BUTTON_WIDTH * 15 + 1) {
                i.set(60);
                j.set(j.get() + BUTTON_HEIGHT + 1);
            }

            StandDisguiseButton btn = new StandDisguiseButton(
                    entityType, this, i.get(), j.get(), BUTTON_WIDTH, BUTTON_HEIGHT, button -> {
                PlayerEntity clientPlayer = ClientUtil.getClientPlayer();
                IStandPower.getStandPowerOptional(clientPlayer).ifPresent(stand_power -> {
                    if (stand_power.getStandManifestation() instanceof AbstractDisguiseStandEntity) {
                        YellowTemperanceEntity yt = (YellowTemperanceEntity) stand_power.getStandManifestation();
                        // 获取实体资源位置 / Get entity resource location
                        ResourceLocation disguiseRes = ForgeRegistries.ENTITIES.getKey(entityType);

                        // 发送网络包到服务端 / Send packet to server
                        PacketHandler.sendToServer(new StandDisguiseSetPacket(disguiseRes, yt.getId()));
                    }
                });
                mc.setScreen(null);
            });
            this.addButton(btn);
        });
    }

    /**
     * 渲染界面 / Render UI
     * 绘制界面背景和悬停按钮的文字提示
     */
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        FontRenderer font = minecraft.font;
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        getHoveredButton().ifPresent(btn -> {
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            TextFormatting formatting = TextFormatting.BOLD;
            ITextComponent msg = btn.getMessage();
            int textPosX = btn.x + btn.getWidth() / 2;
            int textPosY = btn.y + btn.getHeight() / 3;
            drawCenteredString(matrixStack, font, msg.copy().withStyle(formatting), textPosX, textPosY, 16777215);
        });
    }
}
