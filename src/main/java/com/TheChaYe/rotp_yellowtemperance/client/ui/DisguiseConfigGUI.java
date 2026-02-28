package com.TheChaYe.rotp_yellowtemperance.client.ui;

import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.ApplyDisguisePacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.RemoveDisguisePacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.SyncSearchHelperPacket;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.*;

/**
 * 伪装配置界面 / Disguise Configuration GUI
 * 提供玩家伪装功能的图形用户界面，支持玩家名自动补全和搜索辅助
 */
public class DisguiseConfigGUI extends Screen {
    /** 搜索辅助功能开启图标 / Search helper enabled icon */
    private static final ResourceLocation SEARCH_HELPER_ON = new ResourceLocation("rotp_yellowtemperance:textures/gui/search_helper_on.png");
    /** 搜索辅助功能关闭图标 / Search helper disabled icon */
    private static final ResourceLocation SEARCH_HELPER_OFF = new ResourceLocation("rotp_yellowtemperance:textures/gui/search_helper_off.png");

    /** 当前玩家实例 / Current player instance */
    private final PlayerEntity player;
    /** 玩家名输入框 / Player name input field */
    private TextFieldWidget nameField;
    /** 搜索辅助功能按钮 / Search helper toggle button */
    private Button searchHelperButton;
    /** 玩家名建议列表 / Player name suggestions list */
    private final List<String> playerSuggestions = new ArrayList<>();
    /** 当前选中的建议索引 / Current selected suggestion index */
    private int suggestionIndex = -1;
    /** 上次搜索文本 / Last search text */
    private String lastSearchText = "";

    /**
     * 构造函数 / Constructor
     * 初始化伪装配置界面
     */
    public DisguiseConfigGUI(PlayerEntity player) {
        super(new TranslationTextComponent("gui.yellowtemperance.disguise_config"));
        this.player = player;
    }

    /**
     * 初始化界面组件 / Initialize GUI components
     * 创建文本框、按钮和其他界面元素
     */
    @Override
    protected void init() {
        Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(true);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.nameField = new TextFieldWidget(
                this.font, centerX - 100, centerY - 20, 175, 20,
                new StringTextComponent("Disguise Name")
        ) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 258) { // TAB key
                    if (!playerSuggestions.isEmpty()) {
                        if (suggestionIndex == -1) {
                            // 第一次按下TAB，选择第一个建议 / First TAB press, select first suggestion
                            suggestionIndex = 0;
                        } else {
                            // 循环选择下一个建议 / Cycle to next suggestion
                            suggestionIndex = (suggestionIndex + 1) % playerSuggestions.size();
                        }
                        nameField.setValue(playerSuggestions.get(suggestionIndex));
                        return true;
                    }
                } else {
                    // 按下其他键时重置建议索引 / Reset suggestion index when other keys pressed
                    suggestionIndex = -1;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                boolean result = super.charTyped(codePoint, modifiers);
                if (result) {
                    updateSuggestions();
                }
                return result;
            }
        };
        this.addWidget(nameField);
        this.setInitialFocus(nameField);

        // 添加搜索辅助按钮，设置为20x20像素大小 / Add search helper button, set to 20x20 pixels
        this.searchHelperButton = new Button(
                centerX + 80 - 2, centerY - 20, 20, 20,
                new StringTextComponent(""),
                button -> toggleSearchHelper()
        ) {
            @Override
            public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY) {
                DisguiseConfigGUI.this.renderTooltip(matrixStack,
                        new TranslationTextComponent("gui.yellowtemperance.disguise_config.search_helper_tooltip"),
                        mouseX, mouseY);
            }
        };
        this.addButton(searchHelperButton);

        this.addButton(new Button(
                centerX - 100, centerY + 10, 90, 20,
                new TranslationTextComponent("gui.yellowtemperance.disguise_config.apply"),
                button -> applyDisguise()
        ));

        this.addButton(new Button(
                centerX + 10, centerY + 10, 90, 20,
                new TranslationTextComponent("gui.yellowtemperance.disguise_config.remove"),
                button -> removeDisguise()
        ));

        updateSuggestions(); // 初始化建议列表 / Initialize suggestions list
    }

    /**
     * 更新玩家名建议列表 / Update player name suggestions
     * 根据当前输入文本生成匹配的玩家名建议
     */
    private void updateSuggestions() {
        String currentText = nameField.getValue();
        // 只有在文本发生变化时才更新建议列表 / Only update suggestions when text changes
        if (!currentText.equals(lastSearchText)) {
            lastSearchText = currentText;
            playerSuggestions.clear();
            suggestionIndex = -1;

            if (Minecraft.getInstance().getConnection() != null) {
                Collection<NetworkPlayerInfo> playerInfoMap = Minecraft.getInstance().getConnection().getOnlinePlayers();
                for (NetworkPlayerInfo playerInfo : playerInfoMap) {
                    String playerName = playerInfo.getProfile().getName();
                    if (playerName.toLowerCase(Locale.ROOT).startsWith(currentText.toLowerCase(Locale.ROOT))) {
                        playerSuggestions.add(playerName);
                    }
                }

                // 按字母顺序排序 / Sort alphabetically
                Collections.sort(playerSuggestions);
            }
        }
    }

    /**
     * 处理键盘按键事件 / Handle keyboard key presses
     * 特别处理Tab键的自动补全功能
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 当文本框获得焦点并且按下Tab键时，处理Tab补全 / Handle Tab completion when text field is focused / Handle Tab completion when text field is focused
        if (keyCode == 258 && nameField.isFocused()) {
            if (!playerSuggestions.isEmpty()) {
                if (suggestionIndex == -1) {
                    // 第一次按下TAB，选择第一个建议 / First TAB press, select first suggestion
                    suggestionIndex = 0;
                } else {
                    // 循环选择下一个建议 / Cycle to next suggestion
                    suggestionIndex = (suggestionIndex + 1) % playerSuggestions.size();
                }
                nameField.setValue(playerSuggestions.get(suggestionIndex));
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 切换搜索辅助功能 / Toggle search helper
     * 开启或关闭玩家名自动搜索辅助功能
     */
    private void toggleSearchHelper() {
        // 通过Capability切换搜索辅助功能 / Toggle search helper through Capability / Toggle search helper through Capability
        boolean newValue = !isSearchHelperEnabled();

        // 更新客户端的Capability / Update client-side Capability / Update client-side Capability
        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
            cap.setSearchHelperEnabled(newValue);
        });

        // 同步状态到服务端 / Sync state to server / Sync state to server
        PacketHandler.CHANNEL.sendToServer(new SyncSearchHelperPacket(newValue));

        // 更新按钮状态 / Update button state / Update button state
        searchHelperButton.playDownSound(Minecraft.getInstance().getSoundManager());
    }
    
    /**
     * 处理搜索辅助状态更新 / Handle search helper state update
     * 当从服务端接收到搜索辅助状态更新时调用
     * @param enabled 新的状态 / New state
     */
    public void onSearchHelperStateUpdate(boolean enabled) {
        // 更新本地Capability / Update local Capability / Update local Capability
        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
            cap.setSearchHelperEnabled(enabled);
        });
        
        // 刷新界面显示 / Refresh GUI display / Refresh GUI display
        refreshSearchHelperButton();
    }

    /**
     * 应用伪装 / Apply disguise
     * 将输入的玩家名发送到服务端进行伪装
     */
    private void applyDisguise() {
        String name = nameField.getValue().trim();
        if (!name.isEmpty()) {
            PacketHandler.CHANNEL.sendToServer(new ApplyDisguisePacket(name));
        }
        this.onClose();
    }

    /**
     * 移除伪装 / Remove disguise
     * 发送移除伪装请求到服务端
     */
    private void removeDisguise() {
        PacketHandler.CHANNEL.sendToServer(new RemoveDisguisePacket());
        this.onClose();
    }

    /**
     * 渲染界面 / Render GUI
     * 绘制背景、文本框、按钮和玩家名建议列表
     */
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        nameField.render(matrixStack, mouseX, mouseY, partialTicks);

        // 渲染搜索辅助按钮的贴图 / Render search helper button texture / Render search helper button texture
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(isSearchHelperEnabled() ? SEARCH_HELPER_ON : SEARCH_HELPER_OFF);
        // 在20x20的按钮中央绘制20x20的贴图（完全填充） / Draw 20x20 texture centered in 20x20 button / Draw 20x20 texture centered in 20x20 button
        blit(matrixStack, searchHelperButton.x, searchHelperButton.y, 0, 0, 20, 20, 20, 20);

        // 渲染玩家名字建议列表（模仿原版命令建议样式） / Render player name suggestions (mimicking vanilla command suggestions) / Render player name suggestions (mimicking vanilla command suggestions)
        if (!playerSuggestions.isEmpty() && nameField.isFocused()) {
            int maxSuggestions = Math.min(10, playerSuggestions.size());
            int suggestionWidth = 200;
            int suggestionHeight = maxSuggestions * 12 + 4;

            // 绘制半透明背景 / Draw semi-transparent background / Draw semi-transparent background
            fill(matrixStack,
                    nameField.x, nameField.y + nameField.getHeight() + 1,
                    nameField.x + suggestionWidth, nameField.y + nameField.getHeight() + 1 + suggestionHeight,
                    0xA0000000);

            // 绘制边框 / Draw border / Draw border
            hLine(matrixStack, nameField.x, nameField.x + suggestionWidth - 1, nameField.y + nameField.getHeight() + 1, 0xFFA0A0A0);
            hLine(matrixStack, nameField.x, nameField.x + suggestionWidth - 1, nameField.y + nameField.getHeight() + 1 + suggestionHeight - 1, 0xFFA0A0A0);
            vLine(matrixStack, nameField.x, nameField.y + nameField.getHeight() + 1, nameField.y + nameField.getHeight() + 1 + suggestionHeight - 1, 0xFFA0A0A0);
            vLine(matrixStack, nameField.x + suggestionWidth - 1, nameField.y + nameField.getHeight() + 1, nameField.y + nameField.getHeight() + 1 + suggestionHeight - 1, 0xFFA0A0A0);

            for (int i = 0; i < maxSuggestions; i++) {
                String suggestion = playerSuggestions.get(i);
                int yPosition = nameField.y + nameField.getHeight() + 3 + (i * 12);

                // 当前选中的建议项用白色显示，其他用灰色显示 / Selected suggestion in white, others in gray
                int color = (suggestionIndex == i) ? 0xFFFFFF : 0xA0A0A0;
                drawString(matrixStack, this.font, suggestion, nameField.x + 2, yPosition, color);
            }
        }
    }

    /**
     * 检查搜索辅助功能是否启用 / Check if search helper is enabled
     * 通过Capability获取搜索辅助功能的当前状态
     */
    private boolean isSearchHelperEnabled() {
        // 通过Capability获取搜索辅助功能状态 / Get search helper status through Capability / Get search helper status through Capability
        return player.getCapability(InitCapabilities.DISGUISE_CAPABILITY)
                .map(cap -> cap.isSearchHelperEnabled())
                .orElse(true); // 默认开启 / Default enabled / Default enabled
    }
    
    /**
     * 刷新搜索辅助按钮 / Refresh search helper button
     * 刷新搜索辅助按钮的显示状态
     */
    private void refreshSearchHelperButton() {
        // 触发按钮的视觉更新 / Trigger button visual update
        searchHelperButton.active = true; // 确保按钮可用 / Ensure button is active
    }

    /**
     * 关闭界面 / Close GUI
     * 清理资源并恢复键盘输入状态
     */
    @Override
    public void onClose() {
        super.onClose();
        Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(false);
    }
}