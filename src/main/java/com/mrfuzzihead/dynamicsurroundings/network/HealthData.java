package com.mrfuzzihead.dynamicsurroundings.network;

import java.util.UUID;

import net.minecraft.entity.Entity;

/** Common server-to-client damage/heal payload. */
public final class HealthData {

    public final UUID entityId;
    public final float posX;
    public final float posY;
    public final float posZ;
    public final boolean isCritical;
    public final int amount;

    public HealthData(Entity entity, boolean isCritical, int amount) {
        this.entityId = entity.getUniqueID();
        this.posX = (float) entity.posX;
        this.posY = (float) entity.posY + entity.height;
        this.posZ = (float) entity.posZ;
        this.isCritical = isCritical;
        this.amount = amount;
    }

    public HealthData(UUID id, float x, float y, float z, boolean isCritical, int amount) {
        this.entityId = id;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.isCritical = isCritical;
        this.amount = amount;
    }
}
