package client.modules.impl.misc;

import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventRespawn;
import client.events.impl.EventRunTicks;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.utils.PacketUtils;
import client.values.ValueBuilder;
import client.values.impl.BooleanValue;
import inject.mixin.O.accessors.MultiPlayerGameModeAccessor;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

@ModuleInfo(
   name = "GameHelper",
   description = "Game helper utilities",
   category = Category.MISC
)
public class GameHelper extends Module {

   public BooleanValue antiLava = ValueBuilder.create(this, "AntiLava")
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();

   public boolean rotation = false;

   private enum State { IDLE, PLACING, WAITING, PICKING_UP }
   private State state = State.IDLE;
   private int waterBucketSlot = -1;
   private int prevSlot = -1;
   private int waitTicks = 0;

   @EventTarget(5)
   public void onTick(EventRunTicks e) {
      if (e.getType() != EventType.PRE) return;
      if (mc.player == null || mc.level == null || mc.gameMode == null) return;

      if (!antiLava.getCurrentValue()) {
         if (state != State.IDLE) {
            resetState();
         }
         return;
      }

      switch (state) {
         case IDLE:
            if (mc.player.isOnFire()) {
               waterBucketSlot = findWaterBucket();
               if (waterBucketSlot != -1) {
                  this.rotation = true;
                  state = State.PLACING;
               }
            }
            break;

         case PLACING:
            if (!mc.player.isOnFire()) {
               resetState();
               break;
            }
            prevSlot = mc.player.getInventory().selected;
            mc.player.getInventory().selected = waterBucketSlot;
            ((MultiPlayerGameModeAccessor) mc.gameMode).invokeEnsureHasSentCarriedItem();
            useItem();
            waitTicks = 0;
            state = State.WAITING;
            break;

         case WAITING:
            waitTicks++;
            if (waitTicks >= 2) {
               state = State.PICKING_UP;
            }
            break;

         case PICKING_UP:
            useItem();
            mc.player.getInventory().selected = prevSlot;
            ((MultiPlayerGameModeAccessor) mc.gameMode).invokeEnsureHasSentCarriedItem();
            prevSlot = -1;
            this.rotation = false;
            state = State.IDLE;
            break;
      }
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      resetState();
   }

   private void useItem() {
      if (mc.player == null || mc.gameMode == null) return;

      mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
          mc.player.getYRot(), 90.0F, mc.player.onGround()
      ));

      MultiPlayerGameModeAccessor gameMode = (MultiPlayerGameModeAccessor) mc.gameMode;
      gameMode.invokeEnsureHasSentCarriedItem();
      PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id));
      ItemStack itemstack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
      if (mc.player.getCooldowns().isOnCooldown(itemstack.getItem())) {
         return;
      }
      InteractionResult cancelResult = ForgeHooks.onItemRightClick(mc.player, InteractionHand.MAIN_HAND);
      if (cancelResult != null) {
         return;
      }
      InteractionResultHolder<ItemStack> interactionresultholder = itemstack.use(mc.level, mc.player, InteractionHand.MAIN_HAND);
      ItemStack itemstack1 = interactionresultholder.getObject();
      if (itemstack1 != itemstack) {
         mc.player.setItemInHand(InteractionHand.MAIN_HAND, itemstack1);
         if (itemstack1.isEmpty()) {
            ForgeEventFactory.onPlayerDestroyItem(mc.player, itemstack, InteractionHand.MAIN_HAND);
         }
      }
   }

   private int findWaterBucket() {
      for (int i = 0; i < 9; i++) {
         ItemStack item = mc.player.getInventory().getItem(i);
         if (!item.isEmpty() && item.getItem() == Items.WATER_BUCKET) {
            return i;
         }
      }
      return -1;
   }

   private void resetState() {
      if (prevSlot != -1) {
         mc.player.getInventory().selected = prevSlot;
         prevSlot = -1;
      }
      this.rotation = false;
      state = State.IDLE;
   }

   @Override
   public void onDisable() {
      resetState();
   }
}