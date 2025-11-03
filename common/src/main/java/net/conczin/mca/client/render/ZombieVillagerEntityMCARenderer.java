package net.conczin.mca.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.mca.Config;
import net.conczin.mca.MCA;
import net.conczin.mca.client.model.VillagerEntityModelMCA;
import net.conczin.mca.client.model.ZombieVillagerEntityModelMCA;
import net.conczin.mca.client.render.layer.ClothingLayer;
import net.conczin.mca.client.render.layer.FaceLayer;
import net.conczin.mca.client.render.layer.HairLayer;
import net.conczin.mca.client.render.layer.SkinLayer;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ZombieVillagerEntityMCA;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class ZombieVillagerEntityMCARenderer extends VillagerLikeEntityMCARenderer<ZombieVillagerEntityMCA> {
    private final SkinLayer<ZombieVillagerEntityMCA, VillagerEntityModelMCA<ZombieVillagerEntityMCA>> skinLayer;
    private final ClothingLayer<ZombieVillagerEntityMCA, VillagerEntityModelMCA<ZombieVillagerEntityMCA>> clothingLayer;

    private final VillagerEntityModelMCA<ZombieVillagerEntityMCA> skinModel;
    private final VillagerEntityModelMCA<ZombieVillagerEntityMCA> slimSkinModel;
    private final VillagerEntityModelMCA<ZombieVillagerEntityMCA> clothingModel;
    private final VillagerEntityModelMCA<ZombieVillagerEntityMCA> slimClothingModel;

    public ZombieVillagerEntityMCARenderer(EntityRendererProvider.Context ctx) {
        super(ctx, createModel(VillagerEntityModelMCA.bodyData(CubeDeformation.NONE)).hideWears());

        skinModel = model;
        slimSkinModel = createModel(VillagerEntityModelMCA.bodyData(CubeDeformation.NONE, true)).hideWears();
        clothingModel = createModel(VillagerEntityModelMCA.bodyData(CubeDeformation.NONE, false)).hideWears();
        slimClothingModel = createModel(VillagerEntityModelMCA.bodyData(CubeDeformation.NONE, true)).hideWears();
        skinLayer = new SkinLayer<>(this, skinModel);
        addLayer(skinLayer);
        addLayer(new FaceLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.01F))).hideWears(), "zombie"));
        clothingLayer = new ClothingLayer<>(this, createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.0625F))), "zombie");
        addLayer(clothingLayer);
        addLayer(new HairLayer<>(this, createModel(VillagerEntityModelMCA.hairData(new CubeDeformation(0.125F)))));
    }

    private static VillagerEntityModelMCA<ZombieVillagerEntityMCA> createModel(MeshDefinition data) {
        return new ZombieVillagerEntityModelMCA<>(LayerDefinition.create(data, 64, 64).bakeRoot());
    }

    @Override
    protected boolean isShaking(ZombieVillagerEntityMCA entity) {
        return entity.isConverting() || entity.isUnderWaterConverting();
    }

    @Override
    public void render(@NotNull ZombieVillagerEntityMCA entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
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
