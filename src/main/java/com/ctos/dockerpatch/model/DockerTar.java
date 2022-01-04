package com.ctos.dockerpatch.model;

import java.util.List;
import java.util.Map;

public class DockerTar {
    private FileInfo imageName;

    private FileInfo jsonFile;

    private FileInfo manifest;

    private FileInfo repositories;

    private Map<String, Layer> layerMap;

    public FileInfo getImageName() {
        return imageName;
    }

    public void setImageName(FileInfo imageName) {
        this.imageName = imageName;
    }

    public FileInfo getJsonFile() {
        return jsonFile;
    }

    public void setJsonFile(FileInfo jsonFile) {
        this.jsonFile = jsonFile;
    }

    public FileInfo getManifest() {
        return manifest;
    }

    public void setManifest(FileInfo manifest) {
        this.manifest = manifest;
    }

    public FileInfo getRepositories() {
        return repositories;
    }

    public void setRepositories(FileInfo repositories) {
        this.repositories = repositories;
    }

    public Map<String, Layer> getLayerMap() {
        return layerMap;
    }

    public void setLayerMap(Map<String, Layer> layerMap) {
        this.layerMap = layerMap;
    }
}
