package client.modules.impl.combat;

import client.Shiori;
import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventClick;
import client.events.impl.EventRender;
import client.events.impl.EventRender2D;
import client.events.impl.EventRespawn;
import client.events.impl.EventRunTicks;
import client.events.impl.EventSprint;
import client.events.impl.EventShader;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.modules.impl.misc.Teams;
import client.modules.impl.move.Blink;
import client.modules.impl.move.Stuck;
import client.modules.impl.render.HUD;
import client.utils.BlinkingPlayer;
import client.utils.ChatUtils;
import client.utils.FriendManager;
import client.utils.InventoryUtils;
import client.utils.NetworkUtils;
import client.utils.RenderUtils;
import client.utils.StencilUtils;
import client.utils.Vector2f;
import client.utils.renderer.Fonts;
import client.utils.rotation.RotationManager;
import client.utils.rotation.RotationUtils;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;
import client.values.impl.FloatValue;
import client.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
   name = "KillAura",
   description = "Automatically attacks entities",
   category = Category.COMBAT
)
public class Killaura extends Module {
   private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
   private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
   public static Entity target;
   public static Entity aimingTarget;
   public static List<Entity> targets = new ArrayList<>();
   public static Vector2f rotation;
   BooleanValue targetEsp = ValueBuilder.create(this, "Target ESP").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue multi = ValueBuilder.create(this, "Multi Attack").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue infSwitch = ValueBuilder.create(this, "Infinity Switch").setDefaultBooleanValue(false).build().getBooleanValue();
   ModeValue particles = ValueBuilder.create(this, "Particles").setModes("Off", "More Particles", "Better Particles").setDefaultModeIndex(0).build().getModeValue();
   public BooleanValue keepSprint = ValueBuilder.create(this, "Keep Sprint").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue onlyHurtTime = ValueBuilder.create(this, "Only HurtTime").setDefaultBooleanValue(false).build().getBooleanValue();
   FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(6.0F)
      .build()
      .getFloatValue();
   FloatValue maxAps = ValueBuilder.create(this, "Max APS")
      .setDefaultFloatValue(12.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .build()
      .getFloatValue();
   FloatValue minAps = ValueBuilder.create(this, "Min APS")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .build()
      .getFloatValue();
   FloatValue switchSize = ValueBuilder.create(this, "Switch Size")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> !this.infSwitch.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue switchAttackTimes = ValueBuilder.create(this, "Switch Delay (Attack Times)")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   FloatValue fov = ValueBuilder.create(this, "FoV")
      .setDefaultFloatValue(360.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(10.0F)
      .setMaxFloatValue(360.0F)
      .build()
      .getFloatValue();
   FloatValue hurtTime = ValueBuilder.create(this, "Hurt Time")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "FoV", "Range", "None").build().getModeValue();
   ModeValue delayMode = ValueBuilder.create(this, "Delay Mode").setModes("1.8", "1.9").build().getModeValue();
   RotationUtils.Data lastRotationData;
   RotationUtils.Data rotationData;
   int attackTimes = 0;
   float attacks = 0.0F;
   private int index;
   public int sprintTickCounter;
   private int sprintCounter;
   private Vector4f blurMatrix;

   @EventTarget
   public void onRender(EventRender e) {
      if (this.targetEsp.getCurrentValue()) {
         PoseStack stack = e.getPMatrixStack();
         float partialTicks = e.getRenderPartialTicks();
         stack.pushPose();
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL11.glEnable(2848);
         RenderSystem.setShader(GameRenderer::getPositionShader);
         RenderUtils.applyRegionalRenderOffset(stack);

         for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
               float[] color = target == living ? targetColorRed : targetColorGreen;
               stack.pushPose();
               RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
               double motionX = entity.getX() - entity.xo;
               double motionY = entity.getY() - entity.yo;
               double motionZ = entity.getZ() - entity.zo;
               AABB boundingBox = entity.getBoundingBox()
                  .move(-motionX, -motionY, -motionZ)
                  .move((double)partialTicks * motionX, (double)partialTicks * motionY, (double)partialTicks * motionZ);
               RenderUtils.drawSolidBox(boundingBox, stack);
               stack.popPose();
            }
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glDisable(3042);
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GL11.glDisable(2848);
         stack.popPose();
      }
   }

   @Override
   public void onEnable() {
      rotation = null;
      this.index = 0;
      target = null;
      aimingTarget = null;
      targets.clear();
   }

   @Override
   public void onDisable() {
      target = null;
      aimingTarget = null;
      this.sprintTickCounter = 0;
      this.sprintCounter = 0;
      super.onDisable();
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      target = null;
      aimingTarget = null;
      this.sprintTickCounter = 0;
      this.sprintCounter = 0;
      this.toggle();
   }

   @EventTarget
   public void onSprint(EventSprint e) {
      if (this.keepSprint.getCurrentValue()) {
         ++this.sprintTickCounter;
         if (this.sprintTickCounter % 2 == 0 && mc.player != null) {
            mc.player.setSprinting(false);
         }
      }
   }

   @EventTarget
   public void onMotion(EventRunTicks event) {
      if (event.getType() == EventType.PRE && mc.player != null) {
         if (mc.screen instanceof AbstractContainerScreen
            || Shiori.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
            || InventoryUtils.shouldDisableFeatures()) {
            target = null;
            aimingTarget = null;
            this.rotationData = null;
            rotation = null;
            this.lastRotationData = null;
            targets.clear();
            this.sprintTickCounter = 0;
            this.sprintCounter = 0;
            return;
         }

         boolean isSwitch = this.switchSize.getCurrentValue() > 1.0F;
         this.setSuffix(this.multi.getCurrentValue() ? "Multi" : (isSwitch ? "Switch" : "Single"));
         this.updateAttackTargets();
         aimingTarget = this.shouldPreAim();
         this.lastRotationData = this.rotationData;
         this.rotationData = null;
         if (aimingTarget != null) {
            this.rotationData = RotationUtils.getRotationDataToEntity(aimingTarget);
            if (this.rotationData.getRotation() != null) {
               rotation = this.rotationData.getRotation();
            } else {
               rotation = null;
            }
         }

         if (targets.isEmpty()) {
            target = null;
            return;
         }

         if (this.index > targets.size() - 1) {
            this.index = 0;
         }

         if (targets.size() > 1
            && ((float)this.attackTimes >= this.switchAttackTimes.getCurrentValue() || this.rotationData != null && this.rotationData.getDistance() > 3.0)) {
            this.attackTimes = 0;

            for (int i = 0; i < targets.size(); i++) {
               this.index++;
               if (this.index > targets.size() - 1) {
                  this.index = 0;
               }

               Entity nextTarget = targets.get(this.index);
               RotationUtils.Data data = RotationUtils.getRotationDataToEntity(nextTarget);
               if (data.getDistance() < 3.0) {
                  break;
               }
            }
         }

         if (this.index > targets.size() - 1 || !isSwitch) {
            this.index = 0;
         }

         target = targets.get(this.index);
         if (this.delayMode.isCurrentMode("1.8")) {
            float maxApsVal = this.keepSprint.getCurrentValue() ? this.maxAps.getCurrentValue() * 2.0F : this.maxAps.getCurrentValue();
            float minApsVal = this.keepSprint.getCurrentValue() ? this.minAps.getCurrentValue() * 2.0F : this.minAps.getCurrentValue();
            this.attacks = this.attacks + (float)(Math.random() * (double)(maxApsVal - minApsVal) + (double)minApsVal) / 20.0F;
         } else if (this.sprintCounter > 0) {
            this.sprintCounter--;
         } else if (mc.player.getAttackStrengthScale(0.0F) >= 0.9F) {
            this.doAttack();
         }
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (mc.player.getUseItem().isEmpty()
         && mc.screen == null
         && Shiori.skipTasks.isEmpty()
         && !NetworkUtils.isServerLag()
         && !Shiori.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
         while (this.attacks >= 1.0F) {
            this.doAttack();
            this.attacks--;
         }
      }
   }

   public Entity shouldPreAim() {
      Entity target = Killaura.target;
      if (target == null) {
         List<Entity> aimTargets = this.getTargets();
         if (!aimTargets.isEmpty()) {
            target = aimTargets.get(0);
         }
      }

      return target;
   }

   public void doAttack() {
      if (!targets.isEmpty()) {
         HitResult hitResult = mc.hitResult;
         if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            if (AntiBots.isBot(result.getEntity())) {
               ChatUtils.addChatMessage("Attacking Bot!");
               return;
            }
         }

         if (this.multi.getCurrentValue()) {
            int attacked = 0;

            for (Entity entity : targets) {
               if (RotationUtils.getDistance(entity, mc.player.getEyePosition(), RotationManager.rotations) < 3.0) {
                  this.attackEntity(entity);
                  if (++attacked >= 2) {
                     break;
                  }
               }
            }
         } else if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            this.attackEntity(result.getEntity());
         }
      }
   }

   public void updateAttackTargets() {
      targets = this.getTargets();
   }

   public boolean isValidTarget(Entity entity) {
      if (entity == mc.player) {
         return false;
      } else if (entity instanceof LivingEntity living) {
         if (living instanceof BlinkingPlayer) {
            return false;
         } else {
            AntiBots module = (AntiBots)Shiori.getInstance().getModuleManager().getModule(AntiBots.class);
            if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
               if (Teams.isSameTeam(living)) {
                  return false;
               } else if (FriendManager.isFriend(living)) {
                  return false;
               } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                  return false;
               } else if (entity instanceof ArmorStand) {
                  return false;
               } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                  return false;
               } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                  return false;
               } else if (!(entity instanceof Player) || !((double)entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                  if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                     && !this.attackMobs.getCurrentValue()) {
                     return false;
                  } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                     return false;
                  } else {
                     return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean isValidAttack(Entity entity) {
      if (!this.isValidTarget(entity)) {
         return false;
      } else if (entity instanceof LivingEntity && (float)((LivingEntity)entity).hurtTime > this.hurtTime.getCurrentValue()) {
         return false;
      } else {
         Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
         return closestPoint.distanceTo(mc.player.getEyePosition()) > (double)this.aimRange.getCurrentValue()
            ? false
            : RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
      }
   }

   public void attackEntity(Entity entity) {
      if (this.onlyHurtTime.getCurrentValue() && entity instanceof LivingEntity le && le.hurtTime > 0) {
         return;
      }
      if (this.keepSprint.getCurrentValue() && this.sprintTickCounter % 2 != 0) {
         return;
      }
      this.attackTimes++;
      float currentYaw = mc.player.getYRot();
      float currentPitch = mc.player.getXRot();
      mc.player.setYRot(RotationManager.rotations.x);
      mc.player.setXRot(RotationManager.rotations.y);

      mc.gameMode.attack(mc.player, entity);
      mc.player.swing(InteractionHand.MAIN_HAND);
      if (this.particles.isCurrentMode("More Particles")) {
         mc.player.magicCrit(entity);
         mc.player.crit(entity);
      } else if (this.particles.isCurrentMode("Better Particles")) {
         if (!(entity instanceof LivingEntity) || ((LivingEntity) entity).hurtTime == 0) {
            mc.player.magicCrit(entity);
            mc.player.crit(entity);
         }
      }

      mc.player.setYRot(currentYaw);
      mc.player.setXRot(currentPitch);

      if (this.delayMode.isCurrentMode("1.9")) {
         this.sprintCounter = (int)mc.player.getCurrentItemAttackStrengthDelay();
      }
   }

   private List<Entity> getTargets() {
      Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
         .filter(entity -> entity instanceof Entity)
         .filter(this::isValidAttack);
      List<Entity> possibleTargets = stream.collect(Collectors.toList());
      if (this.priority.isCurrentMode("Range")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> (double)o.distanceTo(mc.player)));
      } else if (this.priority.isCurrentMode("FoV")) {
         possibleTargets.sort(
            Comparator.comparingDouble(o -> (double)RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
         );
      } else if (this.priority.isCurrentMode("Health")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double)living.getHealth() : 0.0));
      }

      possibleTargets.sort(Comparator.comparing(o -> o instanceof EndCrystal ? 0 : 1));
      return this.infSwitch.getCurrentValue()
         ? possibleTargets
         : possibleTargets.subList(0, (int)Math.min((float)possibleTargets.size(), this.switchSize.getCurrentValue()));
   }
}