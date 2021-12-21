package com.ctos.dockerpatch.model;

public class Layer {
    private String layerVersion;

    private String layerJson;

    private String layerTar;

    public String getLayerVersion() {
        return layerVersion;
    }

    public void setLayerVersion(String layerVersion) {
        this.layerVersion = layerVersion;
    }

    public String getLayerJson() {
        return layerJson;
    }

    public void setLayerJson(String layerJson) {
        this.layerJson = layerJson;
    }

    public String getLayerTar() {
        return layerTar;
    }

    public void setLayerTar(String layerTar) {
        this.layerTar = layerTar;
    }
}
