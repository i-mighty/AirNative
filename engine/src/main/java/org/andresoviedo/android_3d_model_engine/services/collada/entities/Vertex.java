package org.andresoviedo.android_3d_model_engine.services.collada.entities;

public class Vertex {

    private static final int NO_INDEX = -1;
    private final float length;
    private float[] position;
    private int textureIndex = NO_INDEX;
    private int normalIndex = NO_INDEX;
    private int colorIndex = NO_INDEX;
    private VertexSkinData weightsData;

    public Vertex(float[] position) {
        this.position = position;
        this.length = position.length;
    }

    public VertexSkinData getWeightsData() {
        return weightsData;
    }

    public void setWeightsData(VertexSkinData weightsData) {
        this.weightsData = weightsData;
    }

    public float getLength() {
        return length;
    }

    public float[] getPosition() {
        return position;
    }

    public void setPosition(float[] position) {
        this.position = position;
    }

    public int getTextureIndex() {
        return textureIndex;
    }

    public void setTextureIndex(int textureIndex) {
        this.textureIndex = textureIndex;
    }

    public int getNormalIndex() {
        return normalIndex;
    }

    public void setNormalIndex(int normalIndex) {
        this.normalIndex = normalIndex;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }
}
