package util;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;

public class WeightLoader {

    public static float[] loadWeights(String path){

        File file = new File(path);
        try(FileInputStream fis = new FileInputStream(file)) {
            FileChannel fc = fis.getChannel();
            long size = fc.size();

            if ((size & 3) != 0)
                throw new IllegalStateException("Invalid weight file size: " + size);

            ByteBuffer bb = ByteBuffer.allocateDirect((int) size);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            while (bb.hasRemaining())
                fc.read(bb);

            bb.flip();

            FloatBuffer fb = bb.asFloatBuffer();
            float[] out = new float[fb.remaining()];
            fb.get(out);

            return out;
        }catch (IOException e){
            throw new RuntimeException("Failed to load NN weights", e);
        }

    }
}

