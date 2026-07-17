package inject.mixin.O;

import client.gui.WelcomeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import client.Shiori;
import client.events.api.types.EventType;
import client.events.impl.EventClick;
import client.events.impl.EventRunTicks;
import client.events.impl.EventShutdown;
import client.modules.impl.misc.GhostHand;
import client.modules.impl.render.Glow;
import client.utils.AnimationUtils;
import client.utils.raytrace.ClientRayTraceUtil;

import java.util.ArrayList;
import java.util.List;

@Mixin({Minecraft.class})
public abstract class MixinMinecraft {
    @Shadow @Nullable
    public HitResult hitResult;

    @Shadow
    public abstract void setScreen(@Nullable Screen p_91153_);

    @Unique
    private long shiori_NextGeneration$lastFrame;

    @Unique
    private boolean shiori$welcomeScreenShown = false;

    @Inject(
            method = "createTitle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onCreateTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Shiori");
    }

    @Inject(
            method = "setScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (!shiori$welcomeScreenShown && screen instanceof TitleScreen && !(screen instanceof WelcomeScreen)) {
            shiori$welcomeScreenShown = true;
            setScreen(new WelcomeScreen());
            ci.cancel();
        }
    }

    @Inject(
            method = {"<init>"},
            at = {@At("TAIL")}
    )
    private void onInit(CallbackInfo info) {
        Shiori.modRegister();
    }

    @Inject(
            method = {"<init>"},
            at = {@At("RETURN")}
    )
    public void onInit(GameConfig pGameConfig, CallbackInfo ci) {
        System.setProperty("java.awt.headless", "false");
        ModList.get().getMods().removeIf(modInfox -> modInfox.getModId().contains("shiori"));
        List<IModFileInfo> fileInfoToRemove = new ArrayList<>();

        for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
            for (IModInfo modInfo : fileInfo.getMods()) {
                if (modInfo.getModId().contains("shiori")) {
                    fileInfoToRemove.add(fileInfo);
                }
            }
        }

        ModList.get().getModFiles().removeAll(fileInfoToRemove);
    }

    @Inject(
            method = {"close"},
            at = {@At("HEAD")},
            remap = false
    )
    private void shutdown(CallbackInfo ci) {
        if (Shiori.getInstance() != null && Shiori.getInstance().getEventManager() != null) {
            Shiori.getInstance().getEventManager().call(new EventShutdown());
        }
    }

    @Inject(
            method = {"tick"},
            at = {@At("HEAD")}
    )
    private void tickPre(CallbackInfo ci) {
        if (Shiori.getInstance() != null && Shiori.getInstance().getEventManager() != null) {
            Shiori.getInstance().getEventManager().call(new EventRunTicks(EventType.PRE));
        }
    }

    @Inject(
            method = {"tick"},
            at = {@At("TAIL")}
    )
    private void tickPost(CallbackInfo ci) {
        if (Shiori.getInstance() != null && Shiori.getInstance().getEventManager() != null) {
            Shiori.getInstance().getEventManager().call(new EventRunTicks(EventType.POST));
        }
    }

    @Inject(
            method = {"tick"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V",
                    shift = Shift.BEFORE
            )}
    )
    private void hookRotation(CallbackInfo ci) {
        ClientRayTraceUtil.updateEyePos();
    }

    @Inject(
            method = {"shouldEntityAppearGlowing"},
            at = {@At("RETURN")},
            cancellable = true
    )
    private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (Glow.shouldGlow(pEntity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = {"runTick"},
            at = {@At("HEAD")}
    )
    private void runTick(CallbackInfo ci) {
        long currentTime = System.nanoTime() / 1000000L;
        int deltaTime = (int)(currentTime - this.shiori_NextGeneration$lastFrame);
        this.shiori_NextGeneration$lastFrame = currentTime;
        AnimationUtils.delta = deltaTime;
    }

    @Inject(
            method = {"handleKeybinds"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
                    ordinal = 0,
                    shift = Shift.BEFORE
            )},
            cancellable = true
    )
    private void clickEvent(CallbackInfo ci) {
        if (Shiori.getInstance() != null && Shiori.getInstance().getEventManager() != null) {
            EventClick event = new EventClick();
            Shiori.getInstance().getEventManager().call(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = {"startUseItem"},
            at = {@At("HEAD")}
    )
    private void onStartUseItem(CallbackInfo ci) {
        if (!GhostHand.enabled) return;
        if (this.hitResult == null || this.hitResult.getType() != HitResult.Type.BLOCK) return;

        Minecraft mc = Minecraft.getInstance();
        BlockHitResult blockHit = (BlockHitResult) this.hitResult;
        BlockPos hitPos = blockHit.getBlockPos();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        BlockState hitState = mc.level.getBlockState(hitPos);
        if (isContainerBlock(hitState)) return;

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getLookAngle();
        double maxReach = mc.gameMode.getPickRange();
        Vec3 endPos = eyePos.add(lookVec.scale(maxReach));

        Vec3 currentStart = blockHit.getLocation().add(lookVec.scale(0.05));
        while (currentStart.distanceTo(eyePos) < maxReach) {
            BlockHitResult nextHit = mc.level.clip(
                new ClipContext(currentStart, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)
            );
            if (nextHit.getType() != HitResult.Type.BLOCK) break;

            BlockPos nextPos = nextHit.getBlockPos();
            BlockState nextState = mc.level.getBlockState(nextPos);

            if (isContainerBlock(nextState)) {
                this.hitResult = nextHit;
                break;
            }

            currentStart = nextHit.getLocation().add(lookVec.scale(0.05));
        }
    }

    private static boolean isContainerBlock(BlockState state) {
        return state.getBlock() instanceof ChestBlock
            || state.getBlock() instanceof EnderChestBlock
            || state.getBlock() instanceof ShulkerBoxBlock
            || state.getBlock() instanceof BarrelBlock
            || state.getBlock() instanceof FurnaceBlock
            || state.getBlock() instanceof BrewingStandBlock
            || state.getBlock() instanceof HopperBlock
            || state.getBlock() instanceof DispenserBlock
            || state.getBlock() instanceof CraftingTableBlock
            || state.getBlock() instanceof BeaconBlock;
    }

    public void setShiori_NextGeneration$lastFrame(long shiori_NextGeneration$lastFrame) {
        this.shiori_NextGeneration$lastFrame = shiori_NextGeneration$lastFrame;
    }
}