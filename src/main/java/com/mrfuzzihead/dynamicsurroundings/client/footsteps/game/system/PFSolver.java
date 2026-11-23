package com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem.ConfigOptions;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.EventType;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IOptions;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem.Substrate;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IIsolator;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.ISolver;
import com.mrfuzzihead.dynamicsurroundings.util.MathStuff;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PFSolver implements ISolver {

    private final IIsolator isolator;

    public PFSolver(IIsolator isolator) {
        this.isolator = isolator;
    }

    @Override
    public void playAssociation(EntityPlayer ply, Association assos, EventType eventType) {
        if (assos != null && !assos.isNotEmitter()) {
            if (assos.getNoAssociation()) {
                this.isolator.getDefaultStepPlayer()
                    .playStep(ply, assos);
            } else {
                this.isolator.getAcoustics()
                    .playAcoustic(ply, assos, eventType);
            }
        }
    }

    static boolean isStationaryAirborne(EntityPlayer ply) {
        return !ply.onGround && !ply.isInWater() && !ply.isOnLadder() && Math.abs(ply.motionY) < 0.02;
    }

    @Override
    public Association findAssociationForPlayer(EntityPlayer ply, double verticalOffsetAsMinus, boolean isRightFoot) {
        if (isStationaryAirborne(ply)) {
            return null;
        } else {
            int yy = MathHelper.floor_double(ply.boundingBox.minY - 0.1 - verticalOffsetAsMinus);
            double rot = MathStuff.toRadians(MathHelper.wrapAngleTo180_float(ply.rotationYaw));
            double xn = MathStuff.cos(rot);
            double zn = MathStuff.sin(rot);
            float feetDistanceToCenter = 0.2F * (isRightFoot ? -1 : 1);
            int xx = MathHelper.floor_double(ply.posX + xn * feetDistanceToCenter);
            int zz = MathHelper.floor_double(ply.posZ + zn * feetDistanceToCenter);
            return this.findAssociationForLocation(ply, xx, yy, zz);
        }
    }

    @Override
    public Association findAssociationForPlayer(EntityPlayer ply, double verticalOffsetAsMinus) {
        int yy = MathHelper.floor_double(ply.posY - 0.1 - ply.getYOffset() - verticalOffsetAsMinus);
        int xx = MathHelper.floor_double(ply.posX);
        int zz = MathHelper.floor_double(ply.posZ);
        return this.findAssociationForLocation(ply, xx, yy, zz);
    }

    @Override
    public Association findAssociationForLocation(EntityPlayer player, int x, int y, int z) {
        if (player.isInWater()) {
            ModLog.debug(
                "WARNING!!! Playing a sound while in the water! This is supposed to be halted by the stopping conditions!!");
        }

        Association worked = this.findAssociationForBlock(x, y, z);
        if (worked == null) {
            double xdang = (player.posX - x) * 2.0 - 1.0;
            double zdang = (player.posZ - z) * 2.0 - 1.0;
            if (Math.max(Math.abs(xdang), Math.abs(zdang)) > 0.2F) {
                boolean isXdangMax = Math.abs(xdang) > Math.abs(zdang);
                if (isXdangMax) {
                    worked = this.findAssociationForBlock(xdang > 0.0 ? x + 1 : x - 1, y, z);
                } else {
                    worked = this.findAssociationForBlock(x, y, zdang > 0.0 ? z + 1 : z - 1);
                }

                if (worked == null) {
                    if (isXdangMax) {
                        worked = this.findAssociationForBlock(x, y, zdang > 0.0 ? z + 1 : z - 1);
                    } else {
                        worked = this.findAssociationForBlock(xdang > 0.0 ? x + 1 : x - 1, y, z);
                    }
                }
            }
        }

        return worked;
    }

    @Override
    public Association findAssociationForBlock(int xx, int yy, int zz) {
        World world = EnvironStateHandler.EnvironState.getSnapshot()
            .getWorld();
        Block in = world.getBlock(xx, yy, zz);
        int inMeta = world.getBlockMetadata(xx, yy, zz);
        Block above = world.getBlock(xx, yy + 1, zz);
        int aboveMeta = world.getBlockMetadata(xx, yy + 1, zz);
        String association = null;
        if (above != Blocks.air) {
            association = this.isolator.getBlockMap()
                .getBlockMapSubstrate(above, aboveMeta, Substrate.CARPET);
        }

        if (association != null && !association.equals("NOT_EMITTER")) {
            yy++;
            in = above;
            ModLog.debug("Carpet detected: " + association);
        } else {
            if (in == Blocks.air) {
                Block below = world.getBlock(xx, yy - 1, zz);
                int belowMeta = world.getBlockMetadata(xx, yy - 1, zz);
                association = this.isolator.getBlockMap()
                    .getBlockMapSubstrate(below, belowMeta, Substrate.FENCE);
                if (association != null) {
                    yy--;
                    in = below;
                    ModLog.debug("Fence detected: " + association);
                }
            }

            if (association == null) {
                association = this.isolator.getBlockMap()
                    .getBlockMap(in, inMeta);
            }

            if (association != null && !association.equals("NOT_EMITTER") && above != Blocks.air) {
                String foliage = this.isolator.getBlockMap()
                    .getBlockMapSubstrate(above, aboveMeta, Substrate.FOLIAGE);
                if (foliage != null && !foliage.equals("NOT_EMITTER")) {
                    association = association + "," + foliage;
                    ModLog.debug("Foliage detected: " + foliage);
                }
            }
        }

        if (association != null) {
            return association.equals("NOT_EMITTER") ? null
                : new Association(in, inMeta, xx, yy, zz).setAssociation(association);
        } else {
            String primitive = this.resolvePrimitive(in);
            if (primitive != null) {
                return primitive.equals("NOT_EMITTER") ? null
                    : new Association(in, inMeta, xx, yy, zz).setPrimitive(primitive);
            } else {
                return new Association(in, inMeta, xx, yy, zz).setNoAssociation();
            }
        }
    }

    private String resolvePrimitive(Block block) {
        if (block != Blocks.air && block.stepSound != null) {
            String soundName = block.stepSound.soundName;
            if (soundName != null && !soundName.isEmpty()) {
                String substrate = String
                    .format(Locale.ENGLISH, "%.2f_%.2f", block.stepSound.volume, block.stepSound.frequency);
                String primitive = this.isolator.getPrimitiveMap()
                    .getPrimitiveMapSubstrate(soundName, substrate);
                if (primitive == null) {
                    if (block.stepSound.soundName != null) {
                        primitive = this.isolator.getPrimitiveMap()
                            .getPrimitiveMapSubstrate(soundName, "break_" + soundName);
                    }

                    if (primitive == null) {
                        primitive = this.isolator.getPrimitiveMap()
                            .getPrimitiveMap(soundName);
                    }
                }

                if (primitive != null) {
                    ModLog.debug("Primitive found for " + soundName + ":" + substrate);
                    return primitive;
                } else {
                    ModLog.debug("No primitive for " + soundName + ":" + substrate);
                    return null;
                }
            } else {
                return "NOT_EMITTER";
            }
        } else {
            return "NOT_EMITTER";
        }
    }

    @Override
    public boolean playSpecialStoppingConditions(EntityPlayer ply) {
        if (ply.isInWater()) {
            float volume = MathHelper.sqrt_double(
                ply.motionX * ply.motionX * 0.2 + ply.motionY * ply.motionY + ply.motionZ * ply.motionZ * 0.2) * 0.35F;
            ConfigOptions options = new ConfigOptions();
            options.getMap()
                .put(IOptions.Option.GLIDING_VOLUME, volume > 1.0F ? 1.0F : volume);
            this.isolator.getAcoustics()
                .playAcoustic(
                    ply,
                    "_SWIM",
                    ply.isInsideOfMaterial(Material.water) ? EventType.SWIM : EventType.WALK,
                    options);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasSpecialStoppingConditions(EntityPlayer ply) {
        return ply.isInWater();
    }

    @Override
    public Association findAssociationForBlock(int xx, int yy, int zz, String strategy) {
        if (!strategy.equals("find_messy_foliage")) {
            return null;
        } else {
            World world = EnvironStateHandler.EnvironState.getSnapshot()
                .getWorld();
            Block above = world.getBlock(xx, yy + 1, zz);
            if (above == Blocks.air) {
                return null;
            } else {
                int aboveMeta = world.getBlockMetadata(xx, yy + 1, zz);
                String association = null;
                boolean found = false;
                String foliage = this.isolator.getBlockMap()
                    .getBlockMapSubstrate(above, aboveMeta, Substrate.FOLIAGE);
                if (foliage != null && !foliage.equals("NOT_EMITTER")) {
                    association = foliage;
                    String isMessy = this.isolator.getBlockMap()
                        .getBlockMapSubstrate(above, aboveMeta, Substrate.MESSY);
                    if (isMessy != null && isMessy.equals("MESSY_GROUND")) {
                        found = true;
                    }
                }

                if (found && association != null) {
                    return association.equals("NOT_EMITTER") ? null : new Association().setAssociation(association);
                } else {
                    return null;
                }
            }
        }
    }
}
