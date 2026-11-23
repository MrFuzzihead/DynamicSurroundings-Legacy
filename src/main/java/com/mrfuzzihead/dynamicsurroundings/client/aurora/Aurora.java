package com.mrfuzzihead.dynamicsurroundings.client.aurora;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.data.AuroraData;
import com.mrfuzzihead.dynamicsurroundings.data.AuroraPreset;
import com.mrfuzzihead.dynamicsurroundings.data.ColorPair;
import com.mrfuzzihead.dynamicsurroundings.util.MathStuff;
import com.mrfuzzihead.dynamicsurroundings.util.RgbColor;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class Aurora {

    private static final float ANGLE2 = MathStuff.toRadians(12.857142F);
    public float posX;
    public float posZ;
    private Node[][] bands;
    private final long seed;
    private float cycle = 0.0F;
    private int fadeTimer = 0;
    private boolean isAlive = true;
    private int length;
    private float nodeLength;
    private float nodeWidth;
    private int bandOffset;
    private final RgbColor baseColor;
    private final RgbColor fadeColor;
    private int alpha = 1;

    public Aurora(AuroraData data) {
        this(data.posX, data.posZ, data.seed, data.colorSet, data.preset);
    }

    public Aurora(float x, float z, long seed, int colorConfig, int presetConfig) {
        this.seed = seed;
        this.posX = x;
        this.posZ = z;
        ColorPair pair = ColorPair.get(colorConfig);
        this.baseColor = pair.baseColor;
        this.fadeColor = pair.fadeColor;
        this.preset(presetConfig);
        this.generateBands();
        this.translate(0.0F);
    }

    public Node[][] getNodeList() {
        return this.bands;
    }

    private void preset(int preset) {
        AuroraPreset p = AuroraPreset.get(preset);
        this.length = p.length;
        this.nodeLength = p.nodeLength;
        this.nodeWidth = p.nodeWidth;
        this.bandOffset = p.bandOffset;
    }

    public RgbColor getBaseColor() {
        return this.baseColor;
    }

    public RgbColor getFadeColor() {
        return this.fadeColor;
    }

    public int getAlpha() {
        return this.alpha;
    }

    public boolean isAlive() {
        return this.isAlive;
    }

    public void die() {
        if (this.isAlive) {
            this.isAlive = false;
            this.fadeTimer = 0;
        }
    }

    public void update() {
        if (this.fadeTimer < 1280) {
            if (this.fadeTimer % 10 == 0 && this.alpha > 0) {
                this.alpha = this.alpha + (this.isAlive ? 1 : -1);
            }

            this.fadeTimer++;
        }

        if ((this.cycle += 0.75F) >= 360.0F) {
            this.cycle -= 360.0F;
        }
    }

    private void generateBands() {
        this.bands = new Node[ModOptions.auroraMultipleBands ? 3 : 1][];
        Node[] band0 = this.bands[0] = this.populate();
        Node[] band1;
        Node[] band2;
        if (this.bands.length > 1) {
            band1 = this.bands[1] = populateFromTemplate(this.bands[0], this.bandOffset);
            band2 = this.bands[2] = populateFromTemplate(this.bands[0], -this.bandOffset);
        } else {
            band2 = null;
            band1 = null;
        }

        float factor = (float) Math.PI / (this.length / 4);
        int lowerBound = this.length / 8 + 1;
        int upperBound = this.length * 7 / 8 - 1;
        int count = 0;

        for (int i = 0; i < this.length; i++) {
            float width;
            if (i < lowerBound) {
                width = MathStuff.sin(factor * count++) * this.nodeWidth;
            } else if (i > upperBound) {
                width = MathStuff.sin(factor * count--) * this.nodeWidth;
            } else {
                width = this.nodeWidth;
            }

            band0[i].setWidth(width);
            if (band1 != null) {
                band1[i].setWidth(width);
                band2[i].setWidth(width);
            }
        }
    }

    private static Node[] populateFromTemplate(Node[] template, int offset) {
        Node[] tet = new Node[template.length];

        for (int i = 0; i < template.length; i++) {
            tet[i] = new Node(template[i], offset);
        }

        return tet;
    }

    private Node[] populate() {
        Node[] nodeList = new Node[this.length];
        XorShiftRandom nodeRand = new XorShiftRandom(this.seed);
        int bound = this.length / 2 - 1;
        float angleTotal = 0.0F;

        for (int i = this.length / 8 / 2 - 1; i >= 0; i--) {
            float angle = (nodeRand.nextFloat() - 0.5F) * 8.0F;
            angleTotal += angle;
            if (MathStuff.abs(angleTotal) > 180.0F) {
                angle = -angle;
                angleTotal += angle;
            }

            for (int k = 7; k >= 0; k--) {
                int idx = i * 8 + k;
                if (idx == bound) {
                    nodeList[idx] = new Node(0.0F, 7.0F + nodeRand.nextFloat(), 0.0F, angle);
                } else {
                    float y;
                    if (i == 0) {
                        y = MathStuff.sin((float) (Math.PI / 16) * k) * 7.0F + nodeRand.nextFloat() / 2.0F;
                    } else {
                        y = 10.0F + nodeRand.nextFloat() * 5.0F;
                    }

                    Node node = nodeList[idx + 1];
                    float subAngle = node.angle + angle;
                    float subAngleRads = MathStuff.toRadians(subAngle);
                    float z = node.posZ - MathStuff.sin(subAngleRads) * this.nodeLength;
                    float x = node.posX - MathStuff.cos(subAngleRads) * this.nodeLength;
                    nodeList[idx] = new Node(x, y, z, subAngle);
                }
            }
        }

        angleTotal = 0.0F;

        for (int j = this.length / 8 / 2; j < this.length / 8; j++) {
            float angle = (nodeRand.nextFloat() - 0.5F) * 8.0F;
            angleTotal += angle;
            if (MathStuff.abs(angleTotal) > 180.0F) {
                angle = -angle;
                angleTotal += angle;
            }

            for (int h = 0; h < 8; h++) {
                float y;
                if (j == this.length / 8 - 1) {
                    y = MathStuff.cos(ANGLE2 * h) * 7.0F + nodeRand.nextFloat() / 2.0F;
                } else {
                    y = 10.0F + nodeRand.nextFloat() * 5.0F;
                }

                Node node = nodeList[j * 8 + h - 1];
                float subAngle = node.angle + angle;
                float subAngleRads = MathStuff.toRadians(subAngle);
                float z = node.posZ + MathStuff.sin(subAngleRads) * this.nodeLength;
                float x = node.posX + MathStuff.cos(subAngleRads) * this.nodeLength;
                nodeList[j * 8 + h] = new Node(x, y, z, subAngle);
            }
        }

        return nodeList;
    }

    public void translate(float partialTick) {
        Node[] nodeList = this.bands[0];
        Node[] second = null;
        Node[] third = null;
        if (this.bands.length > 1) {
            second = this.bands[1];
            third = this.bands[2];
        }

        float c = this.cycle + 0.75F * partialTick;

        for (int i = 0; i < nodeList.length; i++) {
            float f = MathStuff.cos(MathStuff.toRadians(8.0F * i + c));
            float dZ = f * 18.0F;
            float dY = f * 3.0F;
            Node node = nodeList[i];
            node.setDeltaZ(dZ);
            node.setDeltaY(dY);
            if (second != null) {
                node = second[i];
                node.setDeltaZ(dZ);
                node.setDeltaY(dY);
                node = third[i];
                node.setDeltaZ(dZ);
                node.setDeltaY(dY);
            }
        }

        findAngles(nodeList);
        if (second != null) {
            findAngles(second);
            findAngles(third);
        }
    }

    private static void findAngles(Node[] nodeList) {
        nodeList[0].findAngles(null);

        for (int i = 1; i < nodeList.length - 1; i++) {
            nodeList[i].findAngles(nodeList[i + 1]);
        }

        nodeList[nodeList.length - 1].findAngles(null);
    }
}
