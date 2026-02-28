package com.TheChaYe.rotp_yellowtemperance.client.ui;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.action.YellowTemperanceDisguise;
import com.TheChaYe.rotp_yellowtemperance.entity.YellowTemperanceEntity;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.YellowTemperanceRenderPacket;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 表单选择界面 / Form Choice UI
 * 提供实体类型选择界面，用于黄色节制的伪装功能
 */
@OnlyIn(Dist.CLIENT)
public class FormChoiceUI extends Screen {
    /** 表单UI背景纹理路径 / Form UI background texture path */
    public static final ResourceLocation FMCUI_BG_LOCATION = new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/gui/fmcui_bg.png");
    /** 按钮宽度 / Button width */
    private final int BUTTON_WIDTH = 24;
    /** 按钮高度 / Button height */
    private final int BUTTON_HEIGHT = 24;
    /** 当前悬停的按钮 / Currently hovered button */
    private Optional<FormButton> HOVERED_BUTTON = Optional.empty();

    /**
     * 构造函数 / Constructor
     * 创建表单选择界面实例
     */
    public FormChoiceUI(Minecraft mc) {
        super(NarratorChatListener.NO_TITLE);
    }

    /**
     * 打开界面 / Open UI
     * 静态方法用于打开表单选择界面
     */
    public static void openUI(Minecraft mc) {
        if (mc != null && mc.screen == null) {
            mc.setScreen(new FormChoiceUI(mc));
        }
    }

    /**
     * 获取悬停按钮 / Get hovered button
     * 返回当前鼠标悬停的按钮
     */
    public Optional<FormButton> getHoveredButton() {
        return HOVERED_BUTTON;
    }

    /**
     * 设置悬停按钮 / Set hovered button
     * 设置当前鼠标悬停的按钮
     */
    public void setHoveredButton(FormButton btn) {
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
     * 创建并排列实体类型选择按钮
     */
    @Override
    protected void init() {
        super.init();
        final Minecraft mc = Minecraft.getInstance();
        final LinkedList<EntityType<?>> tempEntityList = RotPYellowTemperanceAddon.getEntitiesForDisguise();

        AtomicInteger i = new AtomicInteger(60);
        AtomicInteger j = new AtomicInteger(48);
        AtomicInteger btn_count = new AtomicInteger(0);

        tempEntityList.forEach(entityType -> {
            i.set(i.get() + BUTTON_WIDTH + 1);
            if (i.get() >= BUTTON_WIDTH * 15 + 1) {
                i.set(60);
                j.set(j.get() + BUTTON_HEIGHT + 1);
            }

            FormButton btn = new FormButton(
                    entityType, this, i.get(), j.get(), BUTTON_WIDTH, BUTTON_HEIGHT, button -> {
                PlayerEntity clientPlayer = ClientUtil.getClientPlayer();
                IStandPower.getStandPowerOptional(clientPlayer).ifPresent(stand_power -> {
                    if (stand_power.getStandManifestation() instanceof YellowTemperanceEntity) {
                        YellowTemperanceEntity yt = (YellowTemperanceEntity) stand_power.getStandManifestation();


                        // 只设置玩家伪装 / Only set player disguise
                        yt.setUserDisguised(true);
                        yt.setUserDisguiseEntity(entityType);

                        // 清除替身伪装 / Clear stand disguise
                        yt.setDisguisedOnce(false);
                        yt.setEntityForDisguise(null);
                        // 使用反射获取动画参数 / Use reflection to get animation parameters
                        float limbSwing = 0;
                        float limbSwingAmount = 0;
                        try {
                            Field limbSwingField = LivingEntity.class.getDeclaredField("limbSwing");
                            Field limbSwingAmountField = LivingEntity.class.getDeclaredField("limbSwingAmount");

                            limbSwingField.setAccessible(true);
                            limbSwingAmountField.setAccessible(true);

                            limbSwing = limbSwingField.getFloat(clientPlayer);
                            limbSwingAmount = limbSwingAmountField.getFloat(clientPlayer);
                        } catch (Exception e) {
                            // 使用默认值 / Use default values
                        }
                        // 发送玩家伪装同步包 - 使用实际动画参数 / Send player disguise sync packet - using actual animation parameters
                        PacketHandler.CHANNEL.sendToServer(new YellowTemperanceRenderPacket(
                                entityType.getRegistryName(),
                                yt.getId(),
                                limbSwing,       // 使用反射获取的值 / Using reflected values
                                limbSwingAmount,  // 使用反射获取的值 / Using reflected values
                                clientPlayer.yRot,
                                clientPlayer.xRot
                        ));

                        // 调用伪装完成事件（先调用伪装设置再调用事件） / Call disguise completion event (set disguise first then call event)
                        YellowTemperanceDisguise.onPlayerDisguised(clientPlayer, yt);

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