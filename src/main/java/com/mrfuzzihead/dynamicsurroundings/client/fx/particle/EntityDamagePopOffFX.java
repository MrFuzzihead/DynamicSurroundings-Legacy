package com.mrfuzzihead.dynamicsurroundings.client.fx.particle;

import net.minecraft.world.World;

import com.mrfuzzihead.dynamicsurroundings.util.RgbColor;

public class EntityDamagePopOffFX extends EntityTextPopOffFX {

    public EntityDamagePopOffFX(World world, double x, double y, double z, int amount) {
        super(world, String.valueOf(amount), RgbColor.RED, 1.0F, x, y, z, 0.001, 0.07500000000000001, 0.001);
    }
}
