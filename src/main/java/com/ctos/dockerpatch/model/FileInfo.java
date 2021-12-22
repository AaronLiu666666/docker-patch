package com.ctos.dockerpatch.model;

public class FileInfo {
    private String fileName;

    private long lastModifiedTime;

    public FileInfo(String fileName, long lastModifiedTime) {
        this.fileName = fileName;
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
