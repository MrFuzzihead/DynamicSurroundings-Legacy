package org.blockartistry.mod.DynSurround.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class PotionParticleScrubHandler implements IClientEffectHandler {

    @Override
    public void process(World world, EntityPlayer player) {
        player.getDataWatcher()
            .updateObject(7, 0);
    }

    @Override
    public boolean hasEvents() {
        return false;
    }
}
