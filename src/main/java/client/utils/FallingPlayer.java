package client.utils;

import client.utils.raytrace.ClientRayTraceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FallingPlayer {
    public double x;
    public double y;
    public double z;
    private double motionX;
    private double motionY;
    private double motionZ;
    private final float yaw;
    private final float strafe;
    private final float forward;
    private float jumpMovementFactor;
    private Minecraft mc = Minecraft.getInstance();

    public FallingPlayer(double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float strafe, float forward) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.yaw = yaw;
        this.strafe = strafe;
        this.forward = forward;
    }

    public FallingPlayer(Player player) {
        this(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getDeltaMovement().x,
                player.getDeltaMovement().y,
                player.getDeltaMovement().z,
                player.getYRot(),
                player.xxa,
                player.zza
        );
        float f = player.level().getBlockState(player.blockPosition()).getBlock().getJumpFactor();
        float f1 = player.level().getBlockState(player.getOnPos()).getBlock().getJumpFactor();
        float jumpingVelocity = 0.42F * ((double) f == 1.0 ? f1 : f) + player.getJumpBoostPower();
        this.jumpMovementFactor = jumpingVelocity;
    }

    public Vec3 getEyePos() {
        return new Vec3(x, y + 1.62, z);
    }

    public Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    private void calculateForTick() {
        float sr = this.strafe * 0.98F;
        float fw = this.forward * 0.98F;
        float v = sr * sr + fw * fw;
        if (v >= 1.0E-4F) {
            v = Mth.sqrt(v);
            if (v < 1.0F) {
                v = 1.0F;
            }

            float fixedJumpFactor = this.jumpMovementFactor;
            if (this.mc.player != null && this.mc.player.isSprinting()) {
                fixedJumpFactor *= 1.3F;
            }

            v = fixedJumpFactor / v;
            sr *= v;
            fw *= v;
            float f1 = Mth.sin(this.yaw * (float) Math.PI / 180.0F);
            float f2 = Mth.cos(this.yaw * (float) Math.PI / 180.0F);
            this.motionX += (double) (sr * f2 - fw * f1);
            this.motionZ += (double) (fw * f2 + sr * f1);
        }

        this.motionY -= 0.08;
        this.motionY *= 0.98F;
        this.x = this.x + this.motionX;
        this.y = this.y + this.motionY;
        this.z = this.z + this.motionZ;
        this.motionX *= 0.91;
        this.motionZ *= 0.91;
    }

    private void calculateForTick2() {
        float sr = this.strafe;
        float fw = this.forward;
        float v = sr * sr + fw * fw;
        if (v >= 1.0E-4F) {
            v = Mth.sqrt(v);
            if (v < 1.0F) {
                v = 1.0F;
            }

            float fixedJumpFactor = this.jumpMovementFactor;
            if (this.mc.player != null && this.mc.player.isSprinting()) {
                fixedJumpFactor *= 1.3F;
            }

            v = fixedJumpFactor / v;
            sr *= v;
            fw *= v;
            float f1 = Mth.sin(this.yaw * (float) Math.PI / 180.0F);
            float f2 = Mth.cos(this.yaw * (float) Math.PI / 180.0F);
            this.motionX += (double) (sr * f2 - fw * f1);
            this.motionZ += (double) (fw * f2 + sr * f1);
        }

        this.motionY -= 0.08;
        this.motionY *= 0.98F;
        this.x = this.x + this.motionX;
        this.y = this.y + this.motionY;
        this.z = this.z + this.motionZ;
    }

    public void calculate(int ticks) {
        for (int i = 0; i < ticks; i++) {
            this.calculateForTick();
        }
    }

    public void calculateMLG(int ticks) {
        for (int i = 0; i < ticks; i++) {
            this.calculateForTick2();
        }
    }

    public BlockPos findCollision(int ticks) {
        float w = mc.player != null ? mc.player.getBbWidth() / 2f : 0.3f;
        for (int i = 0; i < ticks; i++) {
            Vec3 start = new Vec3(x, y, z);
            calculateForTick();
            Vec3 end = new Vec3(x, y, z);

            double x = start.x;
            double y = start.y;
            double z = start.z;
            double x2 = end.x;
            double y2 = end.y;
            double z2 = end.z;

            BlockPos raytracedBlock;
            if ((raytracedBlock = rayTrace(start, end)) != null) {
                return raytracedBlock;
            }

            if ((raytracedBlock = rayTrace(new Vec3(x - w, y, z - w), new Vec3(x2 - w, y2, z2 - w))) != null)
                return raytracedBlock;
            if ((raytracedBlock = rayTrace(new Vec3(x - w, y, z + w), new Vec3(x2 - w, y2, z2 + w))) != null)
                return raytracedBlock;
            if ((raytracedBlock = rayTrace(new Vec3(x + w, y, z - w), new Vec3(x2 + w, y2, z2 - w))) != null)
                return raytracedBlock;
            if ((raytracedBlock = rayTrace(new Vec3(x + w, y, z + w), new Vec3(x2 + w, y2, z2 + w))) != null)
                return raytracedBlock;

            if ((raytracedBlock = rayTrace(new Vec3(x - w, y + 1.62, z - w), new Vec3(x2 - w, y2 + 1.62, z2 - w))) != null)
                return raytracedBlock;
            if ((raytracedBlock = rayTrace(new Vec3(x - w, y + 1.62, z + w), new Vec3(x2 - w, y2 + 1.62, z2 + w))) != null)
                return raytracedBlock;
            if ((raytracedBlock = rayTrace(new Vec3(x + w, y + 1.62, z - w), new Vec3(x2 + w, y2 + 1.62, z2 - w))) != null)
                return raytracedBlock;
            if ((raytracedBlock = rayTrace(new Vec3(x + w, y + 1.62, z + w), new Vec3(x2 + w, y2 + 1.62, z2 + w))) != null)
                return raytracedBlock;
        }
        return null;
    }

    private BlockPos rayTrace(Vec3 start, Vec3 end) {
        if (start == null || end == null) return null;
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z)) return null;
        if (Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z)) return null;

        int x1 = Mth.floor(start.x);
        int y1 = Mth.floor(start.y);
        int z1 = Mth.floor(start.z);
        int x2 = Mth.floor(end.x);
        int y2 = Mth.floor(end.y);
        int z2 = Mth.floor(end.z);

        BlockPos pos = BlockPos.containing(x1, y1, z1);

        if (mc.player == null) return null;
        Level world = mc.player.level();

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);

        int stepX = Integer.compare(x2, x1);
        int stepY = Integer.compare(y2, y1);
        int stepZ = Integer.compare(z2, z1);

        double tMaxX = x1 == x2 ? Double.MAX_VALUE : Math.abs((stepX > 0 ? x1 + 1 - start.x : start.x - x1) / (end.x - start.x));
        double tMaxY = y1 == y2 ? Double.MAX_VALUE : Math.abs((stepY > 0 ? y1 + 1 - start.y : start.y - y1) / (end.y - start.y));
        double tMaxZ = z1 == z2 ? Double.MAX_VALUE : Math.abs((stepZ > 0 ? z1 + 1 - start.z : start.z - z1) / (end.z - start.z));

        double tDeltaX = dx == 0 ? Double.MAX_VALUE : Math.abs(1.0 / (end.x - start.x));
        double tDeltaY = dy == 0 ? Double.MAX_VALUE : Math.abs(1.0 / (end.y - start.y));
        double tDeltaZ = dz == 0 ? Double.MAX_VALUE : Math.abs(1.0 / (end.z - start.z));

        int totalSteps = dx + dy + dz;
        int maxSteps = Math.min(totalSteps + 1, 100);

        for (int step = 0; step < maxSteps; step++) {
            pos = BlockPos.containing(x1, y1, z1);
            if (!world.isEmptyBlock(pos)) {
                VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos, CollisionContext.of(mc.player));
                if (!shape.isEmpty()) {
                    return pos;
                }
            }
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x1 += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                y1 += stepY;
                tMaxY += tDeltaY;
            } else {
                z1 += stepZ;
                tMaxZ += tDeltaZ;
            }
        }
        return null;
    }
}