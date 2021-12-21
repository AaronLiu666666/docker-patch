package com.ctos.dockerpatch.model;

import java.util.List;
import java.util.Map;

public class DockerTar {
    private String jsonFile;

    private String manifest;

    private String repositories;

    private Map<String, Layer> layerMap;

    public String getJsonFile() {
        return jsonFile;
    }

    public void setJsonFile(String jsonFile) {
        this.jsonFile = jsonFile;
    }

    public String getManifest() {
        return manifest;
    }

    public void setManifest(String manifest) {
        this.manifest = manifest;
    }

    public String getRepositories() {
        return repositories;
    }

    public void setRepositories(String repositories) {
        this.repositories = repositories;
    }

    public Map<String, Layer> getLayerMap() {
        return layerMap;
    }

    public void setLayerMap(Map<String, Layer> layerMap) {
        this.layerMap = layerMap;
    }
}
