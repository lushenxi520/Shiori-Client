package client.modules.impl.combat;

import client.Shiori;
import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventRunTicks;
import client.events.impl.EventSprint;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.modules.impl.move.Scaffold;
import client.modules.impl.move.Stuck;
import client.utils.Vector2f;
import client.utils.animation.Timer;
import client.utils.rotation.RotationManager;
import client.utils.rotation.RotationUtils;
import client.values.ValueBuilder;
import client.values.impl.FloatValue;
import inject.mixin.O.accessors.MultiPlayerGameModeAccessor;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

@ModuleInfo(
   name = "AutoThrow",
   description = "Automatically throws projectiles at enemies",
   category = Category.COMBAT
)
public class AutoThrow extends Module {
   public Vector2f targetRotation;
   public int ticksUntilThrow;
   private int savedSlot = -1;

   private final FloatValue minDistance = ValueBuilder.create(this, "Min Distance")
      .setDefaultFloatValue(5.0F)
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(30.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();

   private final FloatValue maxDistance = ValueBuilder.create(this, "Max Distance")
      .setDefaultFloatValue(10.0F)
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(30.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();

   private final FloatValue throwDelay = ValueBuilder.create(this, "Delay")
      .setDefaultFloatValue(500.0F)
      .setMinFloatValue(50.0F)
      .setMaxFloatValue(2000.0F)
      .setFloatStep(50.0F)
      .build()
      .getFloatValue();

   private final Timer throwTimer = new Timer();

   @Override
   public void onEnable() {
      this.targetRotation = null;
      this.ticksUntilThrow = 0;
      this.savedSlot = -1;
      this.throwTimer.reset();
   }

   @Override
   public void onDisable() {
      this.targetRotation = null;
      this.ticksUntilThrow = 0;
      this.savedSlot = -1;
   }

   @EventTarget
   public void onSprint(EventSprint sprintEvent) {
      if (mc.player == null || mc.level == null || mc.gameMode == null || mc.getConnection() == null) {
         return;
      }

      Scaffold scaffold = (Scaffold) Shiori.getInstance().getModuleManager().getModule(Scaffold.class);
      Stuck stuck = (Stuck) Shiori.getInstance().getModuleManager().getModule(Stuck.class);

      if ((scaffold != null && scaffold.isEnabled()) || (stuck != null && stuck.isEnabled()) || mc.player.isUsingItem() || mc.screen != null) {
         this.ticksUntilThrow = 0;
         this.targetRotation = null;
         return;
      }

      if (this.ticksUntilThrow <= 0) {
         this.targetRotation = null;
      }

      int projectileSlot = -1;
      for (int slot = 0; slot < 9; ++slot) {
         ItemStack itemStack = mc.player.getInventory().getItem(slot);
         if (itemStack.isEmpty() || !(itemStack.getItem() instanceof EggItem) && !(itemStack.getItem() instanceof SnowballItem)) {
            continue;
         }
         projectileSlot = slot;
         break;
      }

      if (mc.player.isUsingItem() || mc.player.getMainHandItem().getItem() instanceof BowItem || mc.player.getMainHandItem().getItem() instanceof CrossbowItem) {
         return;
      }

      if (projectileSlot == -1) {
         return;
      }

      if (--this.ticksUntilThrow == 0) {
         int slot = mc.player.getInventory().selected;
         boolean shouldSwap = slot != projectileSlot;

         if (shouldSwap) {
            mc.player.getInventory().selected = projectileSlot;
            ((MultiPlayerGameModeAccessor) mc.gameMode).invokeEnsureHasSentCarriedItem();
            this.savedSlot = slot;
         }

         float prevYaw = mc.player.getYRot();
         float prevPitch = mc.player.getXRot();

         if (RotationManager.rotations != null && RotationManager.active) {
            mc.player.setYRot(RotationManager.rotations.x);
            mc.player.setXRot(RotationManager.rotations.y);
         }

         try {
            if (!(mc.player.getMainHandItem().getItem() instanceof EggItem) && !(mc.player.getMainHandItem().getItem() instanceof SnowballItem)) {
               return;
            }
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         } finally {
            mc.player.setYRot(prevYaw);
            mc.player.setXRot(prevPitch);
         }
      } else {
         if (!this.findTarget().isPresent() || !this.throwTimer.hasPassed((long) this.throwDelay.getCurrentValue()) || (stuck != null && stuck.isEnabled())) {
            return;
         }

         this.targetRotation = this.calculateThrowRotation(this.findTarget().get());
         if (this.targetRotation != null) {
            RotationManager.setRotations(this.targetRotation);
            RotationManager.active = true;
            this.ticksUntilThrow = 2;
         }
         this.throwTimer.reset();
      }
   }

   @EventTarget
   public void onTick(EventRunTicks tickEvent) {
      if (tickEvent.getType() != EventType.POST) {
         return;
      }
      if (mc.player == null) {
         return;
      }
      if (this.savedSlot != -1) {
         mc.player.getInventory().selected = this.savedSlot;
         this.savedSlot = -1;
         RotationManager.active = false;
      }
   }

   private Vector2f calculateThrowRotation(Entity entity) {
      float projectileSpeed = 1.5F;
      float gravity = 0.03F;

      double predictX = entity.getX();
      double predictY = entity.getY() + (double) entity.getBbHeight() * 0.8;
      double predictZ = entity.getZ();

      double velX = entity.getX() - entity.xOld;
      double velY = entity.getY() - entity.yOld;
      double velZ = entity.getZ() - entity.zOld;

      for (int i = 0; i < 3; ++i) {
         double dx = predictX - mc.player.getX();
         double dy = predictY - (mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose()));
         double dz = predictZ - mc.player.getZ();
         double horizDist = Math.sqrt(dx * dx + dz * dz);
         float travelTicks = (float) (horizDist / (double) (projectileSpeed * 0.4F));
         predictX = entity.getX() + velX * (double) travelTicks;
         predictY = entity.getY() + (double) entity.getBbHeight() * 0.8 + velY * (double) travelTicks;
         predictZ = entity.getZ() + velZ * (double) travelTicks;
      }

      double dx = predictX - mc.player.getX();
      double dy = predictY - (mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose()));
      double dz = predictZ - mc.player.getZ();
      double horizDist = Math.sqrt(dx * dx + dz * dz);

      float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
      float pitch = -RotationUtils.ballisticPitch((float) horizDist, (float) dy, projectileSpeed, gravity);

      return new Vector2f(yaw, pitch);
   }

   private Optional<? extends Player> findTarget() {
      if (mc.player == null || mc.level == null) {
         return Optional.empty();
      }

      Killaura killaura = (Killaura) Shiori.getInstance().getModuleManager().getModule(Killaura.class);

      return mc.level.players().stream()
         .filter(player -> player != mc.player)
         .filter(player -> killaura.isValidTarget(player))
         .filter(player -> {
            double dist = this.getDistanceTo(player);
            return dist >= this.minDistance.getCurrentValue() && dist <= this.maxDistance.getCurrentValue();
         })
         .filter(this::hasLineOfSight)
         .filter(player -> !this.isInvisibleAlly(player))
         .min(Comparator.comparingDouble(player -> mc.player.distanceTo(player)));
   }

   private double getDistanceTo(Entity entity) {
      double dx = mc.player.getX() - entity.getX();
      double dz = mc.player.getZ() - entity.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private boolean hasLineOfSight(Entity entity) {
      if (mc.player == null || mc.level == null) {
         return false;
      }
      Vec3 eyePos = new Vec3(mc.player.getX(), mc.player.getY() + (double) mc.player.getEyeHeight(), mc.player.getZ());
      Vec3 targetPos = new Vec3(entity.getX(), entity.getY() + (double) entity.getEyeHeight(), entity.getZ());
      BlockHitResult hit = mc.level.clip(new ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
      return hit.getType() == HitResult.Type.MISS;
   }

   private boolean isInvisibleAlly(Entity entity) {
      if (!entity.isInvisible()) {
         return false;
      }
      if (mc.player.isSpectator()) {
         return false;
      }
      Team team = entity.getTeam();
      return team == null || mc.player.getTeam() != team || !team.isAlliedTo(mc.player.getTeam());
   }
}