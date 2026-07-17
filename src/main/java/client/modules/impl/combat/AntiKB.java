package client.modules.impl.combat;

import java.awt.Color;
import java.util.concurrent.LinkedBlockingDeque;

import client.Shiori;
import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventMotion;
import client.events.impl.EventMoveInput;
import client.events.impl.EventPacket;
import client.events.impl.EventRender2D;
import client.events.impl.EventRunTicks;
import client.modules.Category;
import client.modules.impl.move.Stuck;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.utils.ChatUtils;
import client.utils.RenderUtils;
import client.utils.SmoothAnimationTimer;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;
import client.values.impl.FloatValue;
import client.values.impl.ModeValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
   name = "AntiKB",
   description = "Reduces knockback",
   category = Category.COMBAT
)
public class AntiKB extends Module {
   public static AntiKB INSTANCE;
   public static boolean isAttacking;
   public static int attackCount;

   ModeValue mode = ValueBuilder.create(this, "Mode").setModes("NoXZ", "Half").build().getModeValue();

   private int attackCooldown = 0;
   private Entity attackTarget = null;
   private int attacksRemaining = 0;
   private int flagCooldown = 0;
   private boolean shouldJump = false;
   private int sprintBoostCounter = 0;
   private int hitCounter = 0;
   private boolean isSuspending = false;
   private int suspendTicks = 0;
   private ClientboundSetEntityMotionPacket knockbackPacket = null;
   private final LinkedBlockingDeque<Packet<?>> packetQueue = new LinkedBlockingDeque<>();
   private volatile boolean isFlushing = false;
   private float instantAttackProgress = 0.0f;
   private boolean isInstantAttacking = false;
   private boolean shouldFlushMotion;
   private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.0F);
   private static final int mainColor = new Color(0, 175, 255, 255).getRGB();

   public FloatValue attackAmount;
   public BooleanValue instantAttack;
   public BooleanValue sprintStateCheck;
   public ModeValue progressBarPosition;

   @Override
   public void initModule() {
      super.initModule();

      attackAmount = ValueBuilder.create(this, "Attack amount")
         .setDefaultFloatValue(5.0F)
         .setFloatStep(1.0F)
         .setMinFloatValue(1.0F)
         .setMaxFloatValue(20.0F)
         .setVisibility(() -> this.mode.isCurrentMode("NoXZ"))
         .build()
         .getFloatValue();

      instantAttack = ValueBuilder.create(this, "Instant Attack")
         .setDefaultBooleanValue(false)
         .setVisibility(() -> this.mode.isCurrentMode("NoXZ"))
         .build()
         .getBooleanValue();

      sprintStateCheck = ValueBuilder.create(this, "Sprint state check")
         .setDefaultBooleanValue(true)
         .setVisibility(() -> this.mode.isCurrentMode("NoXZ"))
         .build()
         .getBooleanValue();

      progressBarPosition = ValueBuilder.create(this, "Progress Bar Position")
         .setDefaultModeIndex(1)
         .setModes("Center", "Top", "Bottom")
         .setVisibility(() -> this.mode.isCurrentMode("NoXZ"))
         .build()
         .getModeValue();
   }

   @Override
   public void onEnable() {
      this.resetAll();
   }

   @Override
   public void onDisable() {
      this.resetAll();
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (!this.mode.isCurrentMode("NoXZ")) return;
      if (e.getType() == EventType.PRE && this.shouldFlushMotion) {
         while (!this.packetQueue.isEmpty()) {
            Packet packet = (Packet) this.packetQueue.poll();
            if (packet == null) continue;
            try {
               packet.handle(mc.getConnection());
            } catch (Exception exception) {
               exception.printStackTrace();
            }
         }
         this.shouldFlushMotion = false;
      }
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (mc.player == null) {
         return;
      }

      if (this.mode.isCurrentMode("Half")) {
         this.handleHalfMode(e);
         return;
      }

      if (!this.mode.isCurrentMode("NoXZ")) return;
      if (this.isFlushing) {
         return;
      }
      if (this.shouldIgnore()) {
         return;
      }

      if (e.getType() == EventType.SEND) {
         return;
      }

      Packet<?> packet = e.getPacket();

      if (packet instanceof ClientboundDisconnectPacket) {
         this.resetAll();
         return;
      }

      if (packet instanceof ClientboundPlayerPositionPacket) {
         if (this.isSuspending) {
            this.release();
         }
         this.resetSuspension();
         ChatUtils.addChatMessage("Flag Detected");
         this.flagCooldown = 2;
      }
      if (this.flagCooldown != 0) {
         return;
      }
      if (this.isSuspending) {
         if (!this.isAllowedPacket(packet)) {
            this.packetQueue.add(packet);
            e.setCancelled(true);
         }
         return;
      }
      if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
         if (motionPacket.getId() != mc.player.getId()) {
            return;
         }
         double dx = -motionPacket.getXa();
         double dz = -motionPacket.getZa();
         if (Math.abs(dx) > 0.01 || Math.abs(dz) > 0.01) {
            this.hitCounter = 1;
         }
         if (motionPacket.getYa() > 0) {
            Entity target;
            this.sprintBoostCounter = this.sprintBoostCounter % 100 + 100;
            if (this.sprintBoostCounter >= 100) {
               this.shouldJump = true;
            }
            boolean canAttack = this.isValidTarget(target = this.getAttackTarget()) && mc.player.isSprinting();
            if (!mc.player.onGround()) {
               this.isSuspending = true;
               this.suspendTicks = 0;
               this.knockbackPacket = motionPacket;
               e.setCancelled(true);
            } else if (canAttack) {
               this.attackTarget = target;
               this.attacksRemaining = (int) attackAmount.getCurrentValue();
            } else {
               this.isSuspending = true;
               this.suspendTicks = 0;
               this.knockbackPacket = motionPacket;
               e.setCancelled(true);
               ChatUtils.addChatMessage("Alink Wait");
            }
         }
      }
   }

   private void handleHalfMode(EventPacket e) {
      if (e.getType() == EventType.SEND) {
         return;
      }

      Packet<?> packet = e.getPacket();
      if (packet instanceof ClientboundSetEntityMotionPacket motionPacket
         && motionPacket.getId() == mc.player.getId()) {
         double x = motionPacket.getXa() / 8000.0 * 0.5;
         double y = motionPacket.getYa() / 8000.0;
         double z = motionPacket.getZa() / 8000.0 * 0.5;
         ClientboundSetEntityMotionPacket newPacket = new ClientboundSetEntityMotionPacket(
            motionPacket.getId(), new Vec3(x, y, z));
         e.setPacket(newPacket);
      }
   }

   @EventTarget
   public void onTick(EventRunTicks e) {
      if (!this.mode.isCurrentMode("NoXZ")) return;
      if (e.getType() != EventType.PRE) {
         return;
      }
      if (mc.player == null) {
         return;
      }
      if (this.attackCooldown > 0) {
         --this.attackCooldown;
         if (this.attackCooldown <= 0) {
            isAttacking = false;
            attackCount = 0;
         }
      }
      if (this.hitCounter > 0) {
         ++this.hitCounter;
         if (this.hitCounter > 2) {
            this.hitCounter = 0;
         }
      }
      if (mc.player.isDeadOrDying() || !mc.player.isAlive() || this.shouldIgnore()) {
         this.clearTarget();
         if (this.isSuspending) {
            this.release();
         }
         if (this.isInstantAttacking) {
            this.isInstantAttacking = false;
            this.instantAttackProgress = 0.0f;
            Shiori.serverTickRate = 1.0f;
         }
         return;
      }
      if (this.flagCooldown > 0) {
         --this.flagCooldown;
         this.clearTarget();
      }
      if (this.isSuspending) {
         ++this.suspendTicks;
         boolean instantAttackEnabled = instantAttack.getCurrentValue();
         if (instantAttackEnabled && this.instantAttackProgress < 3.0f) {
            float tickRate;
            Shiori.serverTickRate = tickRate = 0.5f;
            this.instantAttackProgress += 1.0f - tickRate;
            this.instantAttackProgress = Math.min(this.instantAttackProgress, 3.0f);
         }
         boolean onGround = mc.player.onGround();
         boolean isTimeout = this.suspendTicks >= 12;
         if (onGround || isTimeout) {
            ChatUtils.addChatMessage(isTimeout ? "Alink Timeout" : "ground");
            if (instantAttackEnabled) {
               Shiori.serverTickRate = 1.0f;
            }
            Entity target = this.getAttackTarget();
            boolean canAttack = this.isValidTarget(target);
            boolean sprinting = mc.player.isSprinting();
            if (onGround && canAttack && sprinting) {
               this.isFlushing = true;
               this.attackTarget = target;
               this.attacksRemaining = (int) attackAmount.getCurrentValue();
               this.applyKnockbackPacket();
               if (instantAttackEnabled && this.instantAttackProgress > 0.0f) {
                  this.attacksRemaining = (int) this.instantAttackProgress;
                  this.scheduleMotionFlush();
                  this.isSuspending = false;
                  this.suspendTicks = 0;
                  this.isFlushing = false;
                  this.isInstantAttacking = true;
                  Shiori.serverTickRate = 4.0f;
               } else {
                  this.doAttackSequence();
                  this.scheduleMotionFlush();
                  this.isSuspending = false;
                  this.suspendTicks = 0;
                  this.isFlushing = false;
               }
            } else {
               this.release();
               if (instantAttackEnabled) {
                  this.instantAttackProgress = 0.0f;
               }
               if (onGround && mc.player.isSprinting()) {
                  mc.player.setSprinting(false);
               }
            }
            return;
         }
         return;
      }
      if (this.isInstantAttacking) {
         this.instantAttackProgress -= 1.0f;
         if (this.instantAttackProgress <= 0.0f) {
            this.instantAttackProgress = 0.0f;
            this.isInstantAttacking = false;
            Shiori.serverTickRate = 1.0f;
            ChatUtils.addChatMessage("done");
         }
      }
      if (this.attacksRemaining > 0 && this.attackTarget != null) {
         this.doAttackSequence();
      }
   }

   @EventTarget
   public void onMoveInput(EventMoveInput e) {
      if (!this.mode.isCurrentMode("NoXZ")) return;
      if (mc.player == null) {
         return;
      }
      if (this.hitCounter > 0) {
         e.setForward(1.0f);
      }
      if (this.shouldJump) {
         this.shouldJump = false;
         if (mc.player.onGround() && mc.player.isSprinting() && !mc.player.hasEffect(MobEffects.JUMP) && !this.shouldIgnore()) {
            e.setJump(true);
         }
      }
   }

   @EventTarget
   public void onRender2D(EventRender2D e) {
      if (!this.mode.isCurrentMode("NoXZ")) return;
      if (mc.player == null) {
         return;
      }
      float progressTarget = 0.0f;
      if (this.isSuspending) {
         progressTarget = (float) this.suspendTicks / 12.0f * 100.0f;
      } else if (this.isInstantAttacking) {
         progressTarget = this.instantAttackProgress / 3.0f * 100.0f;
      } else if (this.attacksRemaining > 0 && this.attackTarget != null) {
         progressTarget = (float) this.attacksRemaining / attackAmount.getCurrentValue() * 100.0f;
      }
      this.progress.target = progressTarget;
      this.progress.update(true);

      if (progressTarget <= 0.0f && this.progress.value < 0.5f) {
         return;
      }

      int x = mc.getWindow().getGuiScaledWidth() / 2 - 50;
      int y;
      switch (progressBarPosition.getCurrentMode()) {
         case "Top":
            y = 10;
            break;
         case "Bottom":
            y = mc.getWindow().getGuiScaledHeight() - 20;
            break;
         default:
            y = mc.getWindow().getGuiScaledHeight() / 2 + 15;
            break;
      }
      RenderUtils.drawRoundedRect(e.getStack(), (float) x, (float) y, 100.0F, 5.0F, 2.0F, Integer.MIN_VALUE);
      RenderUtils.drawRoundedRect(e.getStack(), (float) x, (float) y, this.progress.value, 5.0F, 2.0F, mainColor);
   }

   private void resetAll() {
      this.clearTarget();
      this.flagCooldown = 0;
      this.shouldJump = false;
      this.sprintBoostCounter = 0;
      this.hitCounter = 0;
      this.resetSuspension();
   }

   private void clearTarget() {
      this.attackTarget = null;
      this.attacksRemaining = 0;
   }

   private void resetSuspension() {
      this.isSuspending = false;
      this.suspendTicks = 0;
      this.knockbackPacket = null;
      this.packetQueue.clear();
      this.isFlushing = false;
      this.instantAttackProgress = 0.0f;
      this.isInstantAttacking = false;
      Shiori.serverTickRate = 1.0f;
   }

   private boolean shouldIgnore() {
      if (mc.player == null || mc.level == null) {
         return true;
      }
      if (mc.player.isDeadOrDying() || !mc.player.isAlive() || mc.player.getHealth() <= 0.0f) {
         return true;
      }
      if (mc.player.isSpectator() || mc.player.getAbilities().flying) {
         return true;
      }
      if (mc.player.isInLava() || mc.player.isOnFire() || mc.player.isInWater() || mc.player.onClimbable() || mc.player.isSleeping()) {
         return true;
      }
      if (mc.level.getBlockState(mc.player.blockPosition()).is(Blocks.COBWEB)) {
         return true;
      }
      Stuck stuck = (Stuck) Shiori.getInstance().getModuleManager().getModule(Stuck.class);
      return stuck != null && stuck.isEnabled();
   }

   private double getAABBDistance(Entity entity) {
      if (mc.player == null) {
         return Double.MAX_VALUE;
      }
      Vec3 eyePos = mc.player.getEyePosition(1.0f);
      AABB box = entity.getBoundingBox();
      double clampedX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
      double clampedY = Math.max(box.minY, Math.min(eyePos.y, box.maxY));
      double clampedZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));
      return eyePos.distanceTo(new Vec3(clampedX, clampedY, clampedZ));
   }

   private Entity getHitResultEntity() {
      Entity hitEntity;
      if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY && (hitEntity = ((EntityHitResult) mc.hitResult).getEntity()) instanceof LivingEntity && hitEntity != mc.player && hitEntity.isAlive() && !hitEntity.isSpectator()) {
         return hitEntity;
      }
      return null;
   }

   private Entity getAttackTarget() {
      if (Killaura.target != null) {
         return Killaura.target;
      }
      return this.getHitResultEntity();
   }

   private boolean isValidTarget(Entity entity) {
      LivingEntity livingEntity;
      if (entity == null || !entity.isAlive()) {
         return false;
      }
      if (entity instanceof LivingEntity && ((livingEntity = (LivingEntity) entity).isDeadOrDying() || livingEntity.getHealth() <= 0.0f)) {
         return false;
      }
      double maxReach = 3.7f;
      return !(this.getAABBDistance(entity) > maxReach);
   }

   private void doAttackSequence() {
      if (this.attackTarget == null || !this.attackTarget.isAlive()) {
         this.clearTarget();
         return;
      }
      double maxReach = 3.7f;
      if (this.getAABBDistance(this.attackTarget) > maxReach) {
         this.clearTarget();
         return;
      }
      isAttacking = true;
      attackCount = this.attacksRemaining--;
      this.attackCooldown = 2;
      this.doAttack(this.attackTarget);
      if (this.attacksRemaining <= 0) {
         this.clearTarget();
         if (instantAttack.getCurrentValue()) {
            ChatUtils.addChatMessage("Attack " + (int) attackAmount.getCurrentValue());
         }
      }
   }

   private boolean doAttack(Entity entity) {
      if (mc.player == null || mc.gameMode == null) {
         return false;
      }
      if (sprintStateCheck.getCurrentValue() && !mc.player.isSprinting()) {
         ChatUtils.addChatMessage("not sprinting");
         return false;
      }
      boolean wasSprinting = mc.player.isSprinting();
      if (wasSprinting) {
         mc.player.setSprinting(false);
      }
      mc.gameMode.attack(mc.player, entity);
      mc.player.swing(InteractionHand.MAIN_HAND);
      if (wasSprinting) {
         Vec3 velocity = mc.player.getDeltaMovement();
         mc.player.setDeltaMovement(velocity.x * 0.6, velocity.y, velocity.z * 0.6);
      }
      if (!instantAttack.getCurrentValue()) {
         ChatUtils.addChatMessage("Attack " + this.attacksRemaining);
      }
      return true;
   }

   private void applyKnockbackPacket() {
      if (this.knockbackPacket != null && mc.getConnection() != null) {
         try {
            this.knockbackPacket.handle(mc.getConnection());
         } catch (Exception exception) {
            exception.printStackTrace();
         }
         this.knockbackPacket = null;
      }
   }

   private void scheduleMotionFlush() {
      if (mc.getConnection() == null) {
         return;
      }
      this.shouldFlushMotion = true;
   }

   private boolean isAllowedPacket(Packet<?> packet) {
      return packet instanceof ClientboundSetEntityMotionPacket
         || packet instanceof ClientboundSetHealthPacket
         || packet instanceof ClientboundPlayerPositionPacket
         || packet instanceof ClientboundSoundPacket
         || packet instanceof ClientboundPlayerChatPacket
         || packet instanceof ClientboundPlayerCombatKillPacket
         || packet instanceof ClientboundContainerClosePacket
         || packet instanceof ClientboundHurtAnimationPacket
         || packet instanceof ClientboundSetTitleTextPacket
         || packet instanceof ClientboundSetPlayerTeamPacket
         || packet instanceof ClientboundSystemChatPacket
         || packet instanceof ClientboundDisconnectPacket
         || packet instanceof ClientboundAnimatePacket && ((ClientboundAnimatePacket) packet).getId() != mc.player.getId();
   }

   private void release() {
      this.isFlushing = true;
      this.applyKnockbackPacket();
      this.scheduleMotionFlush();
      this.isFlushing = false;
      this.isSuspending = false;
      this.suspendTicks = 0;
      this.instantAttackProgress = 0.0f;
      this.isInstantAttacking = false;
      Shiori.serverTickRate = 1.0f;
   }

   static {
      isAttacking = false;
      attackCount = 0;
   }
}