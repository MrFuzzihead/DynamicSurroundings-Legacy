package com.mrfuzzihead.dynamicsurroundings.network;

import com.mrfuzzihead.dynamicsurroundings.data.RegistryManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class PacketReload implements IMessage, IMessageHandler<PacketReload, IMessage> {

    public PacketReload() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    @Override
    public IMessage onMessage(PacketReload message, MessageContext ctx) {
        ClientPacketHandler.schedule(() -> RegistryManager.reload("biomes", "blocks", "sounds"));
        return null;
    }
}
