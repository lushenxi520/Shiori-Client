package client.modules.impl.combat;

import client.Shiori;
import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventMotion;
import client.events.impl.EventPacket;
import client.events.impl.EventRespawn;
import client.modules.Category;
import client.modules.Module;
import client.modules.ModuleInfo;
import client.utils.ChatUtils;
import client.values.ValueBuilder;
import client.values.impl.FloatValue;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

@ModuleInfo(
   name = "AntiBots",
   category = Category.COMBAT,
   description = "Prevents bots from attacking you"
)
public class AntiBots extends Module {
   private static final Map<UUID, String> uuidDisplayNames = new ConcurrentHashMap<>();
   private static final Map<Integer, String> entityIdDisplayNames = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> uuids = new ConcurrentHashMap<>();
   private static final Set<Integer> ids = new HashSet<>();
   private static final Map<UUID, Long> respawnTime = new ConcurrentHashMap<>();
   private final FloatValue respawnTimeValue = ValueBuilder.create(this, "Respawn Time")
      .setDefaultFloatValue(2500.0F)
      .setFloatStep(100.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(10000.0F)
      .build()
      .getFloatValue();

   public static boolean isBedWarsBot(Entity entity) {
      AntiBots module = (AntiBots) Shiori.getInstance().getModuleManager().getModule(AntiBots.class);
      if (module.respawnTimeValue.getCurrentValue() < 1.0F) {
         return false;
      } else {
         return !respawnTime.containsKey(entity.getUUID())
            ? false
            : (float)(System.currentTimeMillis() - respawnTime.get(entity.getUUID())) < module.respawnTimeValue.getCurrentValue();
      }
   }

   public static boolean isBot(Entity entity) {
      return ids.contains(entity.getId());
   }

   @EventTarget
   public void bedWarsBot(EventPacket e) {
      if (e.getType() == EventType.RECEIVE && mc.level != null) {
         if (e.getPacket() instanceof ClientboundPlayerInfoUpdatePacket) {
            ClientboundPlayerInfoUpdatePacket packet = (ClientboundPlayerInfoUpdatePacket)e.getPacket();
            if (packet.actions().contains(Action.ADD_PLAYER)) {
               for (net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
                  GameProfile profile = entry.profile();
                  UUID id = profile.getId();
                  respawnTime.put(id, System.currentTimeMillis());
               }
            }
         } else if (e.getPacket() instanceof ClientboundAnimatePacket) {
            ClientboundAnimatePacket packet = (ClientboundAnimatePacket)e.getPacket();
            Entity entity = mc.level.getEntity(packet.getId());
            if (entity != null && packet.getAction() == 0 && respawnTime.containsKey(entity.getUUID())) {
               respawnTime.remove(entity.getUUID());
            }
         }
      }
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      uuidDisplayNames.clear();
      entityIdDisplayNames.clear();
      ids.clear();
      uuids.clear();
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         for (Entry<UUID, Long> entry : uuids.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() > 500L) {
               ChatUtils.addChatMessage("Fake Staff Detected! (" + uuidDisplayNames.get(entry.getKey()) + ")");
               uuids.remove(entry.getKey());
            }
         }
      }
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.RECEIVE) {
         if (e.getPacket() instanceof ClientboundPlayerInfoUpdatePacket) {
            ClientboundPlayerInfoUpdatePacket packet = (ClientboundPlayerInfoUpdatePacket)e.getPacket();
            if (packet.actions().contains(Action.ADD_PLAYER)) {
               for (net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
                  if (entry.displayName() != null && entry.displayName().getSiblings().isEmpty() && entry.gameMode() == GameType.SURVIVAL) {
                     UUID uuid = entry.profile().getId();
                     uuids.put(uuid, System.currentTimeMillis());
                     uuidDisplayNames.put(uuid, entry.displayName().getString());
                  }
               }
            }
         } else if (e.getPacket() instanceof ClientboundAddPlayerPacket) {
            ClientboundAddPlayerPacket packet = (ClientboundAddPlayerPacket)e.getPacket();
            if (uuids.containsKey(packet.getPlayerId())) {
               String displayName = uuidDisplayNames.get(packet.getPlayerId());
               ChatUtils.addChatMessage("Bot Detected! (" + displayName + ")");
               entityIdDisplayNames.put(packet.getEntityId(), displayName);
               uuids.remove(packet.getPlayerId());
               ids.add(packet.getEntityId());
            }
         } else if (e.getPacket() instanceof ClientboundRemoveEntitiesPacket) {
            ClientboundRemoveEntitiesPacket packet = (ClientboundRemoveEntitiesPacket)e.getPacket();
            IntListIterator var9 = packet.getEntityIds().iterator();

            while (var9.hasNext()) {
               Integer entityId = (Integer)var9.next();
               if (ids.contains(entityId)) {
                  String displayName = entityIdDisplayNames.get(entityId);
                  ChatUtils.addChatMessage("Bot Removed! (" + displayName + ")");
                  ids.remove(entityId);
               }
            }
         }
      }
   }
}
