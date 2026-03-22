// 原代码来自：https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// 修改日期：2026-03-19
// Original code from: https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// on 2026-03-19
package com.TheChaYe.rotp_yellowtemperance.client.ui;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.github.standobyte.jojo.client.ui.BlitFloat;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * 替身伪装按钮 / Stand Disguise Button
 * 自定义按钮类，用于显示替身实体的伪装选项
 */
public class StandDisguiseButton extends Button {
    /**
     * 表单 UI 背景纹理路径 / Form UI background texture path
     */
    public static final ResourceLocation FMCUI_BG_LOCATION = new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/gui/fmcui_bg.png");
    /**
     * 关联的实体类型 / Associated entity type
     */
    public final EntityType<?> entityType;
    /**
     * 主界面引用 / Main screen reference
     */
    public final StandDisguiseChoiceUI mainScreen;

    /**
     * 构造函数 / Constructor
     *
     * @param entityType 替身实体类型 / Stand entity type
     * @param screen     主界面 / Main screen
     * @param x          X 坐标 / X coordinate
     * @param y          Y 坐标 / Y coordinate
     * @param w          宽度 / Width
     * @param h          高度 / Height
     * @param onPress    点击回调 / Click callback
     */
    public StandDisguiseButton(EntityType<?> entityType, StandDisguiseChoiceUI screen, int x, int y, int w, int h, IPressable onPress) {
        super(x, y, w, h, new TranslationTextComponent("rotp_yellowtemperance.stand_disguise", entityType.getDescription().getString()), onPress);
        this.entityType = entityType;
        this.mainScreen = screen;
    }

    /**
     * 渲染按钮 / Render button
     * 绘制按钮背景、图标和悬停效果
     */
    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        FontRenderer fontrenderer = minecraft.font;
        minecraft.getTextureManager().bind(FMCUI_BG_LOCATION);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
        int i = this.isHovered() ? 1 : 0;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        int j = this.getFGColor();
        blit(matrixStack, this.x, this.y, 0, 0, this.width, this.height, 128, 128);

        // 图标渲染 / Icon render
        ResourceLocation entity_icon = EntityTypeIcon.getIcon(entityType);
        if (entity_icon != EntityTypeIcon.UNKNOWN) {
            Minecraft.getInstance().getTextureManager().bind(entity_icon);
            BlitFloat.blitFloat(matrixStack, x + 4, y + 4, 0.0F, 0.0F, 16f, 16f, 16.0F, 16.0F);
        }

        if (this.isHovered()) {
            // 渲染按钮轮廓 / Render outline of the button
            minecraft.getTextureManager().bind(FMCUI_BG_LOCATION);
            blit(matrixStack, this.x, this.y, 24, 0, this.width, this.height, 128, 128);
            if (!mainScreen.getHoveredButton().isPresent())
                mainScreen.setHoveredButton(this);
        } else {
            mainScreen.getHoveredButton().ifPresent(btn -> {
                if (btn.equals(this))
                    mainScreen.setHoveredButton(null);
            });
        }
    }
}
