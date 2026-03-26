package com.TheChaYe.rotp_yellowtemperance.registry;

import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 伪装菜单系统 / Disguise Menu System
 * 提供伪装选择的容器和界面
 */
public class DisguiseMenuSystem {

    /**
     * 伪装菜单提供者 / Disguise menu provider
     * 为伪装选择界面提供容器
     */
    public static class DisguiseMenuProvider implements INamedContainerProvider {
        @Override
        public ITextComponent getDisplayName() {
            return new StringTextComponent("选择变形");
        }

        @Override
        public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
            return new DisguiseContainer(id, inv);
        }
    }

    /**
     * 伪装容器 / Disguise container
     * 处理伪装选择界面的物品槽位逻辑
     */
    public static class DisguiseContainer extends Container {
        /**
         * 构造函数 / Constructor
         *
         * @param id        容器 ID / Container ID
         * @param playerInv 玩家物品栏 / Player inventory
         */
        public DisguiseContainer(int id, PlayerInventory playerInv) {
            super(ContainerType.GENERIC_9x3, id);

            // 添加生物选择按钮 / Add entity selection buttons
            IItemHandler itemHandler = new ItemStackHandler(EntityDisguiseRegistry.getEntitiesForDisguise().size()) {
                @Override
                public ItemStack getStackInSlot(int slot) {
                    EntityType<?> entityType = EntityDisguiseRegistry.getEntitiesForDisguise().get(slot);
                    EntitySize size = EntityDisguiseRegistry.getEntitySizeCache().get(entityType);

                    // 在物品名称中显示尺寸信息 / Display size information in item name
                    String displayName = String.format("%s (%.1f×%.1f)",
                            entityType.getDescription().getString(),
                            size.width,
                            size.height);

                    return new ItemStack(Items.PAPER).setHoverName(
                            new StringTextComponent(displayName)
                    );
                }
            };

            for (int i = 0; i < EntityDisguiseRegistry.getEntitiesForDisguise().size(); i++) {
                addSlot(new SlotItemHandler(itemHandler, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18) {
                    @Override
                    public boolean mayPickup(PlayerEntity player) {
                        return true;
                    }
                });
            }

            // 玩家物品栏 / Player inventory
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 9; ++j) {
                    this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
                }
            }

            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInv, k, 8 + k * 18, 142));
            }
        }

        /**
         * 检查容器是否仍然有效 / Check if container is still valid
         *
         * @param player 玩家实体 / Player entity
         * @return 是否有效 / Whether valid
         */
        @Override
        public boolean stillValid(PlayerEntity player) {
            return true;
        }
    }
}
