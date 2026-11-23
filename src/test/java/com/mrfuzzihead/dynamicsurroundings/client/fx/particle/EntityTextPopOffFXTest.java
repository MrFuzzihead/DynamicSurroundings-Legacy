package com.mrfuzzihead.dynamicsurroundings.client.fx.particle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.testutil.WorldTestSupport;

public class EntityTextPopOffFXTest {

    @Test
    public void renderParticleIsARenderlessMarker() {
        World world = mock(World.class);
        WorldTestSupport.setProvider(world, mock(WorldProvider.class));
        EntityDamagePopOffFX effect = new EntityDamagePopOffFX(world, 0.0, 0.0, 0.0, 7);
        try {
            assertTrue(
                EntityTextPopOffFX.active()
                    .contains(effect));
            // The old implementation touched FontRenderer/tessellator state here; the new one must be a no-op.
            effect.renderParticle(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            effect.noClip = true;
            effect.setDead();
            effect.onUpdate();
            assertFalse(
                EntityTextPopOffFX.active()
                    .contains(effect));
        } finally {
            EntityTextPopOffFX.active()
                .remove(effect);
        }
    }
}
