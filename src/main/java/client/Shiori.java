package client;

import client.commands.CommandManager;
import client.events.api.EventManager;
import client.events.api.EventTarget;
import client.events.api.types.EventType;
import client.events.impl.EventRunTicks;
import client.events.impl.EventShutdown;
import client.files.FileManager;
import client.modules.ModuleManager;
import client.modules.impl.render.ClickGUIModule;
import client.ui.notification.NotificationManager;
import client.utils.EntityWatcher;
import client.utils.EventWrapper;
import client.utils.LogUtils;
import client.utils.NetworkUtils;
import client.utils.ServerUtils;
import client.utils.TickTimeHelper;
import client.utils.renderer.Fonts;
import client.utils.renderer.PostProcessRenderer;
import client.utils.renderer.Shaders;
import client.utils.rotation.RotationManager;
import client.values.HasValueManager;
import client.values.ValueManager;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.MinecraftForge;


@Getter
@Setter
public class Shiori {

   @Getter
   public static final String CLIENT_NAME = "Shiori";
   public static final String CLIENT_DISPLAY_NAME = "cnmd";
   private static Shiori instance;
   private final EventManager eventManager;
   private final EventWrapper eventWrapper;
   public static float serverTickRate;
   private final ValueManager valueManager;
   private final HasValueManager hasValueManager;
   private final RotationManager rotationManager;
   public final ModuleManager moduleManager;
   public static boolean isMCPMapped;
   private final CommandManager commandManager;
   private final FileManager fileManager;
   private final NotificationManager notificationManager;
   public static float TICK_TIMER = 1.0F;
   public static Queue<Runnable> skipTasks = new ConcurrentLinkedQueue<>();

   private Shiori() {
      System.out.println("Shiori Init");
      instance = this;
      this.eventManager = new EventManager();
      Shaders.init();
      PostProcessRenderer.init();

      try {
         Fonts.loadFonts();
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      } catch (FontFormatException var3) {
         throw new RuntimeException(var3);
      }

      this.eventWrapper = new EventWrapper();
      this.valueManager = new ValueManager();
      this.hasValueManager = new HasValueManager();
      this.moduleManager = new ModuleManager();
      this.rotationManager = new RotationManager();
      this.commandManager = new CommandManager();
      this.fileManager = new FileManager();
      this.notificationManager = new NotificationManager();
      this.fileManager.load();
      this.moduleManager.getModule(ClickGUIModule.class).setEnabled(false);
      this.eventManager.register(getInstance());
      this.eventManager.register(this.eventWrapper);
      this.eventManager.register(new RotationManager());
      this.eventManager.register(new NetworkUtils());
      this.eventManager.register(new ServerUtils());
      this.eventManager.register(new EntityWatcher());
      this.eventManager.register(new client.ui.hud.HudManager());
      MinecraftForge.EVENT_BUS.register(this.eventWrapper);
   }

   public static void modRegister() {
      try {
         new Shiori();
      } catch (Exception var1) {
         System.err.println("Failed to load client");
         var1.printStackTrace(System.err);
      }
   }

   @EventTarget
   public void onShutdown(EventShutdown e) {
      this.fileManager.save();
      LogUtils.close();
   }

   @EventTarget(0)
   public void onEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE) {
         TickTimeHelper.update();
      }
   }

   public static Shiori getInstance() {
      return instance;
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public EventWrapper getEventWrapper() {
      return this.eventWrapper;
   }

   public ValueManager getValueManager() {
      return this.valueManager;
   }

   public HasValueManager getHasValueManager() {
      return this.hasValueManager;
   }

   public RotationManager getRotationManager() {
      return this.rotationManager;
   }

   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   public FileManager getFileManager() {
      return this.fileManager;
   }

   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }
}