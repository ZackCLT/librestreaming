package me.lake.librestreaming.sample.audiofilter;

import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;

import me.lake.librestreaming.filter.softaudiofilter.BaseSoftAudioFilter;
import me.lake.librestreaming.sample.music.AudioDecoder;

public class BgmAudioFilter extends BaseSoftAudioFilter {

    private AssetFileDescriptor assetFileDescriptor;
    private AudioDecoder audioDecoder;
    private boolean loop = false;
    private boolean hasData = false;
    private float micGain = 0.0f;
    private float bgmGain = 0.3f;

    public BgmAudioFilter(AssetFileDescriptor assetFileDescriptor, boolean loop) {
        this.assetFileDescriptor = assetFileDescriptor;
        this.loop = loop;
        this.audioDecoder = AudioDecoder.createDefaultDecoder(assetFileDescriptor, AudioFormat.CHANNEL_IN_MONO);
    }

    @Override
    public void onInit(int size) {
        super.onInit(size);
        audioDecoder.setOnAudioDecoderListener(listener);
        audioDecoder.startDecode();
    }

    @Override
    public boolean onFrame(byte[] originBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        if (originBuff == null || targetBuff == null || hasData == false) return false;
        byte[] bgm = getAudioData(originBuff.length);
        for (int i = 0; i < originBuff.length; i += 2) {
            short origin = (short) (((originBuff[i + 1] << 8) | (originBuff[i] & 0xff)));
            short bgSample = (short) (((bgm[i + 1] << 8) | (bgm[i] & 0xff)));

            int mixed = (int) (origin * micGain + bgSample * bgmGain);
            if (mixed > Short.MAX_VALUE) mixed = Short.MAX_VALUE;
            if (mixed < Short.MIN_VALUE) mixed = Short.MIN_VALUE;

            targetBuff[i] = (byte) (mixed & 0xff);
            targetBuff[i + 1] = (byte) ((mixed >> 8) & 0xff);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (assetFileDescriptor != null) {
            try {
                assetFileDescriptor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            assetFileDescriptor = null;
        }

        if (audioDecoder != null) {
            audioDecoder.reset(false);
            audioDecoder = null;
        }
    }

    private byte[] getAudioData(int size) {
        return (audioDecoder == null) ? null : audioDecoder.getAudioBuffer(size);
    }

    private AudioDecoder.OnAudioDecoderListener listener = new AudioDecoder.OnAudioDecoderListener() {
        @Override
        public void onDecode(byte[] decodedBytes, double progress) {
        }

        @Override
        public void onDataReady() {
            hasData = true;
        }

        @Override
        public void onDataFinish() {
            if (loop) {
                audioDecoder.reset(true);
                audioDecoder.startDecode();
            } else {
                hasData = false;
            }
        }
    };

}
