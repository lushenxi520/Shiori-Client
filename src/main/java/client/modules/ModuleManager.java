package client.modules;

import client.Shiori;
import client.events.api.EventTarget;
import client.events.impl.EventKey;
import client.events.impl.EventMouseClick;
import client.exceptions.NoSuchModuleException;
import client.modules.impl.combat.*;
import client.modules.impl.misc.*;
import client.modules.impl.move.*;
import client.modules.impl.render.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleManager {
   private static final Logger log = LogManager.getLogger(ModuleManager.class);
   private final List<Module> modules = new ArrayList<>();
   private final Map<Class<? extends Module>, Module> classMap = new HashMap<>();
   private final Map<String, Module> nameMap = new HashMap<>();

   public ModuleManager() {
      try {
         this.initModules();
         this.modules.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
      } catch (Exception var2) {
         log.error("Failed to initialize modules", var2);
         throw new RuntimeException(var2);
      }

      Shiori.getInstance().getEventManager().register(this);
   }

   private void initModules() {
      this.registerModule(
         new Killaura(),
         new AntiKB(),
         new AutoThrow(),
         new HUD(),
         new NameTags(),
         new ChestStealer(),
         new GhostHand(),
         new InventoryManager(),
         new Scaffold(),
         new AntiBots(),
         new TargetHUD(),
         new Sprint(),
         new ChestESP(),
         new ClickGUIModule(),
         new Custom(),
         new Teams(),
         new Glow(),
         new ItemTracker(),
         new AutoMLG(),
         new NoJumpDelay(),
         new FastPlace(),
         new AntiFireball(),
         new Stuck(),
         new OldHitting(),
         new ScoreboardSpoof(),
         new AutoTools(),
         new ViewClip(),
         new Disabler(),
         new TestDis(),
         new Projectile(),
         new TimeChanger(),
         new FullBright(),
         new NameProtect(),
         new NoHurtCam(),
         new NoFOVChange(),
         new AutoClicker(),
         new AntiBlindness(),
         new AntiNausea(),
         new Scoreboard(),
         new Compass(),
         new Blink(),
         new PostProcess(),
         new AttackCrystal(),
         new EffectDisplay(),
         new NoRender(),
         new ItemTags(),
         new MotionBlur(),
         new Helper(),
         new GameHelper(),
         new LongJump(),
         new MusicPlayer(),
         new IsLand()
      );
   }

   private void registerModule(Module... modules) {
      for (Module module : modules) {
         this.registerModule(module);
      }
   }

   private void registerModule(Module module) {
      module.initModule();
      this.modules.add(module);
      this.classMap.put((Class<? extends Module>)module.getClass(), module);
      this.nameMap.put(module.getName().toLowerCase(), module);
   }

   public List<Module> getModulesByCategory(Category category) {
      List<Module> modules = new ArrayList<>();

      for (Module module : this.modules) {
         if (module.getCategory() == category) {
            modules.add(module);
         }
      }

      return modules;
   }

   public Module getModule(Class<? extends Module> clazz) {
      Module module = this.classMap.get(clazz);
      if (module == null) {
         throw new NoSuchModuleException();
      } else {
         return module;
      }
   }

   public Module getModule(String name) {
      Module module = this.nameMap.get(name.toLowerCase());
      if (module == null) {
         throw new NoSuchModuleException();
      } else {
         return module;
      }
   }

   @EventTarget
   public void onKey(EventKey e) {
      if (e.isState() && Minecraft.getInstance().screen == null) {
         for (Module module : this.modules) {
            if (module.getKey() == e.getKey()) {
               module.toggle();
            }
         }
      }
   }

   @EventTarget
   public void onKey(EventMouseClick e) {
      if (!e.isState() && (e.getKey() == 3 || e.getKey() == 4)) {
         for (Module module : this.modules) {
            if (module.getKey() == -e.getKey()) {
               module.toggle();
            }
         }
      }
   }

   public List<Module> getModules() {
      return this.modules;
   }
}