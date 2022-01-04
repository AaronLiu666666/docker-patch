package com.ctos.dockerpatch.cli;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ctos.dockerpatch.model.DockerPatch;
import com.ctos.dockerpatch.model.DockerTar;
import com.ctos.dockerpatch.model.FileInfo;
import com.ctos.dockerpatch.util.DockerTarUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.tar.TarUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Cli {
    private static final String PATCH_DIR_NAME = "patch-dir";

    private static final String TAR_PREFIX = "docker-patch-";

    public static void main(String[] args) {
        Options options = new Options();
        //-d C:\CTOS\docker-layer-diff\local\nginx-1.20.2-alpine.tar C:\CTOS\docker-layer-diff\local\nginx-1.21.1-alpine.tar
        options.addOption("d", false, "生成两个版本的docker镜像间的增量更新部分");
        //-m C:\CTOS\docker-layer-diff\local\nginx-1.20.2-alpine.tar C:\CTOS\docker-layer-diff\local\docker-patch-20220104235120.tar
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
                DockerTar oldDockerTar = DockerTarUtil.generateDockerTarInfo(oldImgPath);
                DockerTar newDockerTar = DockerTarUtil.generateDockerTarInfo(newImgPath);
                DockerPatch dockerPatch = DockerTarUtil.generateDockerPatch(oldDockerTar, newDockerTar);
                System.out.println(JSONUtil.toJsonPrettyStr(dockerPatch));

                //解压出必要文件，打包成tar包;将待更新的文件日期、新镜像名存入数据结构
                String parentDir = FileUtil.file(newImgPath).getParentFile().getAbsolutePath();
                String patchDir = parentDir + FileUtil.FILE_SEPARATOR + PATCH_DIR_NAME;
                FileUtil.mkdir(patchDir);
                if(!FileUtil.isDirectory(patchDir)) {
                    System.out.println("创建解压目录失败");
                    return;
                }
                DockerTarUtil.unTarLayersToAdd(newImgPath, patchDir, dockerPatch);
                //将数据结构写入json文件
                String dockerPatchJsonStr = JSONUtil.toJsonPrettyStr(dockerPatch);
                byte[] dockerPatchBytes = StrUtil.bytes(dockerPatchJsonStr);
                FileUtil.writeBytes(dockerPatchBytes, patchDir + FileUtil.FILE_SEPARATOR + "docker-patch.txt");

                //打tar包，删除解压后的目录
                String timestamp = DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss");
                String tarPath = parentDir + FileUtil.FILE_SEPARATOR + TAR_PREFIX + timestamp + ".tar";
                DockerTarUtil.compressToTar(patchDir, tarPath);

                FileUtil.del(patchDir);
            }

            if(cmd.hasOption("m")) {
                String[] cmdArgs = cmd.getArgs();
                if(cmdArgs.length != 2) {
                    System.out.println("入参必须有两个，第一个是旧镜像路径，第二个是patch路径");
                    return;
                }

                System.out.println("-m, 开始生成新镜像" + cmdArgs[0] + ", " + cmdArgs[1]);
                String oldImgPath = cmdArgs[0];
                String patchPath = cmdArgs[1];
                //将patch内容解压，生成新目录，放入相关文件
                String parentDir = FileUtil.file(oldImgPath).getParentFile().getAbsolutePath();
                String patchDir = parentDir + FileUtil.FILE_SEPARATOR + PATCH_DIR_NAME;
                DockerTarUtil.unTar(patchPath, patchDir);

                //读取docker-patch.txt
                String dockerPatchJsonStr = FileUtil.readString(new File(patchDir + FileUtil.FILE_SEPARATOR + "docker-patch.txt"), StandardCharsets.UTF_8);
                DockerPatch dockerPatch = JSONUtil.toBean(dockerPatchJsonStr, DockerPatch.class);

                //根据解压旧镜像tar
                List<String> layerToDeleteList = dockerPatch.getLayerToDeleteList();
                DockerTarUtil.unTarToDir(oldImgPath, patchDir, layerToDeleteList);

                //修改文件日期，生成tar包
                DockerTar newDockerTar = dockerPatch.getNewDockerTar();
                DockerTarUtil.modifyFileLastModified(patchDir, newDockerTar);
                FileUtil.del(patchDir + FileUtil.FILE_SEPARATOR + "docker-patch.txt");

                //生成tar包
                FileInfo imageFileInfo = newDockerTar.getImageName();
                String imageName = imageFileInfo.getFileName();
                long lastModifiedTime = imageFileInfo.getLastModifiedTime();
                String tarPath = parentDir + FileUtil.FILE_SEPARATOR + imageName;
                DockerTarUtil.compressToTar(patchDir, tarPath);
                DockerTarUtil.changeLastModified(lastModifiedTime, tarPath);


                FileUtil.del(patchDir);
            }
        } catch (ParseException e) {
            System.out.println("解析入参失败:" + e);
        } catch (FileNotFoundException e) {
            System.out.println("生成tar包失败:" + e);
        } catch (Exception e) {
            System.out.println("发生错误:" + e);
        }
    }


}
