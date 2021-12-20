package com.ctos.dockerpatch.cli;


import cn.hutool.core.util.StrUtil;

public class CommandLine {

    public static void main(String[] args) {
        //读取入参，v1.0版本入参仅两个，旧、新tar包的path
        if(StrUtil.isEmpty(args[0]) || StrUtil.isEmpty(args[1])) {
            System.out.println("至少需要旧镜像路径、新镜像路径两个参数");
        }
        System.out.println("旧镜像: " + args[0]);
        System.out.println("新镜像: " + args[1]);

        //解压tar包内容

        //比对得到本次更新的文件，并打包成tar包

        //删除解压后的目录
    }
}
