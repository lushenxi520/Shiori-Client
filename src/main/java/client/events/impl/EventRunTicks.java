package client.events.impl;

import client.events.api.events.Event;
import client.events.api.types.EventType;

public class EventRunTicks implements Event {
   private final EventType type;

   public EventType getType() {
      return this.type;
   }

   public EventRunTicks(EventType type) {
      this.type = type;
   }
}
