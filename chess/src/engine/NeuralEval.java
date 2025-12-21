package engine;
public class NeuralEval {

    private final float[] W1, B1;
    private final float[] W2, B2;
    private final float[] W3, B3;
    private final float[] W4, B4;

    private int offset;

    public NeuralEval(float[] weights) {

        int in = 19 * 8 * 8;
        int h1 = 512;
        int h2 = 256;
        int h3 = 64;
        int out = 1;

        offset = 0;

        W1 = next(weights, in * h1);
        B1 = next(weights, h1);

        W2 = next(weights, h1 * h2);
        B2 = next(weights, h2);

        W3 = next(weights, h2 * h3);
        B3 = next(weights, h3);

        W4 = next(weights, h3 * out);
        B4 = next(weights, out);
    }

    private float[] next(float[] weights, int size) {
        float[] arr = new float[size];
        System.arraycopy(weights, offset, arr, 0, size);
        offset += size;
        return arr;
    }

    private float relu(float x) { return x > 0 ? x : 0; }

    public float evaluate(float[] input) {

        float[] h1 = new float[B1.length];
        for (int i = 0; i < h1.length; i++) {
            float sum = B1[i];
            int wOff = i * input.length;
            for (int j = 0; j < input.length; j++)
                sum += W1[wOff + j] * input[j];
            h1[i] = relu(sum);
        }

        float[] h2 = new float[B2.length];
        for (int i = 0; i < h2.length; i++) {
            float sum = B2[i];
            int wOff = i * h1.length;
            for (int j = 0; j < h1.length; j++)
                sum += W2[wOff + j] * h1[j];
            h2[i] = relu(sum);
        }

        float[] h3 = new float[B3.length];
        for (int i = 0; i < h3.length; i++) {
            float sum = B3[i];
            int wOff = i * h2.length;
            for (int j = 0; j < h2.length; j++)
                sum += W3[wOff + j] * h2[j];
            h3[i] = relu(sum);
        }

        float out = B4[0];
        for (int j = 0; j < h3.length; j++)
            out += W4[j] * h3[j];

        return out * 1000f;
    }
}
