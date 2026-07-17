package client.utils;

public class SmoothAnimationTimer {
   public float target;
   public float speed = 0.4F;
   public float value;
   private long startTime;
   private float startValue;
   private float duration;
   private boolean isAnimating;

   public SmoothAnimationTimer() {
      this.target = 0.0F;
      this.value = 0.0F;
   }

   public SmoothAnimationTimer(float target) {
      this.target = target;
      this.value = target;
   }

   public SmoothAnimationTimer(float target, float value) {
      this.target = target;
      this.value = value;
   }

   public SmoothAnimationTimer(float target, float value, float speed) {
      this.target = target;
      this.speed = speed;
      this.value = value;
   }

   public void update(boolean increment) {
      this.value = AnimationUtils.getAnimationState(
         this.value, increment ? this.target : 0.0F, Math.max(10.0F, Math.abs(this.value - (increment ? this.target : 0.0F)) * 40.0F) * this.speed
      );
   }

   public boolean isAnimationDone(boolean increment) {
      return increment ? this.value == this.target : this.value == 0.0F;
   }

   public void animate(float newTarget, float duration) {
      if (this.target != newTarget) {
         this.target = newTarget;
         this.startValue = this.value;
         this.startTime = System.currentTimeMillis();
         this.duration = duration * 1000.0F;
         this.isAnimating = true;
      }
   }

   public void tick() {
      if (!this.isAnimating) {
         return;
      }
      long elapsed = System.currentTimeMillis() - this.startTime;
      float progress = Math.min(1.0F, (float) elapsed / this.duration);
      float easedProgress = easeOutCubic(progress);
      this.value = this.startValue + (this.target - this.startValue) * easedProgress;
      if (progress >= 1.0F) {
         this.value = this.target;
         this.isAnimating = false;
      }
   }

   public float getValueF() {
      return this.value;
   }

   public float getProgress() {
      if (!this.isAnimating || this.duration <= 0.0F) {
         return 1.0F;
      }
      long elapsed = System.currentTimeMillis() - this.startTime;
      return Math.min(1.0F, (float) elapsed / this.duration);
   }

   public boolean isAnimating() {
      return this.isAnimating;
   }

   public void setCurrentValue(float value) {
      this.value = value;
      this.target = value;
      this.isAnimating = false;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public void setStartTime(long time) {
      this.startTime = time;
   }

   private static float easeOutCubic(float t) {
      return (float)(1.0 - Math.pow(1.0 - t, 3.0));
   }
}