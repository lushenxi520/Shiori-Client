package client.modules.impl.render;

import client.modules.Category;
import client.modules.Module;
import client.modules.impl.combat.Killaura;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;
import client.values.impl.FloatValue;
import client.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;

public class OldHitting extends Module {
    private final ModeValue animationModeSetting = ValueBuilder.create(this, "Animation")
            .setModes("Vanilla", "Leaked", "Slide")
            .setDefaultModeIndex(1)
            .build()
            .getModeValue();
    private final FloatValue sizeSetting = ValueBuilder.create(this, "Size")
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(3.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    private final FloatValue speedSetting = ValueBuilder.create(this, "Speed")
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(5.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    private final FloatValue yOffsetSetting = ValueBuilder.create(this, "Y-Offset")
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(-1.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public final BooleanValue swordOnly = ValueBuilder.create(this, "SwordOnly")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public static OldHitting INSTANCE;

    public OldHitting() {
        super("OldHitting", "Changes the sword hitting animation to old versions.", Category.RENDER);
        INSTANCE = this;
    }

    public boolean isKillAuraAttacking() {
        return Killaura.aimingTarget != null;
    }

    public static void applyTranslate(double tx, double ty, double tz, PoseStack poseStack) {
        poseStack.translate(tx, ty, tz);
    }

    public static void applyRotate(float angle, float ax, float ay, float az, PoseStack poseStack) {
        poseStack.mulPose(new Quaternionf().rotationAxis(angle * ((float)Math.PI / 180), ax, ay, az));
    }

    public static void applyScale(float sx, float sy, float sz, PoseStack poseStack) {
        poseStack.scale(sx, sy, sz);
    }

    public void applyHitAnimation(PoseStack poseStack, float progress, HumanoidArm humanoidArm, float equipProgress) {
        float scaledProgress = progress * this.speedSetting.getCurrentValue();
        float size = this.sizeSetting.getCurrentValue();
        poseStack.translate(0.0F, this.yOffsetSetting.getCurrentValue(), 0.0F);
        if (this.animationModeSetting.isCurrentMode("Vanilla")) {
            int side = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;
            OldHitting.applyTranslate((float)side * 0.56F, -0.52F + equipProgress * -0.6F, -0.72, poseStack);
            OldHitting.applyTranslate((float)side * -0.1414214F, 0.08F, 0.1414214F, poseStack);
            OldHitting.applyRotate(-102.25F, 1.0F, 0.0F, 0.0F, poseStack);
            OldHitting.applyRotate((float)side * 13.365F, 0.0F, 1.0F, 0.0F, poseStack);
            OldHitting.applyRotate((float)side * 78.05F, 0.0F, 0.0F, 1.0F, poseStack);
            double sinSquared = Math.sin((double)(scaledProgress * scaledProgress) * Math.PI);
            double sinSqrt = Math.sin(Math.sqrt(scaledProgress) * Math.PI);
            OldHitting.applyRotate((float)(sinSquared * -20.0), 0.0F, 1.0F, 0.0F, poseStack);
            OldHitting.applyRotate((float)(sinSqrt * -20.0), 0.0F, 0.0F, 1.0F, poseStack);
            OldHitting.applyRotate((float)(sinSqrt * -80.0), 1.0F, 0.0F, 0.0F, poseStack);
            OldHitting.applyScale(size, size, size, poseStack);
        }
        if (this.animationModeSetting.isCurrentMode("Leaked")) {
            this.setupLeakedAnim(poseStack, equipProgress, scaledProgress, size);
            this.setupLeakedArmPos(poseStack);
            float pulse = Mth.sin(Mth.sqrt(scaledProgress) * (float)Math.PI) / 8.0F;
            poseStack.translate(0.008, 0.24, 0.03);
            poseStack.translate(-0.16, -0.25, 0.0);
            poseStack.scale((0.8F + pulse) * size, (0.8F + pulse) * size, (0.8F + pulse) * size);
            OldHitting.applyRotate(-Mth.sin(Mth.sqrt(scaledProgress) * (float)Math.PI) * 20.0F, 0.0F, 1.2F, -0.8F, poseStack);
            OldHitting.applyRotate(-Mth.sin(Mth.sqrt(scaledProgress) * (float)Math.PI) * 30.0F, 1.0F, 0.0F, 0.0F, poseStack);
            poseStack.scale(2.4F * size, 2.4F * size, 2.4F * size);
            OldHitting.applyRotate(-38.4F, 0.0F, 1.0F, 0.0F, poseStack);
            OldHitting.applyScale(size, size, size, poseStack);
        }
        if (this.animationModeSetting.isCurrentMode("Slide")) {
            float slideSwing = Mth.sin(Mth.sqrt(scaledProgress) * (float)Math.PI);
            OldHitting.applyTranslate(0.648F, -0.55F, -0.72F, poseStack);
            OldHitting.applyTranslate(0.0F, 0.0F, 0.0F, poseStack);
            OldHitting.applyRotate(77.0F, 0.0F, 1.0F, 0.0F, poseStack);
            OldHitting.applyRotate(-10.0F, 0.0F, 0.0F, 1.0F, poseStack);
            OldHitting.applyRotate(-80.0F, 1.0F, 0.0F, 0.0F, poseStack);
            OldHitting.applyRotate(-slideSwing * 20.0F, 1.0F, 0.0F, 0.0F, poseStack);
            OldHitting.applyScale(1.2F * size, 1.2F * size, 1.2F * size, poseStack);
            OldHitting.applyScale(size, size, size, poseStack);
        }
    }

    private void setupLeakedAnim(PoseStack poseStack, float equipProgress, float scaledProgress, float size) {
        poseStack.translate(0.56F, -0.52F, -0.72F);
        OldHitting.applyRotate(45.0F, 0.0F, 1.0F, 0.0F, poseStack);
        float sinSquared = Mth.sin(scaledProgress * scaledProgress * (float)Math.PI);
        float sinSqrt = Mth.sin(Mth.sqrt(scaledProgress) * (float)Math.PI);
        OldHitting.applyRotate(sinSquared * -20.0F, 0.0F, 1.0F, 0.0F, poseStack);
        OldHitting.applyRotate(sinSqrt * -20.0F, 0.0F, 0.0F, 1.0F, poseStack);
        OldHitting.applyRotate(sinSqrt * -80.0F, 1.0F, 0.0F, 0.0F, poseStack);
        poseStack.scale(0.4F * size, 0.4F * size, 0.4F * size);
    }

    private void setupLeakedArmPos(PoseStack poseStack) {
        poseStack.translate(-0.5F, 0.2F, 0.0F);
        OldHitting.applyRotate(30.0F, 0.0F, 1.0F, 0.0F, poseStack);
        OldHitting.applyRotate(-80.0F, 1.0F, 0.0F, 0.0F, poseStack);
        OldHitting.applyRotate(60.0F, 0.0F, 1.0F, 0.0F, poseStack);
    }
}