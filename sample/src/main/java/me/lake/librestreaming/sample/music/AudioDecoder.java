package me.lake.librestreaming.sample.music;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Created by ycchang on 2017/7/19.
 */

public abstract class AudioDecoder {
    Context mContext;
    String mEncodeFile;
    String mOutFile;
    int mOutChannel;
    String mAssetFileName;
    AssetFileDescriptor mAssetFileDescriptor;

    boolean isExternalFile;
    FileDescriptor mFileDescriptor;

    OnAudioDecoderListener mOnAudioDecoderListener;

    AudioDecoder(String encodeFile, String outFile, int outChannel) {
        this.mEncodeFile = encodeFile;
        this.mOutFile = outFile;
        this.mOutChannel = outChannel;
    }

    AudioDecoder(String encodeFile, int outChannel) {
        this.mEncodeFile = encodeFile;
        this.mOutFile = null;
        this.mOutChannel = outChannel;
    }

    AudioDecoder(Context ctx, String assetFileName, int outChannel) {
        this.mContext = ctx;
        this.mAssetFileName = assetFileName;
        this.mOutChannel = outChannel;
    }

    AudioDecoder(AssetFileDescriptor afd, int outChannel) {
        this.mAssetFileDescriptor = afd;
        this.mOutChannel = outChannel;
    }

    AudioDecoder(FileDescriptor fd, int outChannel) {
        this.mFileDescriptor = fd;
        this.mOutChannel = outChannel;
    }

    public static AudioDecoder createDefaultDecoder(String encodeFile, int outChannel) {
        return new AudioFileDecoder(encodeFile, outChannel);
    }

    public static AudioDecoder createDefaultDecoder(String encodeFile, String outFile, int outChannel) {
        return new AudioFileDecoder(encodeFile, outFile, outChannel);
    }

    public static AudioDecoder createDefaultDecoder(Context ctx, String assetFileName, int outChannel) {
        return new AudioFileDecoder(ctx, assetFileName, outChannel);
    }

    public static AudioDecoder createDefaultDecoder(AssetFileDescriptor afd, int outChannel) {
        return new AudioFileDecoder(afd, outChannel);
    }

    public static AudioDecoder createDefaultDecoder(FileDescriptor fd, int outChannel) {
        return new AudioFileDecoder(fd, outChannel);
    }

    public void setOnAudioDecoderListener(OnAudioDecoderListener l) {
        this.mOnAudioDecoderListener = l;
    }

    /**
     * 解码
     *
     * @return
     * @throws IOException
     */
    public abstract void startDecode();

    public abstract void reset(boolean isRestart);

    public abstract byte[] getAudioBuffer(int size);

    public static class RawAudioInfo {
        public String tempRawFile;
        public int size;
        public long sampleRate;
        public int channel;
    }

    public interface OnAudioDecoderListener {
        /**
         * monitor when processing decode
         *
         * @param decodedBytes
         * @param progress     range 0~1
         * @throws IOException
         */
        void onDecode(byte[] decodedBytes, double progress);

        void onDataReady();

        void onDataFinish();

    }
}
