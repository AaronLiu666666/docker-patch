package com.ctos.dockerpatch.model;

public class Layer {
    private FileInfo layerDir;

    private FileInfo layerVersion;

    private FileInfo layerJson;

    private FileInfo layerTar;

    public FileInfo getLayerDir() {
        return layerDir;
    }

    public void setLayerDir(FileInfo layerDir) {
        this.layerDir = layerDir;
    }

    public FileInfo getLayerVersion() {
        return layerVersion;
    }

    public void setLayerVersion(FileInfo layerVersion) {
        this.layerVersion = layerVersion;
    }

    public FileInfo getLayerJson() {
        return layerJson;
    }

    public void setLayerJson(FileInfo layerJson) {
        this.layerJson = layerJson;
    }

    public FileInfo getLayerTar() {
        return layerTar;
    }

    public void setLayerTar(FileInfo layerTar) {
        this.layerTar = layerTar;
    }
}
