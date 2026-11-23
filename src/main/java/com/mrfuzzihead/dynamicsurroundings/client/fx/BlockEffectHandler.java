package com.mrfuzzihead.dynamicsurroundings.client.fx;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironmentSnapshot;
import com.mrfuzzihead.dynamicsurroundings.client.IClientEffectHandler;
import com.mrfuzzihead.dynamicsurroundings.client.sound.SoundEffect;
import com.mrfuzzihead.dynamicsurroundings.compat.BlockPos;
import com.mrfuzzihead.dynamicsurroundings.compat.MCHelper;
import com.mrfuzzihead.dynamicsurroundings.data.BlockRegistry;
import com.mrfuzzihead.dynamicsurroundings.util.random.LCGRandom;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BlockEffectHandler implements IClientEffectHandler {

    private static final Random random = new XorShiftRandom();
    private static final LCGRandom lcg = new LCGRandom();
    private static final double RATIO = 0.0335671847202175;
    private static int cachedRange = Integer.MIN_VALUE;
    private static int cachedCheckCount;
    private static final int SAMPLE_SLICES = 2;

    private static int randomRange(int range) {
        return lcg.nextInt(range) - lcg.nextInt(range);
    }

    static int checkCountForRange(int range) {
        if (range != cachedRange) {
            cachedRange = range;
            cachedCheckCount = (int) (Math.pow(range * 2 - 1, 3.0) * RATIO);
        }

        return cachedCheckCount;
    }

    static int sampleCountForRange(int range) {
        return Math.max(1, checkCountForRange(range) / SAMPLE_SLICES);
    }

    @Override
    public void process(World world, EntityPlayer player) {
        if (!Minecraft.getMinecraft()
            .isGamePaused() && BlockRegistry.hasEffectsOrSounds()) {
            BlockPos playerPos = new BlockPos(player);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            EnvironmentSnapshot snapshot = EnvironStateHandler.EnvironState.getSnapshot();
            String conditions = snapshot.getConditions();
            int range = ModOptions.specialEffectRange;
            int checkCount = sampleCountForRange(range);

            for (int i = 0; i < checkCount; i++) {
                pos.setPos(
                    playerPos.getX() + randomRange(range),
                    playerPos.getY() + randomRange(range),
                    playerPos.getZ() + randomRange(range));
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

            if (snapshot.isPlayerOnGround() && snapshot.isPlayerMoving()) {
                BlockPos stepPos = stepBlockPos(player);
                Block block = MCHelper.getBlock(world, stepPos);
                if (block != Blocks.air && !block.getMaterial()
                    .isLiquid()) {
                    SoundEffect sound = BlockRegistry.getStepSound(block, random, conditions);
                    if (sound != null) {
                        sound.doEffect(block, world, stepPos, random);
                    }
                }
            }
        }
    }

    static BlockPos stepBlockPos(EntityPlayer player) {
        return new BlockPos(player.posX, player.posY - 0.1D, player.posZ);
    }

    @Override
    public boolean hasEvents() {
        return false;
    }
}
