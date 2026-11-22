package com.mrfuzzihead.dynamicsurroundings.network;

import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.util.PlayerUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class PacketRainIntensity implements IMessage, IMessageHandler<PacketRainIntensity, IMessage> {

    private float intensity;
    private int dimension;

    public PacketRainIntensity() {}

    public PacketRainIntensity(float intensity, int dimension) {
        this.intensity = intensity;
        this.dimension = dimension;
    }

    public void fromBytes(ByteBuf buf) {
        this.intensity = buf.readFloat();
        this.dimension = buf.readInt();
    }

    public void toBytes(ByteBuf buf) {
        buf.writeFloat(this.intensity);
        buf.writeInt(this.dimension);
    }

    public IMessage onMessage(PacketRainIntensity message, MessageContext ctx) {
        final int dimension = message.dimension;
        final float intensity = message.intensity;
        ClientPacketHandler.schedule(() -> {
            if (dimension == PlayerUtils.getClientPlayerDimension()) {
                Weather.setIntensity(intensity);
            }
        });
        return null;
    }
}
