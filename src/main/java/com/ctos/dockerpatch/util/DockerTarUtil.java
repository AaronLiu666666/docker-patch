package com.ctos.dockerpatch.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ctos.dockerpatch.model.DockerPatch;
import com.ctos.dockerpatch.model.DockerTar;
import com.ctos.dockerpatch.model.FileInfo;
import com.ctos.dockerpatch.model.Layer;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.*;
import java.util.*;

public class DockerTarUtil {
    private static final String PATCH_DIR_NAME = "patch-dir";

    public static final int BUFFER_SIZE = 4096;

    public static DockerTar generateDockerTarInfo(String imgPath) {
        File image = new File(imgPath);
        DockerTar dockerTar = new DockerTar();
        dockerTar.setImageName(new FileInfo(image.getName(), image.lastModified()));
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
        List<String> layerToDeleteList = CollUtil.subtractToList(oldDockerTarLayerSet, newDockerTarLayerSet);
        //todo,新目录的每个文件的"最后修改时间"，都必须记录
        Collection<String> layerToModifyList = CollUtil.intersection(newDockerTarLayerSet, oldDockerTarLayerSet);


        DockerPatch dockerPatch = new DockerPatch();
        dockerPatch.setLayerToAddList(layerToAddList);
        dockerPatch.setLayerToDeleteList(layerToDeleteList);
        dockerPatch.setNewDockerTar(newDockerTar);

        return dockerPatch;
    }

    /**
     * 将目录压缩为tar包
     * @param dirPath
     * @param tarPath
     * @throws IOException
     */
    public static void compressToTar(String dirPath, String tarPath) throws IOException {
        //获取这个路径包含的所有文件list，忽略目录
        List<String> files = getFileListByDir(dirPath);
        if (files != null) {
            //将所有文件打进tar包
            FileOutputStream fileOutputStream = new FileOutputStream(tarPath);
            TarArchiveOutputStream out = new TarArchiveOutputStream(fileOutputStream);
            out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);

            for (String file : files) {
                List<String> split = StrUtil.split(file, PATCH_DIR_NAME);
                out.putArchiveEntry(new TarArchiveEntry(new File(file), split.get(1)));
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));

                int len = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                while((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
                in.close();
                out.closeArchiveEntry();
                System.out.println(file);
            }
            out.finish();
        }
    }

    /**
     * 获取指定目录下的文件列表（不包括目录）
     * @param dirPath
     * @return
     */
    private static List<String> getFileListByDir(String dirPath) {
        List<String> fileList = new ArrayList<>();
        File file = new File(dirPath);
        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if(f.isDirectory()) {
                List<String> fileListByChildDir = getFileListByDir(f.getAbsolutePath());
                CollUtil.addAll(fileList, fileListByChildDir);
            }
            if (f.isFile()) {
                fileList.add(f.getAbsolutePath());
            }
        }
        return fileList;
    }

    /**
     * 解压tar包至destDir
     * @param imgPath
     * @param destDir
     */
    public static void unTar(String imgPath, String destDir) {
        try {
            FileInputStream dirStream = new FileInputStream(imgPath);
            ArchiveInputStream tarStream = new ArchiveStreamFactory().createArchiveInputStream("tar", dirStream);
            TarArchiveEntry entry = null;
            while((entry = (TarArchiveEntry)tarStream.getNextEntry()) != null) {
                String name = entry.getName();
                if(entry.isDirectory()) {
                    continue;
                }
                File outputFile = new File(destDir + FileUtil.FILE_SEPARATOR + name);
                if(!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                try {
                    int len = 0;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while((len = tarStream.read(buffer)) > 0) {
                        FileUtil.writeBytes(buffer, outputFile, 0, len, true);
                    }
                } catch (IOException e) {
                    System.out.println("生成解压后的文件失败，文件名" + name + ", " + e);
                }
        }
        } catch (IOException ioException) {
            System.out.println("tar文件没找到:" + imgPath);
        } catch (ArchiveException archiveException) {
            System.out.println("读取tar包失败:" + imgPath);
        }
    }

    /**
     * 解压至特定目录，不包括list中的目录，也不包括.json、repositories、manifest.json
     * @param imgPath
     * @param destDir
     * @param filesToDeleteList
     */
    public static void unTarToDir(String imgPath, String destDir, List<String> filesToDeleteList) {
        try {
            FileInputStream dirStream = new FileInputStream(imgPath);
            ArchiveInputStream tarStream = new ArchiveStreamFactory().createArchiveInputStream("tar", dirStream);
            TarArchiveEntry entry = null;
            while((entry = (TarArchiveEntry)tarStream.getNextEntry()) != null) {
                String name = entry.getName();
                if(checkFileInList(name, filesToDeleteList)  || StrUtil.contains(name, ".json") ||
                        StrUtil.contains(name, "repositories")) {
                    continue;
                }
                if(entry.isDirectory()) {
                    continue;
                }
                File outputFile = new File(destDir + FileUtil.FILE_SEPARATOR + name);
                if(!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                try {
                    int len = 0;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while((len = tarStream.read(buffer)) > 0) {
                        FileUtil.writeBytes(buffer, outputFile, 0, len, true);
                    }
                } catch (IOException e) {
                    System.out.println("生成解压后的文件失败，文件名" + name + ", " + e);
                }
            }
        } catch (IOException ioException) {
            System.out.println("tar文件没找到:" + imgPath);
        } catch (ArchiveException archiveException) {
            System.out.println("读取tar包失败:" + imgPath);
        }
    }

    /**
     * 从新版本docker镜像的tar包中，解压出新增的layer
     * @param imgPath
     * @param patchDir
     * @param dockerPatch
     */
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
     * 根据dockerTar中的"最后修改时间"，修改patchDir中的文件
     * @param destDir: patch-dir的绝对路径
     * @param dockerTar
     */
    public static void modifyFileLastModified(String destDir, DockerTar dockerTar) {
        //修改jsonFile
        FileInfo jsonFile = dockerTar.getJsonFile();
        changeLastModified(jsonFile.getLastModifiedTime(), destDir + FileUtil.FILE_SEPARATOR + jsonFile.getFileName());

        //修改manifest
        FileInfo manifest = dockerTar.getManifest();
        changeLastModified(manifest.getLastModifiedTime(), destDir + FileUtil.FILE_SEPARATOR + manifest.getFileName());

        //修改repositories
        FileInfo repositories = dockerTar.getRepositories();
        changeLastModified(repositories.getLastModifiedTime(), destDir + FileUtil.FILE_SEPARATOR + repositories.getFileName());

        //修改layers
        Map<String, Layer> layerMap = dockerTar.getLayerMap();
        layerMap.forEach((dirPath, layer) -> {
            FileInfo layerDir = layer.getLayerDir();
            FileInfo layerTar = layer.getLayerTar();
            FileInfo layerJson = layer.getLayerJson();
            FileInfo layerVersion = layer.getLayerVersion();
            changeLastModified(layerDir.getLastModifiedTime(), destDir + FileUtil.FILE_SEPARATOR + layerDir.getFileName());
            changeLastModified(layerTar.getLastModifiedTime(), destDir + FileUtil.FILE_SEPARATOR + layerTar.getFileName());
            changeLastModified(layerJson.getLastModifiedTime(), destDir + FileUtil.FILE_SEPARATOR + layerJson.getFileName());
            changeLastModified(layerVersion.getLastModifiedTime(), destDir + FileUtil.FILE_SEPARATOR + layerVersion.getFileName());
        });
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
     * 检查字符串name中，是否包含list中某个String
     * @param name
     * @param filesToDeleteList
     * @return
     */
    private static boolean checkFileInList(String name, List<String> filesToDeleteList) {
        for (String s : filesToDeleteList) {
            if(name.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 修改文件的"最后修改时间"
     * @param lastModifiedTime
     * @param filePath2
     */
    public static void changeLastModified(long lastModifiedTime, String filePath2) {
        File file2 = new File(filePath2);
        file2.setLastModified(lastModifiedTime);
    }
}
