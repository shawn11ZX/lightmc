package com.wooduan.lightmc.serializer.jobj;

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.APC;

public class JObjCompressor 
{
	private static final Logger logger = LoggerFactory.getLogger(JObjCompressor.class);
	private static int compressionThreshold = 1024 * 1;
    private static int zipCompressLevel = 5;
    
    public static APC internalDecode(ByteBuf buffer) throws ClassNotFoundException, IOException 
    {
    	int sysFlag = buffer.readInt();
    	byte[] body = new byte[buffer.readableBytes()];
    	buffer.readBytes(body);
    	// uncompress body
    	if ((sysFlag & MessageSysFlag.CompressedFlag) == MessageSysFlag.CompressedFlag) {
    		body = uncompress(body);
    	}
    	ObjectInputStream input = new HackedObjectInputStream(
    			new ByteArrayInputStream(body));
		APC rpc = (APC) input.readObject();
		return rpc;
    }
    
    
    public static boolean tryToCompressMessage(final ByteBuf buffer, final int index) 
    {
        byte[] body = new byte[buffer.readableBytes() - index];
        buffer.getBytes(index, body);
        if (body != null) {
            if (body.length >= compressionThreshold) {
                try {
                    byte[] data = compress(body, zipCompressLevel);
                    if (data != null) {
                    	buffer.setBytes(index, data);
                        return true;
                    }
                }
                catch (IOException e) {
                	logger.error("tryToCompressMessage exception", e);
                }
            }
        }

        return false;
    }
    
    public static byte[] uncompress(final byte[] src) throws IOException {
        byte[] result = src;
        byte[] uncompressData = new byte[src.length];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(src);
        InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(src.length);

        try {
            while (true) {
                int len = inflaterInputStream.read(uncompressData, 0, uncompressData.length);
                if (len <= 0) {
                    break;
                }
                byteArrayOutputStream.write(uncompressData, 0, len);
            }
            byteArrayOutputStream.flush();
            result = byteArrayOutputStream.toByteArray();
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            try {
                byteArrayInputStream.close();
            }
            catch (IOException e) {
            }
            try {
                inflaterInputStream.close();
            }
            catch (IOException e) {
            }
            try {
                byteArrayOutputStream.close();
            }
            catch (IOException e) {
            }
        }

        return result;
    }


    public static byte[] compress(final byte[] src, final int level) throws IOException {
        byte[] result = src;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(src.length);
        java.util.zip.Deflater deflater = new java.util.zip.Deflater(level);
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
        try {
            deflaterOutputStream.write(src);
            deflaterOutputStream.finish();
            deflaterOutputStream.close();
            result = byteArrayOutputStream.toByteArray();
        }
        catch (IOException e) {
            deflater.end();
            throw e;
        }
        finally {
            try {
                byteArrayOutputStream.close();
            }
            catch (IOException e) {
            }

            deflater.end();
        }

        return result;
    }
    
    public static void setCompressionThreshold(int compressionThreshold) {
		JObjCompressor.compressionThreshold = compressionThreshold;
	}
    
    public static void setZipCompressLevel(final int zipCompressLevel) 
    {
        JObjCompressor.zipCompressLevel = zipCompressLevel;
    }
}
