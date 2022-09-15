package org.dennissoftman.format.bigf;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

public class BigfReader {
    /*
    4 bytes magic number 'BIGF'
    4 bytes LE BIG size
    4 bytes BE file count
    4 bytes BE data start (from file start)
    --- file structure
    4 bytes BE file data start
    4 bytes BE file data size
    nul-terminated file name
    * */
    public static final int ARCH_END_MARK = 0x4C323331; // L321

    public List<BigfFileInfo> getFileInfos(InputStream dataStream) throws IOException
    {
        List<BigfFileInfo> files = new LinkedList<>();
        BigfInfo archInfo = readBigInfo(dataStream);
        for(int i=0; i < archInfo.getFileCount(); i++)
        {
            BigfFileInfo fInfo = readFileInfo(dataStream, archInfo);
            if(!fInfo.isCorrect())
                throw new IOException("Archive is corrupted: " + fInfo.toDebugString());

            files.add(fInfo);
        }
        return files;
    }

    private BigfInfo readBigInfo(InputStream rd) throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate(16);
        BigfInfo bInfo = new BigfInfo();

        if(rd.read(buf.array(), 0, 4) != 4)
            throw new IOException("Failed to read magic number");
        bInfo.setMagicNumber(buf.getInt(0));

        if(rd.read(buf.array(), 0, 4) != 4)
            throw new IOException("Failed to read archive size");
        bInfo.setSize(buf.order(ByteOrder.LITTLE_ENDIAN).getInt(0));

        if(rd.read(buf.array(), 0, 4) != 4)
            throw new IOException("Failed to read file count");
        bInfo.setFileCount(buf.order(ByteOrder.BIG_ENDIAN).getInt(0));

        if(rd.read(buf.array(), 0, 4) != 4)
            throw new IOException("Failed to read base data addr");
        bInfo.setDataStart(buf.order(ByteOrder.BIG_ENDIAN).getInt(0));

        return bInfo;
    }

    private BigfFileInfo readFileInfo(InputStream rd, BigfInfo bigInfo) throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate(16);
        BigfFileInfo info = new BigfFileInfo();

        if(rd.read(buf.array(), 0, 4) != 4) // file data offset
            throw new IOException("Failed to read file data offset");
        if(buf.getInt(0) == ARCH_END_MARK) // L321
            throw new IOException("End of archive");
        info.setFileOffset(buf.order(ByteOrder.BIG_ENDIAN).getInt(0));

        if(rd.read(buf.array(), 0, 4) != 4) // file data size
            throw new IOException("Failed to read file data size");
        info.setFileSize(buf.order(ByteOrder.BIG_ENDIAN).getInt(0));

        StringBuilder fileNameBuilder = new StringBuilder();
        int r = 0;
        while((r = rd.read()) > 0)
            fileNameBuilder.append((char)r);
        info.setFileName(fileNameBuilder.toString());

        return info;
    }
}
