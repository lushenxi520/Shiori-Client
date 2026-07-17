package client.events.impl;

import client.events.api.events.Event;

public class EventRotation implements Event {
   private float yaw;
   private float speed;

   public EventRotation(float yaw, float speed) {
      this.yaw = yaw;
      this.speed = speed;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getSpeed() {
      return this.speed;
   }

   public void setSpeed(float speed) {
      this.speed = speed;
   }
}