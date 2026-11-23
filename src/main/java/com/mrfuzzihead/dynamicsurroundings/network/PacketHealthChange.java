package com.mrfuzzihead.dynamicsurroundings.network;

import java.util.UUID;

import com.mrfuzzihead.dynamicsurroundings.client.DamageEffectHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketHealthChange implements IMessage, IMessageHandler<PacketHealthChange, IMessage> {

    private UUID entityId;
    private float posX;
    private float posY;
    private float posZ;
    private boolean isCritical;
    private int amount;

    public PacketHealthChange() {}

    public PacketHealthChange(HealthData data) {
        this.entityId = data.entityId;
        this.posX = data.posX;
        this.posY = data.posY;
        this.posZ = data.posZ;
        this.isCritical = data.isCritical;
        this.amount = data.amount;
    }

    public IMessage onMessage(PacketHealthChange message, MessageContext ctx) {
        final HealthData data = new HealthData(
            message.entityId,
            message.posX,
            message.posY,
            message.posZ,
            message.isCritical,
            message.amount);
        ClientPacketHandler.schedule(() -> DamageEffectHandler.handleEvent(data));
        return null;
    }

    public void fromBytes(ByteBuf buf) {
        this.entityId = new UUID(buf.readLong(), buf.readLong());
        this.posX = buf.readFloat();
        this.posY = buf.readFloat();
        this.posZ = buf.readFloat();
        this.isCritical = buf.readBoolean();
        this.amount = buf.readInt();
    }

    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.entityId.getMostSignificantBits());
        buf.writeLong(this.entityId.getLeastSignificantBits());
        buf.writeFloat(this.posX);
        buf.writeFloat(this.posY);
        buf.writeFloat(this.posZ);
        buf.writeBoolean(this.isCritical);
        buf.writeInt(this.amount);
    }
}
