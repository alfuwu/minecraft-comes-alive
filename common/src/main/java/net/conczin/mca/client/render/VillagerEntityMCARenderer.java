package net.conczin.mca.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.mca.Config;
import net.conczin.mca.client.model.CommonVillagerModel;
import net.conczin.mca.client.model.VillagerEntityModelMCA;
import net.conczin.mca.client.render.layer.ClothingLayer;
import net.conczin.mca.client.render.layer.FaceLayer;
import net.conczin.mca.client.render.layer.HairLayer;
import net.conczin.mca.client.render.layer.SkinLayer;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class VillagerEntityMCARenderer extends VillagerLikeEntityMCARenderer<VillagerEntityMCA> {
    private final SkinLayer<VillagerEntityMCA, VillagerEntityModelMCA<VillagerEntityMCA>> skinLayer;
    private final ClothingLayer<VillagerEntityMCA, VillagerEntityModelMCA<VillagerEntityMCA>> clothingLayer;

    private final VillagerEntityModelMCA<VillagerEntityMCA> skinModel;
    private final VillagerEntityModelMCA<VillagerEntityMCA> slimSkinModel;
    private final VillagerEntityModelMCA<VillagerEntityMCA> clothingModel;
    private final VillagerEntityModelMCA<VillagerEntityMCA> slimClothingModel;

    public VillagerEntityMCARenderer(EntityRendererProvider.Context ctx) {
        super(ctx, createModel(VillagerEntityModelMCA.bodyData(CubeDeformation.NONE)).hideWears());

        skinModel = model;
        slimSkinModel = createModel(VillagerEntityModelMCA.bodyData(CubeDeformation.NONE, true)).hideWears();
        clothingModel = createModel(VillagerEntityModelMCA.bodyData(CubeDeformation.NONE, false)).hideWears();
        slimClothingModel = createModel(VillagerEntityModelMCA.bodyData(CubeDeformation.NONE, true)).hideWears();
        skinLayer = new SkinLayer<>(this, skinModel);
        addLayer(skinLayer);
        addLayer(new FaceLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.01F))).hideWears(), "normal"));
        clothingLayer = new ClothingLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.0625F))), "normal");
        addLayer(clothingLayer);
        addLayer(new HairLayer<>(this, createModel(VillagerEntityModelMCA.hairData(new CubeDeformation(0.125F)))));
    }

    private static VillagerEntityModelMCA<VillagerEntityMCA> createModel(MeshDefinition data) {
        return new VillagerEntityModelMCA<>(LayerDefinition.create(data, 64, 64).bakeRoot());
    }

    @Override
    public void render(@NotNull VillagerEntityMCA entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        boolean slim = entity.getGenetics().getGender() == Gender.FEMALE && Config.getInstance().femalesUseSlimArms;
        if (slim) {
            skinLayer.model = slimSkinModel;
            clothingLayer.model = slimClothingModel;
            model = slimSkinModel;
        } else {
            skinLayer.model = skinModel;
            clothingLayer.model = clothingModel;
            model = skinModel;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
