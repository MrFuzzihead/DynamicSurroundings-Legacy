package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem;

import net.minecraft.block.Block;

public class BlockInfo {

    protected Block block;
    protected int meta;

    protected BlockInfo() {
        this.block = null;
        this.meta = -1;
    }

    public BlockInfo(Block block, int meta) {
        this.block = block;
        this.meta = meta;
    }

    public Block getBlock() {
        return this.block;
    }

    public int getMeta() {
        return this.meta;
    }

    @Override
    public int hashCode() {
        return (this.block == null ? 0 : this.block.hashCode()) ^ this.meta * 31;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof BlockInfo)) {
            return false;
        } else {
            BlockInfo bi = (BlockInfo) o;
            return this.block == bi.block && this.meta == bi.meta;
        }
    }

    public static class BlockInfoMutable extends BlockInfo {

        public BlockInfoMutable() {}

        public BlockInfoMutable(BlockInfo bi) {
            super(bi.block, bi.meta);
        }

        public BlockInfo.BlockInfoMutable setBlock(Block block) {
            this.block = block;
            this.meta = -1;
            return this;
        }

        public BlockInfo.BlockInfoMutable setMeta(int meta) {
            this.meta = meta;
            return this;
        }

        public BlockInfo.BlockInfoMutable asGeneric() {
            this.meta = -1;
            return this;
        }

        public BlockInfo asImmutable() {
            return new BlockInfo(this.block, this.meta);
        }
    }
}
