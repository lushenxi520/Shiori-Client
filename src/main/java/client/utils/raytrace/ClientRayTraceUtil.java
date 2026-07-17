package client.utils.raytrace;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.function.Predicate;

public final class ClientRayTraceUtil {
    private static final double EPSILON = 1.0E-7D;
    public static Vec3 eyePos = null;
    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean didHitBlockFace(float yaw, float pitch, BlockPos targetPos, Direction expectedFace, boolean strict) {
        return didHitBlockFace(mc.player, yaw, pitch, targetPos, expectedFace, strict, ClientRayTraceUtil::isIgnoredBlock);
    }

    public static boolean didHitBlockFace(Player player, float yaw, float pitch, BlockPos targetPos, Direction expectedFace, boolean strict) {
        return didHitBlockFace(player, yaw, pitch, targetPos, expectedFace, strict, ClientRayTraceUtil::isIgnoredBlock);
    }

    public static boolean didHitBlockFace(float yaw, float pitch, BlockPos targetPos, Direction expectedFace, boolean strict, Predicate<BlockState> ignorePredicate) {
        return didHitBlockFace(mc.player, yaw, pitch, targetPos, expectedFace, strict, ignorePredicate);
    }

    public static boolean didHitBlockFace(Player player, float yaw, float pitch, BlockPos targetPos, Direction expectedFace, boolean strict, Predicate<BlockState> ignorePredicate) {
        if (player == null || expectedFace == null) {
            return false;
        }

        BlockHitResult result = getFacedBlock(yaw, pitch, ignorePredicate);
        if (result == null || targetPos.getX() != result.getBlockPos().getX() || targetPos.getY() != result.getBlockPos().getY() || targetPos.getZ() != result.getBlockPos().getZ() || (expectedFace != result.getDirection() && strict)) {
            return false;
        }
        return true;
    }

    public static void updateEyePos() {
        if (mc.player == null) return;
        eyePos = mc.player.getEyePosition();
    }

    public static BlockHitResult getFacedBlock(float yaw, float pitch) {
        return getFacedBlock(yaw, pitch, ClientRayTraceUtil::isIgnoredBlock);
    }

    public static BlockHitResult getFacedBlock(float yaw, float pitch, Predicate<BlockState> ignorePredicate) {
        final double reachDistance = mc.gameMode.getPickRange();
        if (yaw == 0.0f && pitch == 0.0f) {
            return null;
        }

        Vec3 startPos = eyePos;
        Vec3 direction = fromPolar(pitch, yaw);
        Vec3 endPos = startPos.add(direction.scale(reachDistance));
        if (direction.x == 0) direction = new Vec3(EPSILON, direction.y, direction.z);
        if (direction.y == 0) direction = new Vec3(direction.x, EPSILON, direction.z);
        if (direction.z == 0) direction = new Vec3(direction.x, direction.y, EPSILON);

        BlockPos currentPos = BlockPos.containing(startPos);
        int stepX = (int) Math.signum(direction.x);
        int stepY = (int) Math.signum(direction.y);
        int stepZ = (int) Math.signum(direction.z);

        double nextBoundaryX = (stepX > 0) ? currentPos.getX() + 1 : currentPos.getX();
        double nextBoundaryY = (stepY > 0) ? currentPos.getY() + 1 : currentPos.getY();
        double nextBoundaryZ = (stepZ > 0) ? currentPos.getZ() + 1 : currentPos.getZ();

        double tMaxX = (nextBoundaryX - startPos.x) / direction.x;
        double tMaxY = (nextBoundaryY - startPos.y) / direction.y;
        double tMaxZ = (nextBoundaryZ - startPos.z) / direction.z;

        double tDeltaX = stepX / direction.x;
        double tDeltaY = stepY / direction.y;
        double tDeltaZ = stepZ / direction.z;

        final var world = mc.player.level();
        AABB box;
        while (startPos.distanceTo(Vec3.atCenterOf(currentPos)) <= reachDistance) {
            if (!world.isEmptyBlock(currentPos)) {
                BlockState state = world.getBlockState(currentPos);
                if (!ignorePredicate.test(state)) {
                    VoxelShape shape;
                    if (state.getFluidState().isSource() && state.getFluidState().getType() == Fluids.WATER) {
                        shape = Shapes.block();
                    } else {
                        shape = state.getCollisionShape(world, currentPos, CollisionContext.of(mc.player));
                    }

                    if (!shape.isEmpty()) {
                        for (AABB localBox : shape.toAabbs()) {
                            box = localBox.move(currentPos);
                            Optional<Vec3> intercept = box.clip(startPos, endPos);

                            if (intercept.isPresent()) {
                                Vec3 hitVec = intercept.get();
                                Direction side = getHitFaceFromBox(hitVec, box);
                                boolean isInside = box.contains(startPos);
                                return new BlockHitResult(hitVec, side, currentPos, isInside);
                            }
                        }
                    }
                }
            }
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentPos = currentPos.offset(stepX, 0, 0);
                    tMaxX += tDeltaX;
                } else {
                    currentPos = currentPos.offset(0, 0, stepZ);
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currentPos = currentPos.offset(0, stepY, 0);
                    tMaxY += tDeltaY;
                } else {
                    currentPos = currentPos.offset(0, 0, stepZ);
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return null;
    }

    public static boolean isIgnoredBlock(BlockState state) {
        var block = state.getBlock();
        return block instanceof BushBlock || block instanceof SnowLayerBlock || block instanceof AirBlock || block instanceof TallGrassBlock || block instanceof LiquidBlock;
    }

    private static Direction getHitFaceFromBox(Vec3 hit, AABB box) {
        final double eps = 1e-7;

        if (Math.abs(hit.x - box.minX) <= eps) return Direction.WEST;
        if (Math.abs(hit.x - box.maxX) <= eps) return Direction.EAST;
        if (Math.abs(hit.y - box.minY) <= eps) return Direction.DOWN;
        if (Math.abs(hit.y - box.maxY) <= eps) return Direction.UP;
        if (Math.abs(hit.z - box.minZ) <= eps) return Direction.NORTH;
        if (Math.abs(hit.z - box.maxZ) <= eps) return Direction.SOUTH;

        double dxMin = Math.abs(hit.x - box.minX);
        double dxMax = Math.abs(hit.x - box.maxX);
        double dyMin = Math.abs(hit.y - box.minY);
        double dyMax = Math.abs(hit.y - box.maxY);
        double dzMin = Math.abs(hit.z - box.minZ);
        double dzMax = Math.abs(hit.z - box.maxZ);

        double m = dxMin; Direction d = Direction.WEST;
        if (dxMax < m) { m = dxMax; d = Direction.EAST; }
        if (dyMin < m) { m = dyMin; d = Direction.DOWN; }
        if (dyMax < m) { m = dyMax; d = Direction.UP; }
        if (dzMin < m) { m = dzMin; d = Direction.NORTH; }
        if (dzMax < m) { d = Direction.SOUTH; }

        return d;
    }

    public static BlockHitResult getFacedContainerBlock(float yaw, float pitch) {
        final double reachDistance = mc.gameMode.getPickRange();
        Vec3 startPos = eyePos;
        Vec3 direction = fromPolar(pitch, yaw);
        Vec3 endPos = startPos.add(direction.scale(reachDistance));

        if (direction.x == 0) direction = new Vec3(EPSILON, direction.y, direction.z);
        if (direction.y == 0) direction = new Vec3(direction.x, EPSILON, direction.z);
        if (direction.z == 0) direction = new Vec3(direction.x, direction.y, EPSILON);

        BlockPos currentPos = BlockPos.containing(startPos);
        int stepX = (int) Math.signum(direction.x);
        int stepY = (int) Math.signum(direction.y);
        int stepZ = (int) Math.signum(direction.z);

        double nextBoundaryX = (stepX > 0) ? currentPos.getX() + 1 : currentPos.getX();
        double nextBoundaryY = (stepY > 0) ? currentPos.getY() + 1 : currentPos.getY();
        double nextBoundaryZ = (stepZ > 0) ? currentPos.getZ() + 1 : currentPos.getZ();

        double tMaxX = (nextBoundaryX - startPos.x) / direction.x;
        double tMaxY = (nextBoundaryY - startPos.y) / direction.y;
        double tMaxZ = (nextBoundaryZ - startPos.z) / direction.z;

        double tDeltaX = stepX / direction.x;
        double tDeltaY = stepY / direction.y;
        double tDeltaZ = stepZ / direction.z;

        while (startPos.distanceTo(Vec3.atCenterOf(currentPos)) <= reachDistance) {
            if (!mc.player.level().isEmptyBlock(currentPos) && isContainerBlock(mc.player.level().getBlockState(currentPos))) {
                AABB currentBox = new AABB(currentPos);
                Optional<Vec3> intercept = currentBox.clip(startPos, endPos);

                if (intercept.isPresent()) {
                    Vec3 hitVec = intercept.get();
                    Direction side = getHitFace(hitVec, currentBox);
                    boolean isInside = currentBox.contains(startPos);
                    return new BlockHitResult(hitVec, side, currentPos, isInside);
                }
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentPos = currentPos.offset(stepX, 0, 0);
                    tMaxX += tDeltaX;
                } else {
                    currentPos = currentPos.offset(0, 0, stepZ);
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currentPos = currentPos.offset(0, stepY, 0);
                    tMaxY += tDeltaY;
                } else {
                    currentPos = currentPos.offset(0, 0, stepZ);
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return null;
    }

    private static boolean isContainerBlock(BlockState state) {
        return state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock ||
                state.getBlock() instanceof net.minecraft.world.level.block.EnderChestBlock ||
                state.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock;
    }

    public static double getDistance(float yaw, float pitch, Entity target) {
        Vec3 dir = fromPolar(pitch, yaw).normalize();
        AABB targetBox = target.getBoundingBox();
        return intersectRayAabb(eyePos, dir, targetBox);
    }

    public static boolean didHitEntity(float yaw, float pitch, double range, Entity target) {
        return overBox(yaw, pitch, range, target.getBoundingBox());
    }

    public static boolean overBox(float yawDeg, float pitchDeg, double range, AABB box) {
        if (box == null) {
            return false;
        }

        Vec3 start = eyePos;
        Vec3 dir = fromPolar(pitchDeg, yawDeg).normalize();
        if (dir.lengthSqr() < 1e-12) return false;
        double tBox = intersectRayAabb(start, dir, box);
        if (!Double.isFinite(tBox) || tBox < 0.0 || tBox > range) {
            return false;
        }
        if (isOccludedBefore(start, dir, tBox, range, mc.player)) {
            return false;
        }
        return true;
    }

    private static boolean isOccludedBefore(Vec3 origin, Vec3 dir, double tStop, double range, Entity viewer) {
        var world = mc.player.level();
        int x = Mth.floor(origin.x);
        int y = Mth.floor(origin.y);
        int z = Mth.floor(origin.z);

        int stepX = dir.x > 0 ? 1 : (dir.x < 0 ? -1 : 0);
        int stepY = dir.y > 0 ? 1 : (dir.y < 0 ? -1 : 0);
        int stepZ = dir.z > 0 ? 1 : (dir.z < 0 ? -1 : 0);

        double tMaxX = nextBoundaryT(origin.x, dir.x, x, stepX);
        double tMaxY = nextBoundaryT(origin.y, dir.y, y, stepY);
        double tMaxZ = nextBoundaryT(origin.z, dir.z, z, stepZ);

        double tDeltaX = stepX != 0 ? 1.0 / Math.abs(dir.x) : Double.POSITIVE_INFINITY;
        double tDeltaY = stepY != 0 ? 1.0 / Math.abs(dir.y) : Double.POSITIVE_INFINITY;
        double tDeltaZ = stepZ != 0 ? 1.0 / Math.abs(dir.z) : Double.POSITIVE_INFINITY;
        double tLimit = Math.min(tStop, range);
        if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
            return true;
        }
        while (true) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (tMaxX > tLimit) break;
                    x += stepX;
                    if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                        return true;
                    }
                    tMaxX += tDeltaX;
                } else {
                    if (tMaxZ > tLimit) break;
                    z += stepZ;
                    if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                        return true;
                    }
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (tMaxY > tLimit) break;
                    y += stepY;
                    if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                        return true;
                    }
                    tMaxY += tDeltaY;
                } else {
                    if (tMaxZ > tLimit) break;
                    z += stepZ;
                    if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                        return true;
                    }
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return false;
    }

    private static double nextBoundaryT(double o, double d, int cell, int step) {
        if (step == 0) return Double.POSITIVE_INFINITY;
        double boundary = step > 0 ? (cell + 1) : cell;
        return (boundary - o) / d;
    }

    private static boolean blockOccludesHere(net.minecraft.world.level.Level world, Vec3 origin, Vec3 dir, BlockPos pos, double tEntity, Entity viewer) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return false;
        VoxelShape shape = state.getShape(world, pos, viewer != null ? CollisionContext.of(viewer) : CollisionContext.empty());
        if (shape.isEmpty()) return false;
        for (AABB local : shape.toAabbs()) {
            AABB box = local.move(pos);
            double tBox = intersectRayAabb(origin, dir, box);
            if (Double.isFinite(tBox) && tBox >= 0.0 && tBox < tEntity) {
                return true;
            }
        }
        return false;
    }

    public static double intersectRayAabb(Vec3 o, Vec3 d, AABB b) {
        double tMin = 0.0;
        double tMax = Double.POSITIVE_INFINITY;

        double[] tmp = new double[2];

        if (!axisSlab(o.x, d.x, b.minX, b.maxX, tmp)) return Double.POSITIVE_INFINITY;
        tMin = Math.max(tMin, tmp[0]); tMax = Math.min(tMax, tmp[1]);
        if (tMax < tMin) return Double.POSITIVE_INFINITY;

        if (!axisSlab(o.y, d.y, b.minY, b.maxY, tmp)) return Double.POSITIVE_INFINITY;
        tMin = Math.max(tMin, tmp[0]); tMax = Math.min(tMax, tmp[1]);
        if (tMax < tMin) return Double.POSITIVE_INFINITY;

        if (!axisSlab(o.z, d.z, b.minZ, b.maxZ, tmp)) return Double.POSITIVE_INFINITY;
        tMin = Math.max(tMin, tmp[0]); tMax = Math.min(tMax, tmp[1]);
        if (tMax < tMin) return Double.POSITIVE_INFINITY;

        return tMin;
    }

    private static boolean axisSlab(double o, double d, double min, double max, double[] out) {
        if (Math.abs(d) < 1e-12) {
            if (o < min || o > max) return false;
            out[0] = Double.NEGATIVE_INFINITY;
            out[1] = Double.POSITIVE_INFINITY;
            return true;
        } else {
            double inv = 1.0 / d;
            double t1 = (min - o) * inv;
            double t2 = (max - o) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            out[0] = t1;
            out[1] = t2;
            return true;
        }
    }

    private static Direction getHitFace(Vec3 hitPos, AABB box) {
        if (Math.abs(hitPos.x - box.minX) < EPSILON) return Direction.WEST;
        if (Math.abs(hitPos.x - box.maxX) < EPSILON) return Direction.EAST;
        if (Math.abs(hitPos.y - box.minY) < EPSILON) return Direction.DOWN;
        if (Math.abs(hitPos.y - box.maxY) < EPSILON) return Direction.UP;
        if (Math.abs(hitPos.z - box.minZ) < EPSILON) return Direction.NORTH;
        if (Math.abs(hitPos.z - box.maxZ) < EPSILON) return Direction.SOUTH;
        return null;
    }

    private static Vec3 fromPolar(float pitch, float yaw) {
        float f = pitch * ((float)Math.PI / 180F);
        float g = -yaw * ((float)Math.PI / 180F);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    private ClientRayTraceUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}