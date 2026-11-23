package com.mrfuzzihead.dynamicsurroundings.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.data.AuroraData;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * The packets are hand-written codecs: every field is read back in the exact order and width it was written with. A
 * mistake there only shows up as garbled effects in a live game, so the wire format is pinned headlessly.
 */
public class PacketCodecTest {

    @Test
    public void rainIntensityRoundTrips() {
        PacketRainIntensity packet = new PacketRainIntensity(0.25F, -1);
        ByteBuf encoded = encode(packet);
        assertEquals(8, encoded.readableBytes());

        // The payload is a float followed by an int.
        ByteBuf wire = copy(encoded);
        assertEquals(0.25F, wire.readFloat(), 0.0F);
        assertEquals(-1, wire.readInt());

        assertBytes(encoded, reencode(copy(encoded), new PacketRainIntensity()));
    }

    @Test
    public void auroraRoundTrips() {
        AuroraData aurora = new AuroraData(-1, 320, -640, 0x1122334455667788L, 3, 2);
        ByteBuf encoded = encode(new PacketAurora(aurora));

        // int dimension, long seed, int x, int z, int colour set, int preset.
        assertEquals(4 + 8 + 4 + 4 + 4 + 4, encoded.readableBytes());

        assertBytes(encoded, reencode(copy(encoded), new PacketAurora()));
    }

    @Test
    public void auroraKeepsNegativeCoordinates() {
        PacketAurora packet = new PacketAurora(7, -1L, -30000000, 30000000, 0, 1);
        ByteBuf encoded = encode(packet);
        assertBytes(encoded, reencode(copy(encoded), new PacketAurora()));
    }

    @Test
    public void reloadHasAnEmptyPayload() {
        ByteBuf encoded = encode(new PacketReload());
        assertEquals(0, encoded.readableBytes());
        assertBytes(encoded, reencode(copy(encoded), new PacketReload()));
    }

    @Test
    public void healthChangeRoundTrips() {
        HealthData data = new HealthData(
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
            1.5F,
            2.5F,
            3.5F,
            true,
            42);
        ByteBuf encoded = encode(new PacketHealthChange(data));

        // UUID as two longs, three floats, a boolean and an int.
        assertEquals(16 + 12 + 1 + 4, encoded.readableBytes());

        assertBytes(encoded, reencode(copy(encoded), new PacketHealthChange()));
    }

    private static ByteBuf encode(IMessage packet) {
        ByteBuf buffer = Unpooled.buffer();
        packet.toBytes(buffer);
        return buffer;
    }

    private static ByteBuf copy(ByteBuf buffer) {
        return Unpooled.copiedBuffer(buffer);
    }

    private static ByteBuf reencode(ByteBuf wire, IMessage empty) {
        empty.fromBytes(wire);
        ByteBuf again = Unpooled.buffer();
        empty.toBytes(again);
        return again;
    }

    private static void assertBytes(ByteBuf expected, ByteBuf actual) {
        assertArrayEquals(toArray(expected), toArray(actual));
    }

    /** Netty 4.0 (the version Forge 1.7.10 ships) has no {@code ByteBufUtil#getBytes}. */
    private static byte[] toArray(ByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), bytes);
        return bytes;
    }
}
