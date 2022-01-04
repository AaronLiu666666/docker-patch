package com.ctos.dockerpatch.model;

import java.util.List;

public class DockerPatch {
    /**
     * 新版本镜像中，新增的layer名称
     */
    private List<String> layerToAddList;

    /**
     * 旧版本中有，但新版本中没有的layer名称（需要删除）
     */
    private List<String> layerToDeleteList;

    /**
     * 新docker镜像中文件的"最后修改时间"
     */
    private DockerTar newDockerTar;

    public List<String> getLayerToAddList() {
        return layerToAddList;
    }

    public void setLayerToAddList(List<String> layerToAddList) {
        this.layerToAddList = layerToAddList;
    }

    public List<String> getLayerToDeleteList() {
        return layerToDeleteList;
    }

    public void setLayerToDeleteList(List<String> layerToDeleteList) {
        this.layerToDeleteList = layerToDeleteList;
    }

    public DockerTar getNewDockerTar() {
        return newDockerTar;
    }

    public void setNewDockerTar(DockerTar newDockerTar) {
        this.newDockerTar = newDockerTar;
    }

}
