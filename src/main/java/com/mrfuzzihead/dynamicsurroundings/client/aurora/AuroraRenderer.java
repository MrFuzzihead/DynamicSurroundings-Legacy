package com.mrfuzzihead.dynamicsurroundings.client.aurora;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.util.Color;
import com.mrfuzzihead.dynamicsurroundings.util.DiurnalUtils;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class AuroraRenderer {

    private static void setColor(Color color, float alpha) {
        Tessellator.instance.setColorRGBA_F(color.red, color.green, color.blue, alpha);
    }

    public static float moonlightFactor(World world) {
        float moonFactor = 1.0F - DiurnalUtils.getMoonPhaseFactor(world) * 1.1F;
        return moonFactor <= 0.0F ? 0.0F : MathHelper.clamp_float(moonFactor * moonFactor, 0.0F, 1.0F);
    }

    public static void renderAurora(float partialTick, Aurora aurora) {
        Minecraft mc = FMLClientHandler.instance()
            .getClient();
        if (mc.theWorld == null || mc.thePlayer == null
            || !ModOptions.auroraEnable
            || !DimensionRegistry.hasAuroras(mc.theWorld)) {
            return;
        }

        float alpha = aurora.getAlpha() * moonlightFactor(mc.theWorld) / 255.0F;
        if (!(alpha <= 0.0F)) {
            Tessellator tess = Tessellator.instance;
            float tranY;
            if (ModOptions.auroraHeightPlayerRelative) {
                tranY = ModOptions.playerFixedHeight;
            } else {
                tranY = DimensionRegistry.getCloudHeight(mc.theWorld) + 5
                    - (float) (mc.thePlayer.lastTickPosY
                        + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTick);
            }

            double tranX = aurora.posX
                - (mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTick);
            double tranZ = aurora.posZ
                - (mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTick);
            if (ModOptions.auroraAnimate) {
                aurora.translate(partialTick);
            }

            Color base = aurora.getBaseColor();
            Color fade = aurora.getFadeColor();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            GL11.glTranslatef((float) tranX, tranY, (float) tranZ);
            GL11.glScaled(0.5, 8.0, 0.5);
            GL11.glDisable(3553);
            GL11.glDisable(2896);
            GL11.glShadeModel(7425);
            GL11.glEnable(3042);
            GL14.glBlendFuncSeparate(770, 1, 1, 0);
            GL11.glDisable(3008);
            GL11.glDisable(2884);
            GL11.glDepthMask(false);
            tess.startDrawing(4);

            for (Node[] array : aurora.getNodeList()) {
                for (int i = 0; i < array.length - 1; i++) {
                    Node node = array[i];
                    double posY = node.getModdedY();
                    double posX = node.tetX;
                    double posZ = node.tetZ;
                    double tetX = node.tetX2;
                    double tetZ = node.tetZ2;
                    double posX2;
                    double posZ2;
                    double tetX2;
                    double tetZ2;
                    double posY2;
                    if (i < array.length - 2) {
                        Node nodePlus = array[i + 1];
                        posX2 = nodePlus.tetX;
                        posZ2 = nodePlus.tetZ;
                        tetX2 = nodePlus.tetX2;
                        tetZ2 = nodePlus.tetZ2;
                        posY2 = nodePlus.getModdedY();
                    } else {
                        posX2 = tetX2 = node.posX;
                        posZ2 = tetZ2 = node.getModdedZ();
                        posY2 = 0.0;
                    }

                    setColor(base, alpha);
                    tess.addVertex(posX, 0.0, posZ);
                    setColor(fade, 0.0F);
                    tess.addVertex(posX, posY, posZ);
                    tess.addVertex(posX2, posY2, posZ2);
                    tess.addVertex(posX2, posY2, posZ2);
                    setColor(base, alpha);
                    tess.addVertex(posX2, 0.0, posZ2);
                    tess.addVertex(posX, 0.0, posZ);
                    setColor(base, alpha);
                    tess.addVertex(posX, 0.0, posZ);
                    tess.addVertex(posX2, 0.0, posZ2);
                    tess.addVertex(tetX2, 0.0, tetZ2);
                    tess.addVertex(tetX2, 0.0, tetZ2);
                    tess.addVertex(tetX, 0.0, tetZ);
                    tess.addVertex(posX, 0.0, posZ);
                    setColor(base, alpha);
                    tess.addVertex(tetX, 0.0, tetZ);
                    setColor(fade, 0.0F);
                    tess.addVertex(tetX, posY, tetZ);
                    tess.addVertex(tetX2, posY2, tetZ2);
                    tess.addVertex(tetX2, posY2, tetZ2);
                    setColor(base, alpha);
                    tess.addVertex(tetX2, 0.0, tetZ2);
                    tess.addVertex(tetX, 0.0, tetZ);
                }
            }

            tess.draw();
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
