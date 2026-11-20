package org.blockartistry.mod.DynSurround.client.fx;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import org.blockartistry.mod.DynSurround.ModOptions;
import org.blockartistry.mod.DynSurround.client.EnvironStateHandler;
import org.blockartistry.mod.DynSurround.client.IClientEffectHandler;
import org.blockartistry.mod.DynSurround.client.sound.SoundEffect;
import org.blockartistry.mod.DynSurround.compat.BlockPos;
import org.blockartistry.mod.DynSurround.compat.MCHelper;
import org.blockartistry.mod.DynSurround.data.BlockRegistry;
import org.blockartistry.mod.DynSurround.util.random.LCGRandom;
import org.blockartistry.mod.DynSurround.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BlockEffectHandler implements IClientEffectHandler {

    private static final Random random = new XorShiftRandom();
    private static final LCGRandom lcg = new LCGRandom();
    private static final double RATIO = 0.0335671847202175;

    private static int randomRange(int range) {
        return lcg.nextInt(range) - lcg.nextInt(range);
    }

    @Override
    public void process(World world, EntityPlayer player) {
        if (!Minecraft.getMinecraft()
            .isGamePaused()) {
            BlockPos playerPos = new BlockPos(player);
            String conditions = EnvironStateHandler.EnvironState.getConditions();
            int RANGE = ModOptions.specialEffectRange;
            int CHECK_COUNT = (int) (Math.pow(RANGE * 2 - 1, 3.0) * 0.0335671847202175);

            for (int i = 0; i < CHECK_COUNT; i++) {
                BlockPos pos = playerPos.add(randomRange(RANGE), randomRange(RANGE), randomRange(RANGE));
                Block block = MCHelper.getBlock(world, pos);
                if (block != Blocks.air) {
                    List<BlockEffect> chain = BlockRegistry.getEffects(block);
                    if (chain != null) {
                        for (BlockEffect effect : chain) {
                            if (effect.trigger(block, world, pos, random)) {
                                effect.doEffect(block, world, pos, random);
                            }
                        }
                    }

                    SoundEffect sound = BlockRegistry.getSound(block, random, conditions);
                    if (sound != null) {
                        sound.doEffect(block, world, pos, random);
                    }
                }
            }

            if (EnvironStateHandler.EnvironState.isPlayerOnGround()
                && EnvironStateHandler.EnvironState.isPlayerMoving()) {
                BlockPos pos = playerPos.down(2);
                Block block = MCHelper.getBlock(world, pos);
                if (block != Blocks.air && !block.getMaterial()
                    .isLiquid()) {
                    SoundEffect sound = BlockRegistry.getStepSound(block, random, conditions);
                    if (sound != null) {
                        sound.doEffect(block, world, pos, random);
                    }
                }
            }
        }
    }

    @Override
    public boolean hasEvents() {
        return false;
    }
}
