package org.dennissoftman.format.bigf;

public class BigfInfo {
    public static final int MAGIC_NUMBER = 0x42494746;
    private int magicNumber; // must be 'BIGF' (0x42494746)
    private int archiveSize; // BIG archive size
    private int fileCount; // file count inside archive
    private int dataStart;   // base data addr (could be used for caching)

    public void setMagicNumber(int val)
    {
        magicNumber = val;
    }

    public int getMagicNumber()
    {
        return magicNumber;
    }

    public int getSize()
    {
        return archiveSize;
    }

    public void setSize(int size)
    {
        archiveSize = size;
    }

    public void setFileCount(int cnt)
    {
        fileCount = cnt;
    }

    public int getFileCount()
    {
        return fileCount;
    }

    public void setDataStart(int start)
    {
        dataStart = start;
    }

    public int getDataStart()
    {
        return dataStart;
    }

    public boolean isCorrect()
    {
        if(magicNumber != MAGIC_NUMBER)
            return false;
        if(archiveSize <= 0)
            return false;
        if(fileCount <= 0)
            return false;
        if(dataStart <= 0)
            return false;
        return true;
    }

    public String toDebugString()
    {
        return String.format("magic: %d, archSize: %d, fileCount: %d, baseAddr: %d",
                magicNumber, archiveSize, fileCount, dataStart);
    }
}
