package client.events.impl;

import client.events.api.events.Event;

public class EventMouseClick implements Event {
   private final int key;
   private final boolean state;
   private final double mouseX;
   private final double mouseY;

   public int getKey() {
      return this.key;
   }

   public boolean isState() {
      return this.state;
   }

   public double getMouseX() {
      return this.mouseX;
   }

   public double getMouseY() {
      return this.mouseY;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventMouseClick other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         return this.getKey() != other.getKey() ? false : this.isState() == other.isState();
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventMouseClick;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getKey();
      return result * 59 + (this.isState() ? 79 : 97);
   }

   @Override
   public String toString() {
      return "EventMouseClick(key=" + this.getKey() + ", state=" + this.isState() + ", mouseX=" + this.mouseX + ", mouseY=" + this.mouseY + ")";
   }

   public EventMouseClick(int key, boolean state, double mouseX, double mouseY) {
      this.key = key;
      this.state = state;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
   }
}