package com.ctos.dockerpatch.cli;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ctos.dockerpatch.model.DockerTar;
import com.ctos.dockerpatch.model.FileInfo;
import com.ctos.dockerpatch.model.Layer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cli {
    private static final String OLD_IMG_DIR = "./old-img-dir";
    private static final String NEW_IMG_DIR = "./new-img-dir";

    public void main(String[] args) {
        Options options = new Options();
        options.addOption("d", false, "生成两个版本的docker镜像间的增量更新部分");
        options.addOption("m", false, "将老镜像和patch合并成新镜像");

        try {
            DefaultParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            if(cmd.hasOption("d")) {
                String[] cmdArgs = cmd.getArgs();
                if(cmdArgs.length != 2) {
                    System.out.println("入参必须有两个，第一个是旧镜像路径，第二个是新镜像路径");
                    return;
                }

                String oldImgPath = cmdArgs[0];
                String newImgPath = cmdArgs[1];
                System.out.println("-d, 开始生成增量补丁" + oldImgPath + ", " + newImgPath);
                //读取新旧tar包的内容，比对出需要更新的文件
                DockerTar oldDockerTar = generateDockerTarInfo(oldImgPath);
                DockerTar newDockerTar = generateDockerTarInfo(newImgPath);

                //解压出必要文件，打包成tar包;将待更新的文件日期、新镜像名存入数据结构

                //将数据结构写入json文件

                //打tar包，删除解压后的目录
            }

            if(cmd.hasOption("m")) {
                String[] cmdArgs = cmd.getArgs();
                if(cmdArgs.length != 2) {
                    System.out.println("入参必须有两个，第一个是旧镜像路径，第二个是patch路径");
                    return;
                }

                System.out.println("-m, 开始生成新镜像" + cmdArgs[0] + ", " + cmdArgs[1]);
                //解压旧镜像tar

                //将patch内容解压，生成新目录，放入相关文件

                //修改文件日期，生成tar包
            }
        } catch (ParseException e) {
            System.out.println("解析入参失败:" + e);
        }
    }

    private DockerTar generateDockerTarInfo(String imgPath) {
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
                    System.out.println("目录:" + name);
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
                            layer.setLayerTar(layerTar);
                            layerMap.put(layerFileName, layer);
                        }
                        if(StrUtil.equals(layerFileName, "VERSION")) {
                            Layer layer = layerMap.get(layerName + "/");
                            FileInfo layerVersion = layer.getLayerVersion();
                            layerVersion.setLastModifiedTime(entry.getLastModifiedDate().getTime());
                            layer.setLayerVersion(layerVersion);
                            layerMap.put(layerFileName, layer);
                        }
                        if(StrUtil.equals(layerFileName, "json")) {
                            Layer layer = layerMap.get(layerName + "/");
                            FileInfo layerJson = layer.getLayerJson();
                            layerJson.setLastModifiedTime(entry.getLastModifiedDate().getTime());
                            layer.setLayerJson(layerJson);
                            layerMap.put(layerFileName, layer);
                        }
                    }

                    if(!StrUtil.equals(name, "manifest.json") & StrUtil.contains(name, ".json")) {
                        dockerTar.setJsonFile(new FileInfo(name, entry.getLastModifiedDate().getTime()));
                    }
                }
            }
            dockerTar.setLayerMap(layerMap);
        } catch (IOException ioException) {
            System.out.println("文件没找到:" + imgPath);
        } catch (ArchiveException archiveException) {
            System.out.println("读取tar包失败:" + imgPath);
        }

        System.out.println(JSONUtil.toJsonPrettyStr(dockerTar));
        return dockerTar;
    }

    private long getLastModified(String filePath) {
        File file2 = new File(filePath);
        return file2.lastModified();
    }

    /**
     * 将filePath2的最后修改时间，改为filePath1的最后修改时间
     * @param filePath1
     * @param filePath2
     */
    private void changeLastModified(String filePath1, String filePath2) {
        File file2 = new File(filePath1);
        long lastModifiedTime = file2.lastModified();

        File file1 = new File(filePath2);
        file1.setLastModified(lastModifiedTime);
    }
}
