package com.mrfuzzihead.dynamicsurroundings.client.weather;

import net.minecraft.block.Block;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.mrfuzzihead.dynamicsurroundings.client.fx.particle.ParticleFactory;

public class NetherSplashRenderer extends StormSplashRenderer {

    @Override
    protected String getBlockSoundFX(Block block, boolean hasDust, World world) {
        return hasDust ? Weather.getIntensity()
            .getDustSound() : null;
    }

    @Override
    protected EntityFX getBlockParticleFX(Block block, boolean dust, World world, double x, double y, double z) {
        return dust ? ParticleFactory.smoke.getEntityFX(0, world, x, y, z, 0.0, 0.0, 0.0) : null;
    }

    @Override
    protected int getPrecipitationHeight(World world, int range, int x, int z, EntityLivingBase view) {
        int y = view == null ? 64 : MathHelper.floor_double(view.posY);
        boolean airBlockFound = false;

        for (int i = range; i >= -range; i--) {
            int yEffective = y + i;
            if (world.isAirBlock(x, yEffective, z)) {
                airBlockFound = true;
                continue;
            }

            Block block = world.getBlock(x, yEffective, z);
            if (airBlockFound && block.getMaterial()
                .isSolid()) {
                return yEffective + 1;
            }
        }

        return 128;
    }
}
