package client.modules.impl.misc;

import client.events.api.EventTarget;
import client.events.impl.EventPacket;
import client.files.FileManager;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.utils.ChatUtils;
import client.utils.MathUtils;
import client.utils.NetworkUtils;
import client.utils.animation.Timer;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;
import java.lang.reflect.Field;
import java.util.Random;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import sun.misc.Unsafe;

@ModuleInfo(
   name = "Disabler",
   category = Category.MISC,
   description = "Disables some checks of the anti cheat."
)
public class Disabler extends Module {
   public static Disabler INSTANCE;
   private final BooleanValue logging = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
   private final BooleanValue badPacketsA = ValueBuilder.create(this, "Grim Bad PacketsA").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimDuplicateRotPlace = ValueBuilder.create(this, "Grim Duplicate RotPlace")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   private final BooleanValue acaFastSwitch = ValueBuilder.create(this, "ACA Fast Switch").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue acaInventoryFrequency = ValueBuilder.create(this, "ACA Inventory Frequency")
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   private final BooleanValue acaAimStep = ValueBuilder.create(this, "ACA Aim Step").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue acaPerfectRotation = ValueBuilder.create(this, "ACA Perfect Rotation")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   private final BooleanValue themisBlink = ValueBuilder.create(this, "Themis Blink").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue onlyRemoteServer = ValueBuilder.create(this, "Only Remote Server")
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();

   private int lastSentSlot = -1;
   private long inventoryOpenTime = 0L;
   private boolean inventoryOpen = false;
   private final Timer inventoryTimer = new Timer();
   private ServerboundContainerClosePacket storedClosePacket = null;
   private long inventoryCloseDelay = 0L;
   private long themisBlinkLastSend = System.currentTimeMillis();
   private int themisBlinkCount = 0;

   private float lastYaw = 0.0F;
   private float lastPitch = 0.0F;
   private float yawDelta = 0.0F;
   private float pitchDelta = 0.0F;
   private float currentYaw = 0.0F;
   private float currentPitch = 0.0F;
   private float yawDiff = 0.0F;
   private float pitchDiff = 0.0F;
   private float lastPlacedYawDiff = 0.0F;
   private float lastPlacedPitchDiff = 0.0F;
   private boolean rotated = false;
   private final Random random = new Random();
   private static final double[] perfectRotSteps = new double[]{
      0.0, 5.625, 11.25, 16.875, 22.5, 28.125, 33.75, 39.375,
      45.0, 50.625, 56.25, 61.875, 67.5, 73.125, 78.75, 84.375, 90.0
   };
   private static final Unsafe unsafe;

   public Disabler() {
      super("Disabler", "Disables some checks of the anti cheat.", Category.MISC);
      INSTANCE = this;
   }

   public void log(String message) {
      if (INSTANCE != null && INSTANCE.isEnabled() && this.logging.getCurrentValue()) {
         ChatUtils.addChatMessage("[Disabler] " + message);
      }
   }

   public void resetState() {
      this.lastSentSlot = -1;
      this.inventoryOpenTime = 0L;
      this.inventoryOpen = false;
      this.storedClosePacket = null;
      this.inventoryCloseDelay = 0L;
      this.lastYaw = 0.0F;
      this.lastPitch = 0.0F;
      this.yawDelta = 0.0F;
      this.pitchDelta = 0.0F;
      this.currentYaw = 0.0F;
      this.currentPitch = 0.0F;
      this.yawDiff = 0.0F;
      this.pitchDiff = 0.0F;
      this.lastPlacedYawDiff = 0.0F;
      this.lastPlacedPitchDiff = 0.0F;
      this.rotated = false;
   }

   private boolean isMoving() {
      return mc.player != null && mc.player.getDeltaMovement().lengthSqr() > 0.001;
   }

   @Override
   public void onEnable() {
      this.resetState();
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (mc.player == null || mc.isSingleplayer() && this.onlyRemoteServer.getCurrentValue()) {
         return;
      }

      Packet<?> packet = e.getPacket();

      if (packet instanceof ClientboundLoginPacket) {
         this.resetState();
         return;
      }

      if (mc.player.isSpectator() || !mc.player.isAlive() || mc.player.isDeadOrDying()
            || mc.screen instanceof ProgressScreen) {
         this.resetState();
         return;
      }

      if (this.storedClosePacket != null && this.inventoryTimer.hasPassed(this.inventoryCloseDelay)) {
         NetworkUtils.sendPacketNoEvent(this.storedClosePacket);
         this.log("InventoryFrequency: Released stored close packet");
         this.storedClosePacket = null;
      }

      if (packet instanceof ClientboundOpenScreenPacket) {
         this.inventoryOpenTime = System.currentTimeMillis();
         this.inventoryOpen = true;
         this.log("Inventory opened at: " + this.inventoryOpenTime);
      }

      if (packet instanceof ServerboundSetCarriedItemPacket carriedItemPacket) {
         int slot = carriedItemPacket.getSlot();
         if (this.badPacketsA.getCurrentValue() && slot == this.lastSentSlot && slot != -1) {
            e.setCancelled(true);
            this.log("BadPacketsA: Cancelled duplicate slot packet: " + slot);
            return;
         }
         if (this.acaFastSwitch.getCurrentValue() && this.lastSentSlot != -1 && slot != this.lastSentSlot) {
            this.sendIntermediateSlots(this.lastSentSlot, slot);
         }
         this.lastSentSlot = slot;
         this.log("Processed slot switch: " + this.lastSentSlot + " -> " + slot);
      }

      if (this.acaInventoryFrequency.getCurrentValue() && packet instanceof ServerboundContainerClosePacket closePacket) {
         if (this.inventoryOpen) {
            long now = System.currentTimeMillis();
            long openDuration = now - this.inventoryOpenTime;
            if (openDuration <= 150L) {
               e.setCancelled(true);
               this.storedClosePacket = closePacket;
               this.inventoryCloseDelay = 151L - openDuration;
               this.inventoryTimer.reset();
               this.log("InventoryFrequency: Storing close packet, will send after " + this.inventoryCloseDelay + "ms");
               this.inventoryOpen = false;
               return;
            }
            this.inventoryOpen = false;
            this.log("InventoryFrequency: Allowed close packet after " + openDuration + "ms");
         }
      }

      if (this.themisBlink.getCurrentValue()) {
         if (System.currentTimeMillis() - this.themisBlinkLastSend > 200L) {
            if (this.themisBlinkCount == 0) {
               NetworkUtils.sendPacketNoEvent(new ServerboundPongPacket(0));
            }
            this.themisBlinkLastSend = System.currentTimeMillis();
            this.themisBlinkCount = 0;
         }
         if (packet instanceof ServerboundMovePlayerPacket.StatusOnly || packet instanceof ServerboundPongPacket) {
            ++this.themisBlinkCount;
         }
      }

      if (this.grimDuplicateRotPlace.getCurrentValue()) {
         if (packet instanceof ServerboundMovePlayerPacket movePacket) {
            if (movePacket.hasRotation()) {
               float prevYaw = this.currentYaw;
               float prevPitch = this.currentPitch;
               this.currentYaw = getPacketYRot(movePacket);
               this.currentPitch = getPacketXRot(movePacket);
               this.yawDiff = Math.abs(this.currentYaw - prevYaw);
               this.pitchDiff = Math.abs(this.currentPitch - prevPitch);
               this.rotated = true;

               float yawDelta;
               if (this.yawDiff > 2.0F && (double)(yawDelta = Math.abs(this.yawDiff - this.lastPlacedYawDiff)) < 1.0E-4) {
                  float jitter = 0.001F + this.random.nextFloat() * 0.009F;
                  float newYaw = this.currentYaw - jitter;
                  setPacketYRot(movePacket, newYaw);
                  this.log("DuplicateRotPlace: Modified yaw from " + this.currentYaw + " to " + newYaw + " (yawDiff: " + yawDelta + ")");
               }

               float pitchDelta;
               if (this.pitchDiff > 2.0F && (double)(pitchDelta = Math.abs(this.pitchDiff - this.lastPlacedPitchDiff)) < 1.0E-4) {
                  float jitter = 0.001F + this.random.nextFloat() * 0.009F;
                  float newPitch = MathUtils.clampPitch_To90(this.currentPitch - jitter);
                  setPacketXRot(movePacket, newPitch);
                  this.log("DuplicateRotPlace: Modified pitch from " + this.currentPitch + " to " + newPitch + " (pitchDiff: " + pitchDelta + ")");
               }
            }
         } else if (packet instanceof ServerboundUseItemOnPacket && this.rotated) {
            this.lastPlacedYawDiff = this.yawDiff;
            this.lastPlacedPitchDiff = this.pitchDiff;
            this.rotated = false;
         }
      }

      if ((this.acaAimStep.getCurrentValue() || this.acaPerfectRotation.getCurrentValue())
            && packet instanceof ServerboundMovePlayerPacket movePacket) {
         float[] fArray;
         float yawAim = getPacketYRot(movePacket);
         float pitchAim = getPacketXRot(movePacket);
         boolean modified = false;

         if (this.acaAimStep.getCurrentValue() && this.isAimStepRotation(yawAim, pitchAim)) {
            float[] result = this.applyAimStep(yawAim, pitchAim);
            yawAim = result[0];
            pitchAim = result[1];
            modified = true;
         }

         if (this.acaPerfectRotation.getCurrentValue() && ((fArray = this.applyPerfectRotation(yawAim, pitchAim))[0] != yawAim || fArray[1] != pitchAim)) {
            yawAim = fArray[0];
            pitchAim = fArray[1];
            modified = true;
            this.log("PerfectRotation: Modified rotation");
         }

         if (modified) {
            setPacketYRot(movePacket, yawAim);
            setPacketXRot(movePacket, MathUtils.clampPitch_To90(pitchAim));
         }

         this.lastYaw = getPacketYRot(movePacket);
         this.lastPitch = getPacketXRot(movePacket);
      }
   }

   private boolean isAimStepRotation(float yaw, float pitch) {
      if (this.lastYaw == 0.0F && this.lastPitch == 0.0F) {
         return false;
      }
      double yawDelta = Math.abs(this.wrapDegrees(yaw - this.lastYaw));
      double pitchDelta = Math.abs(pitch - this.lastPitch);
      boolean yawStuck = yawDelta < 1.0E-5 && pitchDelta > 1.0;
      boolean pitchStuck = pitchDelta < 1.0E-5 && yawDelta > 1.0;
      return yawStuck || pitchStuck;
   }

   private float[] applyAimStep(float yaw, float pitch) {
      double yawDelta = Math.abs(this.wrapDegrees(yaw - this.lastYaw));
      double pitchDelta = Math.abs(pitch - this.lastPitch);
      float newYaw = yaw;
      float newPitch = pitch;
      if (yawDelta < 1.0E-5 && pitchDelta > 1.0) {
         newYaw = this.lastYaw + (float) (this.random.nextGaussian() * 0.001);
      }
      if (pitchDelta < 1.0E-5 && yawDelta > 1.0) {
         newPitch = this.lastPitch + (float) (this.random.nextGaussian() * 0.001);
      }
      return new float[]{newYaw, newPitch};
   }

   private float[] applyPerfectRotation(float yaw, float pitch) {
      if (this.lastYaw == 0.0F && this.lastPitch == 0.0F) {
         return new float[]{yaw, pitch};
      }
      double yawDelta = Math.abs(this.wrapDegrees(yaw - this.lastYaw));
      double pitchDelta = Math.abs(pitch - this.lastPitch);
      float newYaw = yaw;
      float newPitch = pitch;
      if (!this.isNearZeroOrMultiple(yawDelta) && this.isKnownRotationStep(yawDelta)) {
         double jitter = this.random.nextGaussian() * 0.005;
         newYaw = yaw + (float) jitter;
      }
      if (!this.isNearZeroOrMultiple(pitchDelta) && this.isKnownRotationStep(pitchDelta)) {
         double jitter = this.random.nextGaussian() * 0.005;
         newPitch = pitch + (float) jitter;
      }
      return new float[]{newYaw, newPitch};
   }

   private boolean isNearZeroOrMultiple(double value) {
      return Math.abs(value) <= 1.0E-10 || this.isMultipleOf(360.0, value);
   }

   private boolean isKnownRotationStep(double value) {
      if (Double.isInfinite(value) || Double.isNaN(value)) {
         return false;
      }
      for (double step : perfectRotSteps) {
         if (this.isMultipleOf(step, value)) {
            return true;
         }
      }
      return false;
   }

   private boolean isMultipleOf(double base, double value) {
      if (base == 0.0) {
         return Math.abs(value) <= 1.0E-10;
      }
      double ratio = value / base;
      return Math.abs(ratio - (double) Math.round(ratio)) <= 1.0E-10;
   }

   private float wrapDegrees(float degrees) {
      while (degrees > 180.0F) {
         degrees -= 360.0F;
      }
      while (degrees < -180.0F) {
         degrees += 360.0F;
      }
      return degrees;
   }

   private void sendIntermediateSlots(int fromSlot, int toSlot) {
      int distance = Math.abs(fromSlot - toSlot);
      if (distance > 1 && !this.isWrapAroundSlot(fromSlot, toSlot)) {
         int step = fromSlot > toSlot ? -1 : 1;
         for (int slot = fromSlot + step; slot != toSlot; slot += step) {
            if (slot < 0 || slot > 8) {
               continue;
            }
            NetworkUtils.sendPacketNoEvent(new ServerboundSetCarriedItemPacket(slot));
            this.log("Sent intermediate slot: " + slot);
         }
      }
   }

   private boolean isWrapAroundSlot(int fromSlot, int toSlot) {
      return fromSlot == 0 && toSlot == 8 || fromSlot == 8 && toSlot == 0;
   }

   public static float getPacketYRot(ServerboundMovePlayerPacket packet) {
      if (mc.gameMode == null) {
         return 0.0F;
      }
      Field yRotField = findField(packet.getClass(), "f_134121_");
      try {
         return yRotField.getFloat(packet);
      } catch (Exception var3) {
         FileManager.logger.error("Failed to get yrot field", var3);
         return 0.0F;
      }
   }

   public static float getPacketXRot(ServerboundMovePlayerPacket packet) {
      if (mc.gameMode == null) {
         return 0.0F;
      }
      Field xRotField = findField(packet.getClass(), "f_134122_");
      try {
         return xRotField.getFloat(packet);
      } catch (Exception var3) {
         FileManager.logger.error("Failed to get xrot field", var3);
         return 0.0F;
      }
   }

   private static Field findField(Class<?> clazz, String... fieldNames) {
      if (clazz == null || fieldNames == null || fieldNames.length == 0) {
         throw new IllegalArgumentException("Class and fieldNames must not be null or empty");
      }
      Exception failed = null;
      for (Class<?> currentClass = clazz; currentClass != null; currentClass = currentClass.getSuperclass()) {
         for (String fieldName : fieldNames) {
            if (fieldName != null) {
               try {
                  Field f = currentClass.getDeclaredField(fieldName);
                  f.setAccessible(true);
                  if ((f.getModifiers() & 16) != 0) {
                     unsafe.putInt(f, (long) unsafe.arrayBaseOffset(boolean[].class), f.getModifiers() & -17);
                  }
                  return f;
               } catch (Exception var9) {
                  failed = var9;
               }
            }
         }
      }
      throw new UnableToFindFieldException(failed);
   }

   public static void setPacketYRot(ServerboundMovePlayerPacket packet, float yRot) {
      if (mc.gameMode != null) {
         Field yRotField = findField(packet.getClass(), "f_134121_");
         try {
            yRotField.setFloat(packet, yRot);
         } catch (Exception var4) {
            FileManager.logger.error("Failed to set yrot field", var4);
         }
      }
   }

   public static void setPacketXRot(ServerboundMovePlayerPacket packet, float xRot) {
      if (mc.gameMode != null) {
         Field xRotField = findField(packet.getClass(), "f_134122_");
         try {
            xRotField.setFloat(packet, xRot);
         } catch (Exception var4) {
            FileManager.logger.error("Failed to set xrot field", var4);
         }
      }
   }

   static {
      try {
         Field field = Unsafe.class.getDeclaredField("theUnsafe");
         field.setAccessible(true);
         unsafe = (Unsafe) field.get(null);
      } catch (Exception var1) {
         throw new RuntimeException(var1);
      }
   }

   private static class UnableToFindFieldException extends RuntimeException {
      private static final long serialVersionUID = 1L;

      public UnableToFindFieldException(Exception e) {
         super(e);
      }
   }
}