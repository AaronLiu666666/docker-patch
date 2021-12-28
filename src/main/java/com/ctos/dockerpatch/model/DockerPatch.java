package com.ctos.dockerpatch.model;

import java.util.List;
import java.util.Map;

public class DockerPatch {
    /**
     * 新版本镜像中，新增的layer名称
     */
    private List<String> layerToAddList;

    /**
     * 旧版本中依然保留，但需要修改"最后修改时间"的文件信息
     */
    private Map<String, Layer> layerToModifyMap;

    public List<String> getLayerToAddList() {
        return layerToAddList;
    }

    public void setLayerToAddList(List<String> layerToAddList) {
        this.layerToAddList = layerToAddList;
    }

    public Map<String, Layer> getLayerToModifyMap() {
        return layerToModifyMap;
    }

    public void setLayerToModifyMap(Map<String, Layer> layerToModifyMap) {
        this.layerToModifyMap = layerToModifyMap;
    }
}
