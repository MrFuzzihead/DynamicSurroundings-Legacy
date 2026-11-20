package org.blockartistry.mod.DynSurround.client.fx;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import org.blockartistry.mod.DynSurround.client.fx.particle.EntityBubbleJetFX;
import org.blockartistry.mod.DynSurround.client.fx.particle.EntityDustJetFX;
import org.blockartistry.mod.DynSurround.client.fx.particle.EntityFireJetFX;
import org.blockartistry.mod.DynSurround.client.fx.particle.EntityFountainJetFX;
import org.blockartistry.mod.DynSurround.client.fx.particle.EntityJetFX;
import org.blockartistry.mod.DynSurround.client.fx.particle.EntitySteamJetFX;
import org.blockartistry.mod.DynSurround.compat.BlockPos;

public abstract class JetEffect extends BlockEffect {

    private static final int MAX_STRENGTH = 10;

    private static int countBlocks(World world, BlockPos pos, Block block, int dir) {
        int count = 0;
        int idx = pos.getY();

        while (count < 10) {
            if (world.getBlock(pos.getX(), idx, pos.getZ()) != block) {
                return count;
            }

            count++;
            idx += dir;
        }

        return count;
    }

    private static double jetSpawnHeight(World world, BlockPos pos) {
        int meta = world.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ());
        return 1.1 - BlockLiquid.getLiquidHeightPercent(meta) + pos.getY();
    }

    public JetEffect(int chance) {
        super(chance);
    }

    protected void addEffect(EntityJetFX fx) {
        Minecraft.getMinecraft().effectRenderer.addEffect(fx);
        fx.playSound();
    }

    public static class Bubble extends JetEffect {

        public Bubble(int chance) {
            super(chance);
        }

        @Override
        public boolean trigger(Block block, World world, BlockPos pos, Random random) {
            return super.trigger(block, world, pos, random) && world.getBlock(pos.getX(), pos.getY() - 1, pos.getZ())
                .getMaterial()
                .isSolid();
        }

        @Override
        public void doEffect(Block block, World world, BlockPos pos, Random random) {
            int waterBlocks = JetEffect.countBlocks(world, pos, block, 1);
            EntityJetFX effect = new EntityBubbleJetFX(
                waterBlocks,
                world,
                pos.getX() + 0.5,
                pos.getY() + 0.1,
                pos.getZ() + 0.5);
            this.addEffect(effect);
        }
    }

    public static class Dust extends JetEffect {

        public Dust(int chance) {
            super(chance);
        }

        @Override
        public boolean trigger(Block block, World world, BlockPos pos, Random random) {
            return super.trigger(block, world, pos, random) && world.isAirBlock(pos.getX(), pos.getY() - 1, pos.getZ());
        }

        @Override
        public void doEffect(Block block, World world, BlockPos pos, Random random) {
            EntityJetFX effect = new EntityDustJetFX(
                2,
                world,
                pos.getX() + 0.5,
                pos.getY() - 0.2,
                pos.getZ() + 0.5,
                block);
            this.addEffect(effect);
        }
    }

    public static class Fire extends JetEffect {

        public Fire(int chance) {
            super(chance);
        }

        @Override
        public boolean trigger(Block block, World world, BlockPos pos, Random random) {
            return super.trigger(block, world, pos, random) && world.isAirBlock(pos.getX(), pos.getY() + 1, pos.getZ());
        }

        @Override
        public void doEffect(Block block, World world, BlockPos pos, Random random) {
            int lavaBlocks = JetEffect.countBlocks(world, pos, block, -1);
            double spawnHeight = JetEffect.jetSpawnHeight(world, pos);
            EntityJetFX effect = new EntityFireJetFX(
                lavaBlocks,
                world,
                pos.getX() + 0.5,
                spawnHeight,
                pos.getZ() + 0.5);
            this.addEffect(effect);
        }
    }

    public static class Fountain extends JetEffect {

        public Fountain(int chance) {
            super(chance);
        }

        @Override
        public boolean trigger(Block block, World world, BlockPos pos, Random random) {
            return super.trigger(block, world, pos, random) && world.isAirBlock(pos.getX(), pos.getY() + 1, pos.getZ());
        }

        @Override
        public void doEffect(Block block, World world, BlockPos pos, Random random) {
            EntityJetFX effect = new EntityFountainJetFX(
                5,
                world,
                pos.getX() + 0.5,
                pos.getY() + 1.1,
                pos.getZ() + 0.5,
                block);
            this.addEffect(effect);
        }
    }

    public static class Steam extends JetEffect {

        public Steam(int chance) {
            super(chance);
        }

        protected int lavaCount(World world, BlockPos pos) {
            int blockCount = 0;

            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    for (int k = -1; k <= 1; k++) {
                        if (world.getBlock(pos.getX() + i, pos.getY() + j, pos.getZ() + k) == Blocks.lava) {
                            blockCount++;
                        }
                    }
                }
            }

            return blockCount;
        }

        @Override
        public boolean trigger(Block block, World world, BlockPos pos, Random random) {
            return super.trigger(block, world, pos, random) && world.isAirBlock(pos.getX(), pos.getY() + 1, pos.getZ())
                ? this.lavaCount(world, pos) != 0
                : false;
        }

        @Override
        public void doEffect(Block block, World world, BlockPos pos, Random random) {
            int strength = this.lavaCount(world, pos);
            double spawnHeight = JetEffect.jetSpawnHeight(world, pos);
            EntityJetFX effect = new EntitySteamJetFX(strength, world, pos.getX() + 0.5, spawnHeight, pos.getZ() + 0.5);
            this.addEffect(effect);
        }
    }
}
