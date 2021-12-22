package com.ctos.dockerpatch.cli;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.file.FileSystemUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ctos.dockerpatch.model.DockerTar;
import com.ctos.dockerpatch.model.FileInfo;
import com.ctos.dockerpatch.model.Layer;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.EntryStreamOffsets;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.*;

/**
 * @ClassName:
 * @Description:
 * @author: jhd
 * @date: 2021/12/21 16:49
 */
public class OtherTest {
    private static final String OLD_IMG_DIR = "C:\\CTOS\\docker-layer-diff\\local\\nginx-1.20.2-alpine.tar";
    private static final String NEW_IMG_DIR = "C:\\CTOS\\docker-layer-diff\\local\\nginx-1.21.1-alpine.tar";
    public static final String outDir = "C:\\CTOS\\docker-layer-diff\\local\\out";

//    public static void main(String[] args) {
//        File file2 = new File(FILE_V2);
//        long lastModifiedTime = file2.lastModified();
//        //1638325262000
//        Date date = new Date(lastModifiedTime);
//        System.out.println(lastModifiedTime);
//
//        File file1 = new File(FILE_V1);
//        System.out.println(file1.lastModified());
//        file1.setLastModified(lastModifiedTime);
//
//    }

//    private static List<File> unTar(final File inputFile, final File outputDir) throws FileNotFoundException, IOException, ArchiveException {
//
//        LOG.info(String.format("Untaring %s to dir %s.", inputFile.getAbsolutePath(), outputDir.getAbsolutePath()));
//
//        final List<File> untaredFiles = new LinkedList<File>();
//        final InputStream is = new FileInputStream(inputFile);
//        final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
//        TarArchiveEntry entry = null;
//        while ((entry = (TarArchiveEntry)debInputStream.getNextEntry()) != null) {
//            final File outputFile = new File(outputDir, entry.getName());
//            if (entry.isDirectory()) {
//                LOG.info(String.format("Attempting to write output directory %s.", outputFile.getAbsolutePath()));
//                if (!outputFile.exists()) {
//                    LOG.info(String.format("Attempting to create output directory %s.", outputFile.getAbsolutePath()));
//                    if (!outputFile.mkdirs()) {
//                        throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
//                    }
//                }
//            } else {
//                LOG.info(String.format("Creating output file %s.", outputFile.getAbsolutePath()));
//                final OutputStream outputFileStream = new FileOutputStream(outputFile);
//                IOUtils.copy(debInputStream, outputFileStream);
//                outputFileStream.close();
//            }
//            untaredFiles.add(outputFile);
//        }
//        debInputStream.close();
//
//        return untaredFiles;
//    }

    public static void main(String[] args) throws IOException, ArchiveException {


        DockerTar dockerTar = new DockerTar();
        dockerTar.setManifest(new FileInfo("manifest.json", 0L));
        dockerTar.setRepositories(new FileInfo("repositories", 0L));

        Map<String, Layer> layerMap = new HashMap<>();

        //解压、记录tar包内容
        FileInputStream oldDirStream = new FileInputStream(OLD_IMG_DIR);
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
                System.out.println("文件:" + name);
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
                    dockerTar.setJsonFile(new FileInfo(name, 0L));
                }
            }
        }
        dockerTar.setLayerMap(layerMap);

        System.out.println(JSONUtil.toJsonPrettyStr(dockerTar));
    }



}
