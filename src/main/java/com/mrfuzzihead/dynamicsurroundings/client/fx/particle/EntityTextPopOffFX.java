package com.mrfuzzihead.dynamicsurroundings.client.fx.particle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.dynamicsurroundings.util.Color;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A text pop-off marker.
 *
 * <p>
 * The original class drew a font string from inside {@code renderParticle}. That is unsafe under Angelica's particle
 * instancer (the font bypasses the capture tessellator and changes GL state inside a deferred pass) and under Iris
 * (the text is shaded as whatever gbuffer program is current). The entity now carries only its position, colour and
 * scale; {@code TextPopOffRenderer} draws all active markers in a dedicated world-overlay pass.
 * </p>
 */
@SideOnly(Side.CLIENT)
public class EntityTextPopOffFX extends EntityFX {

    protected static final float GRAVITY = 0.8F;
    protected static final float SIZE = 3.0F;
    protected static final int LIFESPAN = 12;
    private static final List<EntityTextPopOffFX> ACTIVE = new CopyOnWriteArrayList<>();
    protected Color renderColor = Color.WHITE;
    protected String text;
    protected boolean shouldOnTop = false;
    protected boolean grow = true;
    protected float scale = 1.0F;

    public EntityTextPopOffFX(World world, String text, Color color, float scale, double x, double y, double z,
        double dX, double dY, double dZ) {
        super(world, x, y, z, dX, dY, dZ);
        this.text = text;
        this.renderColor = color;
        this.motionX = dX;
        this.motionY = dY;
        this.motionZ = dZ;
        float dist = MathHelper
            .sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
        this.motionX = this.motionX / dist * 0.12;
        this.motionY = this.motionY / dist * 0.12;
        this.motionZ = this.motionZ / dist * 0.12;
        this.particleTextureJitterX = 1.5F;
        this.particleTextureJitterY = 1.5F;
        this.particleGravity = GRAVITY;
        this.particleScale = SIZE;
        this.particleMaxAge = LIFESPAN;
        ACTIVE.add(this);
    }

    public static List<EntityTextPopOffFX> active() {
        return ACTIVE;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isDead) {
            ACTIVE.remove(this);
            return;
        }

        if (this.grow) {
            this.particleScale *= 1.08F;
            if (this.particleScale > 9.0F) {
                this.grow = false;
            }
        } else {
            this.particleScale *= 0.96F;
        }
    }

    @Override
    public void renderParticle(Tessellator tessellator, float partialTicks, float rotationX, float rotationZ,
        float rotationYZ, float rotationXY, float rotationXZ) {
        // Rendered by TextPopOffRenderer after the particle/weather pass.
    }

    /**
     * Draws this marker in world space. The caller supplies the interpolated camera position so the draw is stable
     * even when it runs outside {@code EffectRenderer.renderParticles}.
     */
    public void renderText(float partialTicks, double cameraX, double cameraY, double cameraZ) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        if (font == null || mc.thePlayer == null) {
            return;
        }

        double locX = this.prevPosX + (this.posX - this.prevPosX) * partialTicks - cameraX;
        double locY = this.prevPosY + (this.posY - this.prevPosY) * partialTicks - cameraY;
        double locZ = this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - cameraZ;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        if (this.shouldOnTop) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        GL11.glTranslatef((float) locX, (float) locY, (float) locZ);
        GL11.glRotatef(-mc.thePlayer.rotationYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(mc.thePlayer.rotationPitch, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-1.0F, -1.0F, 1.0F);
        GL11.glScaled(this.particleScale * 0.008, this.particleScale * 0.008, this.particleScale * 0.008);
        GL11.glScaled(this.scale, this.scale, this.scale);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 0.003662109F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        font.drawStringWithShadow(
            this.text,
            -MathHelper.floor_float(font.getStringWidth(this.text) / 2.0F) + 1,
            -MathHelper.floor_float(font.FONT_HEIGHT / 2.0F) + 1,
            this.renderColor.rgb());
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    @Override
    public int getFXLayer() {
        return 3;
    }
}
