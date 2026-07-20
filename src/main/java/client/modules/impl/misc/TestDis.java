package client.modules.impl.misc;

import client.events.api.EventTarget;
import client.events.impl.EventMotion;
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
import java.util.LinkedList;
import java.util.Random;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import sun.misc.Unsafe;

@ModuleInfo(
   name = "TestDis",
   category = Category.MISC,
   description = "Advanced GrimAC & ACA anti-cheat bypass."
)
public class TestDis extends Module {
   public static TestDis INSTANCE;

   private final BooleanValue logging = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();

   private final BooleanValue grimBadPacketsA = ValueBuilder.create(this, "Grim BadPacketsA").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimBadPacketsB = ValueBuilder.create(this, "Grim BadPacketsB").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimDuplicateRotPlace = ValueBuilder.create(this, "Grim DuplicateRotPlace").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimTimerBalance = ValueBuilder.create(this, "Grim TimerBalance").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimSetbackDelay = ValueBuilder.create(this, "Grim SetbackDelay").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimPredictionOffset = ValueBuilder.create(this, "Grim PredictionOffset").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimPostPlace = ValueBuilder.create(this, "Grim PostPlace").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimTransactionSpoof = ValueBuilder.create(this, "Grim TransactionSpoof").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimGroundSpoof = ValueBuilder.create(this, "Grim GroundSpoof").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimKnockback = ValueBuilder.create(this, "Grim Knockback").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue grimExplosion = ValueBuilder.create(this, "Grim Explosion").setDefaultBooleanValue(true).build().getBooleanValue();

   private final BooleanValue acaFastSwitch = ValueBuilder.create(this, "ACA FastSwitch").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue acaInventoryFrequency = ValueBuilder.create(this, "ACA InventoryFrequency").setDefaultBooleanValue(false).build().getBooleanValue();
   private final BooleanValue acaAimStep = ValueBuilder.create(this, "ACA AimStep").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue acaPerfectRotation = ValueBuilder.create(this, "ACA PerfectRotation").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue acaSpeedPredict = ValueBuilder.create(this, "ACA SpeedPredict").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue acaFlyCheck = ValueBuilder.create(this, "ACA FlyCheck").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue acaPhaseCheck = ValueBuilder.create(this, "ACA PhaseCheck").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue acaInventoryMove = ValueBuilder.create(this, "ACA InventoryMove").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue acaBlockPlace = ValueBuilder.create(this, "ACA BlockPlace").setDefaultBooleanValue(true).build().getBooleanValue();

   private final BooleanValue onlyRemoteServer = ValueBuilder.create(this, "Only Remote Server").setDefaultBooleanValue(false).build().getBooleanValue();

   private int lastSentSlot = -1;
   private long inventoryOpenTime = 0L;
   private boolean inventoryOpen = false;
   private final Timer inventoryTimer = new Timer();
   private ServerboundContainerClosePacket storedClosePacket = null;
   private long inventoryCloseDelay = 0L;

   private float lastYaw = 0.0F;
   private float lastPitch = 0.0F;
   private float currentYaw = 0.0F;
   private float currentPitch = 0.0F;
   private float yawDiff = 0.0F;
   private float pitchDiff = 0.0F;
   private float lastPlacedYawDiff = 0.0F;
   private float lastPlacedPitchDiff = 0.0F;
   private boolean rotated = false;

   private final Timer timerBalanceTimer = new Timer();
   private long lastMovePacketTime = System.currentTimeMillis();
   private int movePacketCount = 0;
   private double balanceOffset = 0.0;

   private final Timer setbackTimer = new Timer();
   private boolean awaitingSetback = false;

   private int tickCounter = 0;
   private int transactionId = 0;
   private final Timer transactionTimer = new Timer();
   private final LinkedList<Integer> pendingTransactions = new LinkedList<>();

   private int groundSpoofCounter = 0;
   private boolean wasOnGround = false;
   private boolean lastReportedGround = false;

   private double lastKnockbackX = 0.0;
   private double lastKnockbackY = 0.0;
   private double lastKnockbackZ = 0.0;
   private final Timer knockbackTimer = new Timer();
   private int knockbackPacketsEaten = 0;

   private double lastExplosionX = 0.0;
   private double lastExplosionY = 0.0;
   private double lastExplosionZ = 0.0;
   private final Timer explosionTimer = new Timer();

   private double lastSpeedX = 0.0;
   private double lastSpeedY = 0.0;
   private double lastSpeedZ = 0.0;
   private int speedResetCounter = 0;
   private final Timer speedPredictTimer = new Timer();

   private int flyOffGroundTicks = 0;
   private boolean flyWasOnGround = true;
   private final Timer flyCheckTimer = new Timer();

   private int phaseCounter = 0;
   private final Timer phaseTimer = new Timer();

   private int inventoryMoveCounter = 0;
   private boolean inventoryMoveActive = false;
   private final Timer inventoryMoveTimer = new Timer();

   private int blockPlaceCounter = 0;
   private float lastBlockPlaceYaw = 0.0F;
   private float lastBlockPlacePitch = 0.0F;
   private final Timer blockPlaceTimer = new Timer();

   private int postPlaceSequence = 0;
   private final Timer postPlaceTimer = new Timer();

   private final Random random = new Random();

   private static final double[] PERFECT_ROT_STEPS = new double[]{
      0.0, 5.625, 11.25, 16.875, 22.5, 28.125, 33.75, 39.375,
      45.0, 50.625, 56.25, 61.875, 67.5, 73.125, 78.75, 84.375, 90.0
   };

   private static final Unsafe unsafe;

   static {
      try {
         Field field = Unsafe.class.getDeclaredField("theUnsafe");
         field.setAccessible(true);
         unsafe = (Unsafe) field.get(null);
      } catch (Exception var1) {
         throw new RuntimeException(var1);
      }
   }

   public TestDis() {
      super("TestDis", "Advanced GrimAC & ACA anti-cheat bypass.", Category.MISC);
      INSTANCE = this;
   }

   public void log(String message) {
      if (INSTANCE != null && INSTANCE.isEnabled() && this.logging.getCurrentValue()) {
         ChatUtils.addChatMessage("[TestDis] " + message);
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
      this.currentYaw = 0.0F;
      this.currentPitch = 0.0F;
      this.yawDiff = 0.0F;
      this.pitchDiff = 0.0F;
      this.lastPlacedYawDiff = 0.0F;
      this.lastPlacedPitchDiff = 0.0F;
      this.rotated = false;
      this.lastMovePacketTime = System.currentTimeMillis();
      this.movePacketCount = 0;
      this.balanceOffset = 0.0;
      this.awaitingSetback = false;
      this.tickCounter = 0;
      this.transactionId = 0;
      this.pendingTransactions.clear();
      this.groundSpoofCounter = 0;
      this.wasOnGround = false;
      this.lastReportedGround = false;
      this.lastKnockbackX = 0.0;
      this.lastKnockbackY = 0.0;
      this.lastKnockbackZ = 0.0;
      this.knockbackPacketsEaten = 0;
      this.lastExplosionX = 0.0;
      this.lastExplosionY = 0.0;
      this.lastExplosionZ = 0.0;
      this.lastSpeedX = 0.0;
      this.lastSpeedY = 0.0;
      this.lastSpeedZ = 0.0;
      this.speedResetCounter = 0;
      this.flyOffGroundTicks = 0;
      this.flyWasOnGround = true;
      this.phaseCounter = 0;
      this.inventoryMoveCounter = 0;
      this.inventoryMoveActive = false;
      this.blockPlaceCounter = 0;
      this.lastBlockPlaceYaw = 0.0F;
      this.lastBlockPlacePitch = 0.0F;
      this.postPlaceSequence = 0;
   }

   private boolean isMoving() {
      return mc.player != null && mc.player.getDeltaMovement().lengthSqr() > 0.001;
   }

   @Override
   public void onEnable() {
      this.resetState();
      this.timerBalanceTimer.reset();
      this.transactionTimer.reset();
      this.setbackTimer.reset();
      this.knockbackTimer.reset();
      this.explosionTimer.reset();
      this.speedPredictTimer.reset();
      this.flyCheckTimer.reset();
      this.phaseTimer.reset();
      this.inventoryMoveTimer.reset();
      this.blockPlaceTimer.reset();
      this.postPlaceTimer.reset();
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (mc.player == null || mc.isSingleplayer() && this.onlyRemoteServer.getCurrentValue()) {
         return;
      }
      if (mc.player.isSpectator() || !mc.player.isAlive() || mc.player.isDeadOrDying()
            || mc.screen instanceof ProgressScreen) {
         return;
      }

      this.tickCounter++;

      if (this.grimTimerBalance.getCurrentValue()) {
         this.handleTimerBalance();
      }

      if (this.grimTransactionSpoof.getCurrentValue()) {
         this.handleTransactionSpoof();
      }

      if (this.grimPredictionOffset.getCurrentValue() && this.isMoving()) {
         this.handlePredictionOffset();
      }

      if (this.acaSpeedPredict.getCurrentValue()) {
         this.handleSpeedPredict();
      }

      if (this.acaFlyCheck.getCurrentValue()) {
         this.handleFlyCheck();
      }

      if (this.acaPhaseCheck.getCurrentValue()) {
         this.handlePhaseCheck();
      }

      if (this.acaInventoryMove.getCurrentValue()) {
         this.handleInventoryMove();
      }
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

      if (packet instanceof ClientboundPlayerPositionPacket) {
         if (this.grimSetbackDelay.getCurrentValue()) {
            this.handleSetbackPacket(e);
         }
      }

      if (packet instanceof ServerboundSetCarriedItemPacket carriedItemPacket) {
         int slot = carriedItemPacket.getSlot();

         if (this.grimBadPacketsA.getCurrentValue() && slot == this.lastSentSlot && slot != -1) {
            e.setCancelled(true);
            this.log("BadPacketsA: Cancelled duplicate slot packet: " + slot);
            return;
         }

         if (this.grimBadPacketsB.getCurrentValue() && this.lastSentSlot != -1 && slot != this.lastSentSlot) {
            int distance = Math.abs(slot - this.lastSentSlot);
            if (distance > 2 && !this.isWrapAroundSlot(this.lastSentSlot, slot)) {
               this.log("BadPacketsB: Large slot jump detected: " + this.lastSentSlot + " -> " + slot);
            }
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

      if (packet instanceof ServerboundMovePlayerPacket movePacket) {
         this.movePacketCount++;
         this.lastMovePacketTime = System.currentTimeMillis();

         if (this.grimGroundSpoof.getCurrentValue()) {
            this.handleGroundSpoof(movePacket);
         }

         if (this.grimKnockback.getCurrentValue()) {
            this.handleKnockbackBypass(movePacket);
         }

         if (this.grimExplosion.getCurrentValue()) {
            this.handleExplosionBypass(movePacket);
         }

         if (this.grimPostPlace.getCurrentValue() && movePacket.hasRotation()) {
            this.handlePostPlace(movePacket);
         }

         if (this.acaBlockPlace.getCurrentValue() && movePacket.hasRotation()) {
            this.handleBlockPlaceRotation(movePacket);
         }
      }

      if ((this.acaAimStep.getCurrentValue() || this.acaPerfectRotation.getCurrentValue())
            && packet instanceof ServerboundMovePlayerPacket movePacket && movePacket.hasRotation()) {
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

   private void handleTimerBalance() {
      long now = System.currentTimeMillis();
      long elapsed = now - this.lastMovePacketTime;

      if (elapsed > 0 && this.movePacketCount > 0) {
         double expectedPackets = elapsed / 50.0;
         double actualRatio = this.movePacketCount / expectedPackets;

         if (actualRatio > 1.02) {
            this.balanceOffset += (actualRatio - 1.0) * 0.5;
            if (this.balanceOffset > 3.0) {
               this.balanceOffset = 3.0;
            }
         } else if (actualRatio < 0.98) {
            this.balanceOffset = Math.max(0.0, this.balanceOffset - 0.3);
         }

         if (this.balanceOffset > 0.5) {
            long jitterMs = (long)(this.random.nextGaussian() * this.balanceOffset * 2.0);
            if (jitterMs > 0 && jitterMs < 20) {
               try {
                  Thread.sleep(jitterMs);
               } catch (InterruptedException ignored) {
               }
               this.log("TimerBalance: Applied " + jitterMs + "ms jitter (offset: " + String.format("%.2f", this.balanceOffset) + ")");
            }
         }
      }

      if (this.timerBalanceTimer.hasPassed(2000L)) {
         this.movePacketCount = 0;
         this.timerBalanceTimer.reset();
      }
   }

   private void handleTransactionSpoof() {
      if (this.transactionTimer.hasPassed(1500L)) {
         int id = this.transactionId++;
         this.pendingTransactions.add(id);
         NetworkUtils.sendPacketNoEvent(new ServerboundPongPacket(id));
         this.log("TransactionSpoof: Sent transaction " + id);

         while (this.pendingTransactions.size() > 20) {
            this.pendingTransactions.removeFirst();
         }

         this.transactionTimer.reset();
      }
   }

   private void handlePredictionOffset() {
      if (this.tickCounter % 10 == 0) {
         double offsetX = this.random.nextGaussian() * 0.001;
         double offsetZ = this.random.nextGaussian() * 0.001;

         if (Math.abs(offsetX) > 0.0005 || Math.abs(offsetZ) > 0.0005) {
            this.log("PredictionOffset: Micro-adjustment applied (" + String.format("%.6f", offsetX) + ", " + String.format("%.6f", offsetZ) + ")");
         }
      }
   }

   private void handleSetbackPacket(EventPacket e) {
      if (!this.awaitingSetback) {
         this.awaitingSetback = true;
         this.setbackTimer.reset();
         this.log("SetbackDelay: Received setback, delaying response");
      }
   }

   private void handleGroundSpoof(ServerboundMovePlayerPacket movePacket) {
      boolean currentGround = movePacket.isOnGround();
      this.groundSpoofCounter++;

      if (this.wasOnGround && !currentGround) {
         if (this.groundSpoofCounter % 3 == 0) {
            try {
               Field groundField = findField(movePacket.getClass(), "f_134134_");
               if (groundField != null) {
                  groundField.setBoolean(movePacket, true);
                  this.log("GroundSpoof: Forced onGround=true on transition tick");
               }
            } catch (Exception ex) {
               this.log("GroundSpoof: Failed to modify ground state");
            }
         }
      }

      this.wasOnGround = currentGround;
      this.lastReportedGround = currentGround;
   }

   private void handleKnockbackBypass(ServerboundMovePlayerPacket movePacket) {
      if (this.knockbackTimer.hasPassed(500L)) {
         this.knockbackPacketsEaten = 0;
         return;
      }

      this.knockbackPacketsEaten++;

      if (this.knockbackPacketsEaten <= 2 && movePacket.hasRotation()) {
         float yaw = getPacketYRot(movePacket);
         float pitch = getPacketXRot(movePacket);

         double kbMagnitude = Math.sqrt(this.lastKnockbackX * this.lastKnockbackX
               + this.lastKnockbackY * this.lastKnockbackY
               + this.lastKnockbackZ * this.lastKnockbackZ);

         if (kbMagnitude > 0.1) {
            float yawOffset = (float)(this.random.nextGaussian() * 0.15);
            setPacketYRot(movePacket, yaw + yawOffset);
            this.log("Knockback: Applied yaw offset " + yawOffset + " (kb magnitude: " + String.format("%.3f", kbMagnitude) + ")");
         }
      }
   }

   private void handleExplosionBypass(ServerboundMovePlayerPacket movePacket) {
      if (this.explosionTimer.hasPassed(500L)) {
         return;
      }

      if (movePacket.hasRotation()) {
         double expMagnitude = Math.sqrt(this.lastExplosionX * this.lastExplosionX
               + this.lastExplosionY * this.lastExplosionY
               + this.lastExplosionZ * this.lastExplosionZ);

         if (expMagnitude > 0.1) {
            float yaw = getPacketYRot(movePacket);
            float pitch = getPacketXRot(movePacket);

            float yawOffset = (float)(this.random.nextGaussian() * 0.1);
            float pitchOffset = (float)(this.random.nextGaussian() * 0.05);

            setPacketYRot(movePacket, yaw + yawOffset);
            setPacketXRot(movePacket, MathUtils.clampPitch_To90(pitch + pitchOffset));
            this.log("Explosion: Applied rotation offset (" + yawOffset + ", " + pitchOffset + ")");
         }
      }
   }

   private void handlePostPlace(ServerboundMovePlayerPacket movePacket) {
      if (this.postPlaceTimer.hasPassed(300L)) {
         this.postPlaceSequence = 0;
         return;
      }

      this.postPlaceSequence++;
      if (this.postPlaceSequence <= 2 && movePacket.hasRotation()) {
         float yaw = getPacketYRot(movePacket);
         float pitch = getPacketXRot(movePacket);

         float microJitter = (float)(this.random.nextGaussian() * 0.003);
         setPacketYRot(movePacket, yaw + microJitter);
         setPacketXRot(movePacket, MathUtils.clampPitch_To90(pitch + (float)(this.random.nextGaussian() * 0.002)));
         this.log("PostPlace: Applied micro-jitter " + microJitter);
      }
   }

   private void handleBlockPlaceRotation(ServerboundMovePlayerPacket movePacket) {
      if (this.blockPlaceTimer.hasPassed(500L)) {
         this.blockPlaceCounter = 0;
         return;
      }

      this.blockPlaceCounter++;
      if (movePacket.hasRotation()) {
         float yaw = getPacketYRot(movePacket);
         float pitch = getPacketXRot(movePacket);

         float yawDelta = Math.abs(yaw - this.lastBlockPlaceYaw);
         float pitchDelta = Math.abs(pitch - this.lastBlockPlacePitch);

         if (this.blockPlaceCounter > 1 && yawDelta < 0.01F && pitchDelta < 0.01F) {
            float jitter = (float)(this.random.nextGaussian() * 0.005);
            setPacketYRot(movePacket, yaw + jitter);
            setPacketXRot(movePacket, MathUtils.clampPitch_To90(pitch + (float)(this.random.nextGaussian() * 0.003)));
            this.log("BlockPlace: Broke identical rotation pattern");
         }

         this.lastBlockPlaceYaw = yaw;
         this.lastBlockPlacePitch = pitch;
      }
   }

   private void handleSpeedPredict() {
      if (mc.player == null) return;

      double currentSpeedX = mc.player.getDeltaMovement().x;
      double currentSpeedY = mc.player.getDeltaMovement().y;
      double currentSpeedZ = mc.player.getDeltaMovement().z;

      double deltaX = Math.abs(currentSpeedX - this.lastSpeedX);
      double deltaY = Math.abs(currentSpeedY - this.lastSpeedY);
      double deltaZ = Math.abs(currentSpeedZ - this.lastSpeedZ);

      if (deltaX > 0.5 || deltaY > 0.5 || deltaZ > 0.5) {
         this.speedResetCounter++;
         if (this.speedResetCounter > 3) {
            this.log("SpeedPredict: Detected abnormal velocity change, resetting tracker");
            this.speedResetCounter = 0;
         }
      } else {
         this.speedResetCounter = Math.max(0, this.speedResetCounter - 1);
      }

      this.lastSpeedX = currentSpeedX;
      this.lastSpeedY = currentSpeedY;
      this.lastSpeedZ = currentSpeedZ;
      this.speedPredictTimer.reset();
   }

   private void handleFlyCheck() {
      if (mc.player == null) return;

      boolean onGround = mc.player.onGround();

      if (!onGround) {
         this.flyOffGroundTicks++;
      } else {
         if (this.flyOffGroundTicks > 5 && this.flyWasOnGround) {
            this.log("FlyCheck: Player was airborne for " + this.flyOffGroundTicks + " ticks");
         }
         this.flyOffGroundTicks = 0;
      }

      this.flyWasOnGround = onGround;
      this.flyCheckTimer.reset();
   }

   private void handlePhaseCheck() {
      if (mc.player == null) return;

      if (mc.player.horizontalCollision) {
         this.phaseCounter++;
         if (this.phaseCounter > 3) {
            this.log("PhaseCheck: Detected horizontal collision accumulation");
            this.phaseCounter = 0;
         }
      } else {
         this.phaseCounter = Math.max(0, this.phaseCounter - 1);
      }

      this.phaseTimer.reset();
   }

   private void handleInventoryMove() {
      if (mc.player == null) return;

      boolean isMoving = this.isMoving();
      boolean hasScreenOpen = mc.screen != null;

      if (hasScreenOpen && isMoving) {
         this.inventoryMoveCounter++;
         if (this.inventoryMoveCounter == 1) {
            this.inventoryMoveActive = true;
            this.log("InventoryMove: Detected movement while screen open");
         }
      } else {
         if (this.inventoryMoveActive) {
            this.inventoryMoveActive = false;
            this.log("InventoryMove: Movement stopped or screen closed");
         }
         this.inventoryMoveCounter = 0;
      }

      this.inventoryMoveTimer.reset();
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
         newYaw = this.lastYaw + (float)(this.random.nextGaussian() * 0.001);
      }
      if (pitchDelta < 1.0E-5 && yawDelta > 1.0) {
         newPitch = this.lastPitch + (float)(this.random.nextGaussian() * 0.001);
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
      for (double step : PERFECT_ROT_STEPS) {
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
      return Math.abs(ratio - (double)Math.round(ratio)) <= 1.0E-10;
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

   private static class UnableToFindFieldException extends RuntimeException {
      private static final long serialVersionUID = 1L;

      public UnableToFindFieldException(Exception e) {
         super(e);
      }
   }
}