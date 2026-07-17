package client.events.impl;

import client.events.api.events.Event;

public class EventStrafe implements Event {
   private float yaw;
   private float forward;
   private float strafe;

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setForward(float forward) {
      this.forward = forward;
   }

   public float getForward() {
      return this.forward;
   }

   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public EventStrafe(float yaw, float forward, float strafe) {
      this.yaw = yaw;
      this.forward = forward;
      this.strafe = strafe;
   }
}