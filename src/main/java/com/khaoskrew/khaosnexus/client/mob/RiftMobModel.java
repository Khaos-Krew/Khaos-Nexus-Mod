package com.khaoskrew.khaosnexus.client.mob;

import com.khaoskrew.khaosnexus.mob.RiftMob;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** One model class for every Rift mob; the geometry comes from RiftMobLayers. */
public class RiftMobModel extends HierarchicalModel<RiftMob> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart orb;
    private final ModelPart cape;
    private final boolean caster;

    public RiftMobModel(ModelPart root, boolean caster) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.orb = root.hasChild("orb") ? root.getChild("orb") : null;
        ModelPart body = root.getChild("body");
        this.cape = body.hasChild("cape") ? body.getChild("cape") : null;
        this.caster = caster;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(RiftMob mob, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.rightLeg.xRot = swing;
        this.leftLeg.xRot = -swing;
        float idle = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        if (this.caster) {
            // Both hands cradle the orb in front of the chest.
            this.rightArm.xRot = -0.9F + idle;
            this.leftArm.xRot = -0.9F - idle;
            this.rightArm.yRot = -0.35F;
            this.leftArm.yRot = 0.35F;
        } else {
            this.rightArm.xRot = -swing * 0.8F + idle;
            this.leftArm.xRot = swing * 0.8F - idle;
            this.rightArm.zRot = 0.05F + idle;
            this.leftArm.zRot = -0.05F - idle;
        }
        if (this.attackTime > 0.0F) {
            float a = Mth.sin(this.attackTime * Mth.PI);
            this.rightArm.xRot -= a * 1.6F;
            this.leftArm.xRot -= a * 1.6F;
        }
        if (this.cape != null) {
            this.cape.xRot += Mth.sin(ageInTicks * 0.08F) * 0.04F + limbSwingAmount * 0.5F;
        }
        if (this.orb != null) {
            this.orb.y += Mth.sin(ageInTicks * 0.15F) * 1.0F;
            this.orb.yRot = ageInTicks * 0.1F;
            this.orb.xRot = ageInTicks * 0.07F;
        }
    }
}
