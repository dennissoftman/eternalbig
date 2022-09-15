package org.dennissoftman.format.bigf;

public class BigfFileInfo {
    private int fileOffset; // offset from archive start
    private int fileSize=-1;   // file data size
    private String fileName;

    public void setFileOffset(int off)
    {
        fileOffset = off;
    }

    public int getFileOffset()
    {
        return fileOffset;
    }

    public void setFileSize(int size)
    {
        fileSize = size;
    }

    public int getFileSize()
    {
        return fileSize;
    }

    public void setFileName(String name)
    {
        fileName = name;
    }

    public String getFileName()
    {
        return fileName.replace('\\', '/');
    }

    @Override
    public String toString() {
        return getFileName();
    }

    public boolean isCorrect()
    {
        if(fileOffset <= 0)
            return false;
        if(fileSize < 0) // allow empty files
            return false;
        if(fileName.length() == 0)
            return false;
        return true;
    }

    public String toDebugString()
    {
        return String.format("dataAddr: %d, fileSize: %d, fileName: %s", fileOffset, fileSize, fileName);
    }
}
