package client.utils.rotation;

import client.Shiori;
import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventAttackYaw;
import client.events.impl.EventFallFlying;
import client.events.impl.EventJump;
import client.events.impl.EventMotion;
import client.events.impl.EventMoveInput;
import client.events.impl.EventPositionItem;
import client.events.impl.EventRayTrace;
import client.events.impl.EventRespawn;
import client.events.impl.EventRotationAnimation;
import client.events.impl.EventRunTicks;
import client.events.impl.EventStrafe;
import client.events.impl.EventUseItemRayTrace;
import client.modules.impl.combat.AttackCrystal;
import client.modules.impl.combat.AutoThrow;
import client.modules.impl.combat.Killaura;
import client.modules.impl.misc.GameHelper;
import client.modules.impl.move.AutoMLG;
import client.modules.impl.move.LongJump;
import client.modules.impl.move.Scaffold;
import client.utils.MoveUtils;
import client.utils.Vector2f;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RotationManager {
   private static final Logger log = LogManager.getLogger(RotationManager.class);
   private static final Minecraft mc = Minecraft.getInstance();
   public static Vector2f rotations;
   public static Vector2f lastRotations;
   public static Vector2f animationRotation;
   public static Vector2f lastAnimationRotation;
   public static boolean active = false;

   public static void setRotations(Vector2f rotations) {
      RotationManager.rotations = rotations;
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      lastRotations = null;
      rotations = null;
   }

   @EventTarget(4)
   public void updateGlobalYaw(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         Killaura killaura = (Killaura) Shiori.getInstance().getModuleManager().getModule(Killaura.class);
         Scaffold scaffold = (Scaffold) Shiori.getInstance().getModuleManager().getModule(Scaffold.class);
         AttackCrystal attackCrystal = (AttackCrystal) Shiori.getInstance().getModuleManager().getModule(AttackCrystal.class);
         AutoMLG autoMLG = (AutoMLG) Shiori.getInstance().getModuleManager().getModule(AutoMLG.class);
         LongJump longJump = (LongJump) Shiori.getInstance().getModuleManager().getModule(LongJump.class);
         AutoThrow autoThrow = (AutoThrow) Shiori.getInstance().getModuleManager().getModule(AutoThrow.class);
         GameHelper gameHelper = (GameHelper) Shiori.getInstance().getModuleManager().getModule(GameHelper.class);
         active = true;
         if (autoMLG.isEnabled() && autoMLG.rotation) {
            setRotations(new Vector2f(mc.player.getYRot(), 90.0F));
         } else if (gameHelper.isEnabled() && gameHelper.rotation) {
            setRotations(new Vector2f(mc.player.getYRot(), 90.0F));
         } else if (longJump.isEnabled() && LongJump.rotation != null) {
            setRotations(LongJump.rotation.toVec2f());
         } else if (attackCrystal.isEnabled() && AttackCrystal.rotations != null) {
            setRotations(new Vector2f(AttackCrystal.rotations.x, AttackCrystal.rotations.y));
         } else if (autoThrow.isEnabled() && autoThrow.targetRotation != null) {
            setRotations(new Vector2f(autoThrow.targetRotation.x, autoThrow.targetRotation.y));
         } else if (scaffold.isEnabled() && scaffold.rots != null) {
            setRotations(new Vector2f(scaffold.rots.x, scaffold.rots.y));
         } else if (killaura.isEnabled() && killaura.target != null && killaura.rotation != null) {
            setRotations(new Vector2f(killaura.rotation.x, killaura.rotation.y));
         } else {
            active = false;
         }
      }
   }

   @EventTarget
   public void onAnimation(EventRotationAnimation e) {
      if (animationRotation != null && lastAnimationRotation != null) {
         e.setYaw(animationRotation.x);
         e.setLastYaw(lastAnimationRotation.x);
         e.setPitch(animationRotation.y);
         e.setLastPitch(lastAnimationRotation.y);
      }
   }

   @EventTarget(4)
   public void onPre(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (rotations == null || lastRotations == null) {
            rotations = lastRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
         }

         lastAnimationRotation = animationRotation;
         float yaw = rotations.x;
         float pitch = rotations.y;
         if (!Float.isNaN(yaw) && !Float.isNaN(pitch) && active) {
            e.setYaw(yaw);
            e.setPitch(pitch);
         }

         Scaffold scaffold = (Scaffold) Shiori.getInstance().getModuleManager().getModule(Scaffold.class);
         if (scaffold.isEnabled()) {
            animationRotation = scaffold.correctRotation;
         } else {
            animationRotation = new Vector2f(e.getYaw(), e.getPitch());
         }

         lastRotations = new Vector2f(e.getYaw(), e.getPitch());
      }
   }

   @EventTarget
   public void onMove(EventMoveInput event) {
      if (active && rotations != null) {
         float yaw = rotations.x;
         MoveUtils.fixMovement(event, yaw);
      }
   }

   @EventTarget
   public void onMove(EventRayTrace event) {
      if (rotations != null && event.entity == mc.player && active) {
         event.setYaw(rotations.x);
         event.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onItemRayTrace(EventUseItemRayTrace event) {
      if (rotations != null && active) {
         event.setYaw(rotations.x);
         event.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onStrafe(EventStrafe event) {
      if (active && rotations != null) {
         event.setYaw(rotations.x);
      }
   }

   @EventTarget
   public void onJump(EventJump event) {
      if (active && rotations != null) {
         event.setYaw(rotations.x);
      }
   }

   @EventTarget(0)
   public void onPositionItem(EventPositionItem e) {
      if (active && rotations != null) {
         PosRot packet = (PosRot)e.getPacket();
         PosRot newPacket = new PosRot(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), rotations.getX(), rotations.getY(), packet.isOnGround());
         e.setPacket(newPacket);
      }
   }

   @EventTarget
   public void onFallFlying(EventFallFlying e) {
      if (rotations != null) {
         e.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onAttack(EventAttackYaw e) {
      if (rotations != null) {
         e.setYaw(rotations.x);
      }
   }
}