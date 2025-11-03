package net.conczin.mca.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.mca.Config;
import net.conczin.mca.MCAClient;
import net.conczin.mca.client.model.CommonVillagerModel;
import net.conczin.mca.client.model.PlayerEntityExtendedModel;
import net.conczin.mca.client.model.VillagerEntityModelMCA;
import net.conczin.mca.client.render.layer.*;
import net.conczin.mca.entity.ai.relationship.AgeState;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class MixinPlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    @Unique
    SkinLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> mca$skinLayer;
    @Unique
    ClothingLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> mca$clothingLayer;
    @Unique
    private PlayerModel<AbstractClientPlayer> mca$villagerModel;
    @Unique
    private PlayerModel<AbstractClientPlayer> mca$skinModel;
    @Unique
    private PlayerModel<AbstractClientPlayer> mca$slimSkinModel;
    @Unique
    private PlayerModel<AbstractClientPlayer> mca$clothingModel;
    @Unique
    private PlayerModel<AbstractClientPlayer> mca$slimClothingModel;
    @Unique
    private PlayerModel<AbstractClientPlayer> mca$vanillaModel;

    public MixinPlayerRenderer(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Unique
    private static PlayerEntityExtendedModel<AbstractClientPlayer> mca$createModel(MeshDefinition data) {
        return new PlayerEntityExtendedModel<>(LayerDefinition.create(data, 64, 64).bakeRoot());
    }

    @Shadow
    protected abstract void setModelProperties(AbstractClientPlayer abstractClientPlayer);

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Z)V", at = @At("TAIL"))
    private void mca$injectInit(EntityRendererProvider.Context ctx, boolean playerSlim, CallbackInfo ci) {
        if (MCAClient.isPlayerRendererAllowed()) {
            mca$villagerModel = mca$createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.0F), playerSlim));
            mca$vanillaModel = model;

            mca$skinModel = mca$createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.0F), false));
            mca$slimSkinModel = mca$createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.0F), true));
            mca$skinLayer = new SkinLayer<>(this, mca$skinModel);
            addLayer(mca$skinLayer);
            addLayer(new FaceLayer<>(this, mca$createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.01F))), "normal"));

            mca$clothingModel = mca$createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.0625F), false));
            mca$slimClothingModel = mca$createModel(VillagerEntityModelMCA.bodyData(new CubeDeformation(0.0625F), true));
            mca$clothingLayer = new ClothingLayer<>(this, mca$clothingModel, "normal");
            addLayer(mca$clothingLayer);
            addLayer(new HairLayer<>(this, mca$createModel(VillagerEntityModelMCA.hairData(new CubeDeformation(0.125F)))));
        }
    }

    @Inject(method = "scale(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("TAIL"), cancellable = true)
    private void mca$injectScale(AbstractClientPlayer player, PoseStack matrices, float f, CallbackInfo ci) {
        if (MCAClient.useGeneticsRenderer(player.getUUID())) {
            float height = CommonVillagerModel.getVillager(player).getRawVerticalScaleFactor();
            float width = CommonVillagerModel.getVillager(player).getRawHorizontalScaleFactor();
            matrices.scale(width, height, width);
            if (CommonVillagerModel.getVillager(player).getAgeState() == AgeState.BABY && !player.isPassenger()) {
                matrices.translate(0, 0.6F, 0);
            }
            ci.cancel();

            boolean slim = CommonVillagerModel.getVillager(player).getGenetics().getGender() == Gender.FEMALE && Config.getInstance().femalesUseSlimArms;
            if (slim) {
                mca$skinLayer.model = mca$slimSkinModel;
                mca$clothingLayer.model = mca$slimClothingModel;
            } else {
                mca$skinLayer.model = mca$skinModel;
                mca$clothingLayer.model = mca$clothingModel;
            }

            // switch to mca model
            model = mca$villagerModel;
        } else if (MCAClient.isPlayerRendererAllowed()) {
            // switch to vanilla model
            model = mca$vanillaModel;
        }
    }

    @Inject(method = "renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V", at = @At("HEAD"), cancellable = true)
    public void mca$injectRenderRightArm(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        if (MCAClient.renderArms(player.getUUID(), "right_arm")) {
            boolean slim = CommonVillagerModel.getVillager(player).getGenetics().getGender() == Gender.FEMALE && Config.getInstance().femalesUseSlimArms;
            var skinModel = slim ? mca$slimSkinModel : mca$skinModel;
            var clothingModel = slim ? mca$slimClothingModel : mca$clothingModel;
            mca$renderCustomArm(matrices, vertexConsumers, light, player, skinModel.rightArm, skinModel.rightSleeve, mca$skinLayer);
            mca$renderCustomArm(matrices, vertexConsumers, light, player, clothingModel.rightArm, clothingModel.rightSleeve, mca$clothingLayer);
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V", at = @At("HEAD"), cancellable = true)
    public void mca$injectRenderLeftArm(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, CallbackInfo ci) {
        if (MCAClient.renderArms(player.getUUID(), "left_arm")) {
            boolean slim = CommonVillagerModel.getVillager(player).getGenetics().getGender() == Gender.FEMALE && Config.getInstance().femalesUseSlimArms;
            var skinModel = slim ? mca$slimSkinModel : mca$skinModel;
            var clothingModel = slim ? mca$slimClothingModel : mca$clothingModel;
            mca$renderCustomArm(matrices, vertexConsumers, light, player, skinModel.leftArm, skinModel.leftSleeve, mca$skinLayer);
            mca$renderCustomArm(matrices, vertexConsumers, light, player, clothingModel.leftArm, clothingModel.leftSleeve, mca$clothingLayer);
            ci.cancel();
        }
    }

    @Unique
    private void mca$renderCustomArm(PoseStack matrices, MultiBufferSource vertexConsumers, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, VillagerLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer) {
        PlayerEntityExtendedModel<AbstractClientPlayer> model = (PlayerEntityExtendedModel<AbstractClientPlayer>) layer.model;
        setModelProperties(player);

        model.attackTime = 0.0f;
        model.crouching = false;
        model.swimAmount = 0.0f;
        model.setupAnim(player, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

        model.applyVillagerDimensions(CommonVillagerModel.getVillager(player), player.isCrouching());

        ResourceLocation skin = layer.getSkin(player);
        if (skin != null && layer.canUse(skin)) {
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(skin));

            int color = layer.getColor(player, 0.0f);

            arm.xRot = 0.0F;
            arm.render(matrices, buffer, light, OverlayTexture.NO_OVERLAY, color);
            sleeve.xRot = 0.0F;
            sleeve.render(matrices, buffer, light, OverlayTexture.NO_OVERLAY, color);
        }
    }
}
