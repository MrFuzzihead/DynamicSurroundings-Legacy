package com.mrfuzzihead.dynamicsurroundings.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.EnumFacing;

import org.junit.Test;

/**
 * The 1.7.10 backport of 1.8's BlockPos. The long serialization is the packed form the mod uses to key positions, so
 * a round trip failure would corrupt that mapping silently.
 *
 * <p>
 * Two pieces of 1.8's class are deliberately not exercised: the {@code getAllInBox} iterators and {@code
 * MutableBlockPos#getImmutable}. The iterators compare against the far corner with {@code equals}, which {@link
 * BlockPos} does not override, so they never terminate; and {@code getImmutable} copies through the private fields
 * of the base class, which a mutable instance never sets, so it returns the origin. Nothing in the mod calls either
 * (they were carried over by the decompile), and fixing them is a separate change from adding the test harness.
 * </p>
 */
public class BlockPosTest {

    @Test
    public void constructorFloorsTheCoordinates() {
        BlockPos pos = new BlockPos(1.9D, -0.1D, -1.9D);
        assertEquals(1, pos.getX());
        assertEquals(-1, pos.getY());
        assertEquals(-2, pos.getZ());
    }

    @Test
    public void copyConstructorCopies() {
        BlockPos pos = new BlockPos(4, 5, 6);
        BlockPos copy = new BlockPos(pos);
        assertEquals(4, copy.getX());
        assertEquals(5, copy.getY());
        assertEquals(6, copy.getZ());
    }

    @Test
    public void longSerializationRoundTrips() {
        int[][] coordinates = { { 0, 0, 0 }, { 1, 2, 3 }, { -1, -2, -3 }, { 29999999, 255, -29999999 },
            { -29999999, -255, 29999999 } };
        for (int[] coords : coordinates) {
            BlockPos pos = new BlockPos(coords[0], coords[1], coords[2]);
            BlockPos restored = BlockPos.fromLong(pos.toLong());
            assertEquals("x of " + pos.toLong(), pos.getX(), restored.getX());
            assertEquals("y of " + pos.toLong(), pos.getY(), restored.getY());
            assertEquals("z of " + pos.toLong(), pos.getZ(), restored.getZ());
        }
    }

    @Test
    public void originSerializesToZero() {
        assertEquals(0L, BlockPos.ORIGIN.toLong());
    }

    @Test
    public void distinctPositionsSerializeDistinctly() {
        Set<Long> serialized = new HashSet<Long>();
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    assertTrue(new BlockPos(x, y, z).toLong() + "", serialized.add(new BlockPos(x, y, z).toLong()));
                }
            }
        }
        assertEquals(343, serialized.size());
    }

    @Test
    public void addReturnsThisWhenTheDeltaIsZero() {
        BlockPos pos = new BlockPos(1, 2, 3);
        assertSame(pos, pos.add(0, 0, 0));
        assertSame(pos, pos.add(0.0D, 0.0D, 0.0D));
        assertNotEquals(
            3,
            pos.add(0, 0, 1)
                .getZ());
    }

    @Test
    public void addsOffsets() {
        BlockPos pos = new BlockPos(1, 2, 3).add(10, 20, 30);
        assertEquals(11, pos.getX());
        assertEquals(22, pos.getY());
        assertEquals(33, pos.getZ());

        BlockPos fractional = new BlockPos(1, 2, 3).add(0.5D, 0.5D, 0.5D);
        assertEquals(1, fractional.getX());
        assertEquals(2, fractional.getY());
        assertEquals(3, fractional.getZ());
    }

    @Test
    public void directionalHelpersOffsetTheRightAxis() {
        BlockPos pos = BlockPos.ORIGIN;
        assertEquals(
            1,
            pos.up()
                .getY());
        assertEquals(
            2,
            pos.up(2)
                .getY());
        assertEquals(
            -1,
            pos.down()
                .getY());
        assertEquals(
            EnumFacing.NORTH.getFrontOffsetZ(),
            pos.north()
                .getZ());
        assertEquals(
            EnumFacing.SOUTH.getFrontOffsetZ(),
            pos.south()
                .getZ());
        assertEquals(
            EnumFacing.WEST.getFrontOffsetX(),
            pos.west()
                .getX());
        assertEquals(
            EnumFacing.EAST.getFrontOffsetX(),
            pos.east()
                .getX());
        assertEquals(
            0,
            pos.east(0)
                .getX());
    }

    @Test
    public void offsetFollowsTheFacingVector() {
        BlockPos pos = BlockPos.ORIGIN;
        assertEquals(
            1,
            pos.offset(EnumFacing.UP)
                .getY());
        assertEquals(
            -1,
            pos.offset(EnumFacing.DOWN)
                .getY());
        assertEquals(
            3 * EnumFacing.EAST.getFrontOffsetX(),
            pos.offset(EnumFacing.EAST, 3)
                .getX());
        assertSame(pos, pos.offset(EnumFacing.UP, 0));
    }

    @Test
    public void mutablePositionsCanBeMoved() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(1, 2, 3);
        assertEquals(1, mutable.getX());
        assertEquals(2, mutable.getY());
        assertEquals(3, mutable.getZ());

        assertSame(mutable, mutable.setPos(4, 5, 6));
        assertEquals(4, mutable.getX());
        assertEquals(5, mutable.getY());
        assertEquals(6, mutable.getZ());

        mutable.setPos(7, 8, 9);
        assertEquals(7, mutable.getX());
    }
}
