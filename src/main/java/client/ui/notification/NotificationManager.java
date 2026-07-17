package client.ui.notification;

import client.events.impl.EventRender2D;
import client.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;

public class NotificationManager {

   private final List<Notification> notifications = new CopyOnWriteArrayList<>();

   public void addNotification(Notification notification) {
      this.notifications.add(notification);
   }

   public void onRender(EventRender2D e) {
      if (this.notifications.isEmpty()) {
         return;
      }

      PoseStack stack = e.getStack();
      float height = 5.0F;

      for (Notification notification : this.notifications) {
         float width = notification.getWidth();
         height += notification.getHeight();
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         float lifeTime = (float)(System.currentTimeMillis() - notification.getCreateTime());
         if (lifeTime > (float)notification.getMaxAge()) {
            widthTimer.target = 0.0F;
            heightTimer.target = 0.0F;
            if (widthTimer.isAnimationDone(true)) {
               this.notifications.remove(notification);
               height -= notification.getHeight();
               continue;
            }
         } else {
            widthTimer.target = width;
            heightTimer.target = height;
         }

         widthTimer.update(true);
         heightTimer.update(true);
         Window window = Minecraft.getInstance().getWindow();
         notification.render(stack, (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F,
               (float)window.getGuiScaledHeight() - heightTimer.value);
      }
   }
}