package com.mrfuzzihead.dynamicsurroundings.client.aurora;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NodeTest {

    @Test
    public void moddedCoordinatesApplyTheDeltaAndClampY() {
        Node node = new Node(1.0F, 2.0F, 3.0F, 45.0F);
        node.setDeltaY(-5.0F);
        node.setDeltaZ(4.0F);

        assertEquals(0.0F, node.getModdedY(), 0.0F);
        assertEquals(7.0F, node.getModdedZ(), 0.0F);
    }

    @Test
    public void findAnglesBuildsTheQuadFromTheNextNode() {
        Node node = new Node(1.0F, 2.0F, 3.0F, 0.0F);
        Node next = new Node(4.0F, 2.0F, 6.0F, 0.0F);
        node.setWidth(2.0F);
        node.setDeltaZ(1.0F);

        node.findAngles(next);

        assertEquals(1.0F, node.tetX, 1.0e-5F);
        assertEquals(1.0F, node.tetX2, 1.0e-5F);
        assertEquals(6.0F, node.tetZ, 1.0e-5F);
        assertEquals(2.0F, node.tetZ2, 1.0e-5F);
    }
}
