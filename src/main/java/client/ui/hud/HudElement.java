package client.ui.hud;

public interface HudElement {
   float getHudX();

   float getHudY();

   float getHudWidth();

   float getHudHeight();

   void setHudX(float x);

   void setHudY(float y);

   boolean isHudDragging();

   void setHudDragging(boolean dragging, float offsetX, float offsetY);

   float getHudDragOffsetX();

   float getHudDragOffsetY();

   default boolean isHudHovered(int mouseX, int mouseY) {
      return mouseX >= getHudX() && mouseX <= getHudX() + getHudWidth()
            && mouseY >= getHudY() && mouseY <= getHudY() + getHudHeight();
   }

   default boolean mousePressed(int mouseX, int mouseY, int button) {
      if (button == 0 && isHudHovered(mouseX, mouseY)) {
         setHudDragging(true, mouseX - getHudX(), mouseY - getHudY());
         return true;
      }
      return false;
   }

   default void mouseDragged(int mouseX, int mouseY) {
      setHudX(mouseX - getHudDragOffsetX());
      setHudY(mouseY - getHudDragOffsetY());
   }

   default void stopDragging() {
      setHudDragging(false, 0, 0);
   }
}