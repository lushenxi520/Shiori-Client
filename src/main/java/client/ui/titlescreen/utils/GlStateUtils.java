package client.ui.titlescreen.utils;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;

public class GlStateUtils {

    private static State savedState = null;

    public static void save() {
        savedState = State.capture();
    }

    public static void restore() {
        if (savedState != null) {
            savedState.restore();
        }
    }

    private static void setEnabled(int cap, boolean enabled) {
        if (enabled) GL11C.glEnable(cap);
        else GL11C.glDisable(cap);
    }

    private static class State {
        final boolean blendEnabled;
        final int blendSrcRgb;
        final int blendDstRgb;
        final int blendSrcAlpha;
        final int blendDstAlpha;
        final boolean depthTestEnabled;
        final boolean depthMask;
        final int depthFunc;
        final boolean cullEnabled;
        final int cullFace;
        final int activeTexture;
        final int[] textureBindings2D;
        final int[] samplerBindings;
        final int program;
        final int vaoBinding;
        final boolean colorMaskR;
        final boolean colorMaskG;
        final boolean colorMaskB;
        final boolean colorMaskA;
        final int unpackAlignment;
        final int pixelUnpackBufferBinding;
        final boolean scissorTestEnabled;
        final int[] scissorBox;

        State(boolean blendEnabled, int blendSrcRgb, int blendDstRgb, int blendSrcAlpha, int blendDstAlpha,
              boolean depthTestEnabled, boolean depthMask, int depthFunc, boolean cullEnabled, int cullFace,
              int activeTexture, int[] textureBindings2D, int[] samplerBindings, int program, int vaoBinding,
              boolean colorMaskR, boolean colorMaskG, boolean colorMaskB, boolean colorMaskA,
              int unpackAlignment, int pixelUnpackBufferBinding, boolean scissorTestEnabled, int[] scissorBox) {
            this.blendEnabled = blendEnabled;
            this.blendSrcRgb = blendSrcRgb;
            this.blendDstRgb = blendDstRgb;
            this.blendSrcAlpha = blendSrcAlpha;
            this.blendDstAlpha = blendDstAlpha;
            this.depthTestEnabled = depthTestEnabled;
            this.depthMask = depthMask;
            this.depthFunc = depthFunc;
            this.cullEnabled = cullEnabled;
            this.cullFace = cullFace;
            this.activeTexture = activeTexture;
            this.textureBindings2D = textureBindings2D;
            this.samplerBindings = samplerBindings;
            this.program = program;
            this.vaoBinding = vaoBinding;
            this.colorMaskR = colorMaskR;
            this.colorMaskG = colorMaskG;
            this.colorMaskB = colorMaskB;
            this.colorMaskA = colorMaskA;
            this.unpackAlignment = unpackAlignment;
            this.pixelUnpackBufferBinding = pixelUnpackBufferBinding;
            this.scissorTestEnabled = scissorTestEnabled;
            this.scissorBox = scissorBox;
        }

        void restore() {
            setEnabled(GL11C.GL_BLEND, blendEnabled);
            GL14C.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);

            setEnabled(GL11C.GL_DEPTH_TEST, depthTestEnabled);
            GL11C.glDepthMask(depthMask);
            GL11C.glDepthFunc(depthFunc);

            setEnabled(GL11C.GL_CULL_FACE, cullEnabled);
            GL11C.glCullFace(cullFace);

            for (int i = 0; i < textureBindings2D.length; i++) {
                GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + i);
                GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, textureBindings2D[i]);
                GL33C.glBindSampler(i, samplerBindings[i]);
            }
            GL13C.glActiveTexture(activeTexture);

            GL20C.glUseProgram(program);
            GL30C.glBindVertexArray(vaoBinding);
            GL11C.glColorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, unpackAlignment);
            GL21C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, pixelUnpackBufferBinding);

            setEnabled(GL11C.GL_SCISSOR_TEST, scissorTestEnabled);
            GL11C.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        }

        static State capture() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                int maxTextureUnits = GL11C.glGetInteger(GL20C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
                int activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);

                int[] textureBindings = new int[maxTextureUnits];
                int[] samplerBindings = new int[maxTextureUnits];

                for (int i = 0; i < maxTextureUnits; i++) {
                    GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + i);
                    textureBindings[i] = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
                    samplerBindings[i] = GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING);
                }
                GL13C.glActiveTexture(activeTexture);

                var colorMask = stack.malloc(4);
                GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMask);
                var scissorBox = stack.mallocInt(4);
                GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, scissorBox);

                return new State(
                        GL11C.glIsEnabled(GL11C.GL_BLEND),
                        GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB),
                        GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB),
                        GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA),
                        GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA),
                        GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST),
                        GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK),
                        GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC),
                        GL11C.glIsEnabled(GL11C.GL_CULL_FACE),
                        GL11C.glGetInteger(GL11C.GL_CULL_FACE_MODE),
                        activeTexture,
                        textureBindings,
                        samplerBindings,
                        GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM),
                        GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING),
                        colorMask.get(0) != 0,
                        colorMask.get(1) != 0,
                        colorMask.get(2) != 0,
                        colorMask.get(3) != 0,
                        GL11C.glGetInteger(GL11C.GL_UNPACK_ALIGNMENT),
                        GL11C.glGetInteger(GL21C.GL_PIXEL_UNPACK_BUFFER_BINDING),
                        GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST),
                        new int[]{scissorBox.get(0), scissorBox.get(1), scissorBox.get(2), scissorBox.get(3)}
                );
            }
        }
    }
}