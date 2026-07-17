package client.utils;

import client.Shiori;
import client.events.api.EventTarget;
import client.events.impl.EventGlobalPacket;
import client.events.impl.EventPacket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkUtils {
   public static Set<Packet<?>> passthroughsPackets = new HashSet<>();
   private static final TimeHelper timer = new TimeHelper();
   public static final Logger LOGGER = LogManager.getLogger("PacketUtil");

   public static boolean isServerLag() {
      return timer.delay(500.0);
   }

   public static void sendPacketNoEvent(Packet<?> packet) {
      LOGGER.info("Sending: " + packet.getClass().getName());
      if (packet instanceof ServerboundCustomPayloadPacket sb) {
         LOGGER.info("RE custompayload, {}", sb.getIdentifier().toString());
         if (sb.getIdentifier().toString().equals("heypixelmod:s2cevent")) {
            FriendlyByteBuf data = sb.getData();
            data.markReaderIndex();
            int id = data.readVarInt();
            LOGGER.info("after packet ({}", id);
            if (id == 2) {
               LOGGER.info("after packet");
               LOGGER.info(Arrays.toString(MixinProtectionUtils.readByteArray(data, data.readableBytes())));
            }

            data.resetReaderIndex();
         }
      }

      passthroughsPackets.add(packet);
      Minecraft.getInstance().getConnection().send(packet);
   }

   @EventTarget(4)
   public void onGlobalPacket(EventGlobalPacket e) {
      if (e.getPacket() instanceof ClientboundPingPacket
         || e.getPacket() instanceof ClientboundMoveEntityPacket
         || e.getPacket() instanceof ClientboundSetTimePacket
         || e.getPacket() instanceof ClientboundSetPlayerTeamPacket) {
         timer.reset();
      }

      if (!e.isCancelled()) {
         Packet<?> packet = e.getPacket();
         EventPacket event = new EventPacket(e.getType(), packet);
         Shiori.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            e.setCancelled(true);
         }

         e.setPacket(event.getPacket());
      }
   }
}