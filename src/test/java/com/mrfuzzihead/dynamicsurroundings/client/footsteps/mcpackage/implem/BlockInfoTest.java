package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;

import org.junit.Test;

public class BlockInfoTest {

    @Test
    public void equalityAndHashingTolerateNullBlocks() {
        BlockInfo nullBlock = new BlockInfo(null, -1);
        BlockInfo same = new BlockInfo(null, -1);
        BlockInfo different = new BlockInfo(null, 0);

        assertEquals(nullBlock, same);
        assertEquals(nullBlock.hashCode(), same.hashCode());
        assertNotEquals(nullBlock, different);
        assertFalse(nullBlock.equals("not block info"));
    }

    @Test
    public void equalityUsesBlockIdentityAndMetadata() {
        Block stone = new BlockStone();
        assertEquals(new BlockInfo(stone, 1), new BlockInfo(stone, 1));
        assertNotEquals(new BlockInfo(stone, 1), new BlockInfo(stone, 2));
    }
}
