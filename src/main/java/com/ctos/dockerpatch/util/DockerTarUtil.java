package com.ctos.dockerpatch.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.SettingUtil;
import com.ctos.dockerpatch.model.DockerPatch;
import com.ctos.dockerpatch.model.DockerTar;
import com.ctos.dockerpatch.model.FileInfo;
import com.ctos.dockerpatch.model.Layer;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class DockerTarUtil {
    public static final int BUFFER_SIZE = 4096;

    public static DockerTar generateDockerTarInfo(String imgPath) {
        DockerTar dockerTar = new DockerTar();
        dockerTar.setManifest(new FileInfo("manifest.json", 0L));
        dockerTar.setRepositories(new FileInfo("repositories", 0L));
        Map<String, Layer> layerMap = new HashMap<>();

        //解压、记录tar包内容
        try {
            FileInputStream oldDirStream = new FileInputStream(imgPath);
            ArchiveInputStream oldTarStream = new ArchiveStreamFactory().createArchiveInputStream("tar", oldDirStream);
            TarArchiveEntry entry = null;
            while((entry = (TarArchiveEntry)oldTarStream.getNextEntry()) != null) {
                String name = entry.getName();
                if(entry.isDirectory()) {
//                    System.out.println("目录:" + name);
                    Layer layer = new Layer();
                    layer.setLayerDir(new FileInfo(name, entry.getLastModifiedDate().getTime()));
                    layer.setLayerTar(new FileInfo(name + "layer.tar", 0L));
                    layer.setLayerVersion(new FileInfo(name + "VERSION", 0L));
                    layer.setLayerJson(new FileInfo(name + "json", 0L));
                    layerMap.put(name, layer);
                } else {
//                    System.out.println("文件:" + name);
                    //先有目录再有文件，所以当遍历到这个文件时，补充最后修改日期的信息即可
                    if(StrUtil.contains(name, "/")) {
                        String layerName = StrUtil.split(name, "/").get(0);
                        String layerFileName = StrUtil.split(name, "/").get(1);
                        if(StrUtil.equals(layerFileName, "layer.tar")) {
                            Layer layer = layerMap.get(layerName + "/");
                            FileInfo layerTar = layer.getLayerTar();
                            layerTar.setLastModifiedTime(entry.getLastModifiedDate().getTime());
                        }
                        if(StrUtil.equals(layerFileName, "VERSION")) {
                            Layer layer = layerMap.get(layerName + "/");
                            FileInfo layerVersion = layer.getLayerVersion();
                            layerVersion.setLastModifiedTime(entry.getLastModifiedDate().getTime());
                        }
                        if(StrUtil.equals(layerFileName, "json")) {
                            Layer layer = layerMap.get(layerName + "/");
                            FileInfo layerJson = layer.getLayerJson();
                            layerJson.setLastModifiedTime(entry.getLastModifiedDate().getTime());
                        }
                    }

                    if(!StrUtil.equals(name, "manifest.json") & StrUtil.contains(name, ".json")) {
                        dockerTar.setJsonFile(new FileInfo(name, entry.getLastModifiedDate().getTime()));
                    }
                }
            }
            dockerTar.setLayerMap(layerMap);
        } catch (IOException ioException) {
            System.out.println("tar文件没找到:" + imgPath);
        } catch (ArchiveException archiveException) {
            System.out.println("读取tar包失败:" + imgPath);
        }

//        System.out.println(JSONUtil.toJsonPrettyStr(dockerTar));
        return dockerTar;
    }

    /**
     *
     * @param oldDockerTar 旧版本docker镜像的DockerTar
     * @param newDockerTar 新版本docker镜像的DockerTar
     * @return
     */
    public static DockerPatch generateDockerPatch(DockerTar oldDockerTar, DockerTar newDockerTar) {
        Map<String, Layer> oldDockerTarLayerMap = oldDockerTar.getLayerMap();
        Map<String, Layer> newDockerTarLayerMap = newDockerTar.getLayerMap();
        Set<String> newDockerTarLayerSet = newDockerTarLayerMap.keySet();
        Set<String> oldDockerTarLayerSet = oldDockerTarLayerMap.keySet();
        List<String> layerToAddList = CollUtil.subtractToList(newDockerTarLayerSet, oldDockerTarLayerSet);
        //todo,新目录的每个文件的"最后修改时间"，都必须记录
        Collection<String> layerToModifyList = CollUtil.intersection(newDockerTarLayerSet, oldDockerTarLayerSet);


        DockerPatch dockerPatch = new DockerPatch();
        dockerPatch.setLayerToAddList(layerToAddList);

        Map<String, Layer> newLayerMap = new HashMap<>();
        for (String newLayerStr : layerToModifyList) {
            Layer layer = newDockerTarLayerMap.get(newLayerStr);
            newLayerMap.put(newLayerStr, layer);
        }
        dockerPatch.setLayerToModifyMap(newLayerMap);
        return dockerPatch;
    }

    public static void unTarLayersToAdd(String imgPath, String patchDir, DockerPatch dockerPatch) {
        List<String> layerToAddList = dockerPatch.getLayerToAddList();

        try {
            FileInputStream dirStream = new FileInputStream(imgPath);
            ArchiveInputStream tarStream = new ArchiveStreamFactory().createArchiveInputStream("tar", dirStream);
            TarArchiveEntry entry = null;
            while((entry = (TarArchiveEntry)tarStream.getNextEntry()) != null) {
                String name = entry.getName();
                for (String layerDirPath : layerToAddList) {
                    if(StrUtil.contains(name, layerDirPath) || StrUtil.contains(name, ".json") ||
                            StrUtil.contains(name, "repositories") ) {
                        if(entry.isDirectory()) {
                            continue;
                        }
                        File outputFile = new File(patchDir + FileUtil.FILE_SEPARATOR + name);
                        if(!outputFile.getParentFile().exists()) {
                            outputFile.getParentFile().mkdirs();
                        }
                        try {
//                            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                            int len = 0;
                            byte[] buffer = new byte[BUFFER_SIZE];
                            while((len = tarStream.read(buffer)) > 0) {
//                                fileOutputStream.write(buffer, 0, len);
                                FileUtil.writeBytes(buffer, outputFile, 0, len, true);
                            }
                        } catch (IOException e) {
                            System.out.println("生成解压后的文件失败，文件名" + name + ", " + e);
                        }
                    }
                }
            }
        } catch (IOException ioException) {
            System.out.println("tar文件没找到:" + imgPath);
        } catch (ArchiveException archiveException) {
            System.out.println("读取tar包失败:" + imgPath);
        }
        //关闭input
    }

    /**
     * 获取文件的最后修改时间
     * @param filePath
     * @return
     */
    private long getLastModified(String filePath) {
        File file = new File(filePath);
        return file.lastModified();
    }

    /**
     * 将filePath2的最后修改时间，改为filePath1的最后修改时间
     * @param filePath1
     * @param filePath2
     */
    private void changeLastModified(String filePath1, String filePath2) {
        File file1 = new File(filePath1);
        long lastModifiedTime = file1.lastModified();

        File file2 = new File(filePath2);
        file2.setLastModified(lastModifiedTime);
    }
}
