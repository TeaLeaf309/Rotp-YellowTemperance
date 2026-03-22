package com.TheChaYe.rotp_yellowtemperance.client.render.stand;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.KhnumEntity;
import com.github.standobyte.jojo.client.render.entity.model.stand.StandEntityModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.StandModelRegistry;
import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class KhnumRenderer extends StandEntityRenderer<KhnumEntity, StandEntityModel<KhnumEntity>> {

    public KhnumRenderer(EntityRendererManager renderManager) {
        super(renderManager,
                StandModelRegistry.registerModel(new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "khnum"), KhnumModel::new),
                new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/entity/stand/khnum.png"), 0);
    }
}
