// Copied and modified from the Porcupine project Android binding.
// https://github.com/Picovoice/Porcupine

package snowball.porcupine;

public class Porcupine {
    private final long object;

    static {
        System.loadLibrary("pv_porcupine");
    }

    public Porcupine(String modelFilePath, String keywordFilePath, float sens) throws Exception {
        try {
            object = init(modelFilePath, keywordFilePath, sens);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public boolean processFrame(short[] pcm) throws Exception {
        try {
            return process(object, pcm);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public void delete() {
        delete(object);
    }

    public native int getFrameLength();

    public native int getSampleRate();

    private native long init(String modelFilePath, String keywordFilePaths, float sensitivitie);

    private native boolean process(long object, short[] pcm);

    private native void delete(long object);
}
