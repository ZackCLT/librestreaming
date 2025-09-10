package me.lake.librestreaming.sample.music;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by ycchang on 2017/7/19.
 */

public class AudioFileDecoder extends AudioDecoder{

    private static final String TAG = "AudioFileDecoder";
    ArrayList<byte[]> dataList = new ArrayList<>();
    boolean isDecodeFinish = false;
    boolean isFirstDataReady = true;
    int totalRawSize = 0;
    int totalGetSize = 0;
    boolean isThreadStart = true;
    Thread decodeThread;
    MediaExtractor extractor = new MediaExtractor();
    private Object syncData = new Object();

    AudioFileDecoder(String encodeFile, String outFile, int outChannel) {
        super(encodeFile, outFile, outChannel);
        try {
            extractor.setDataSource(encodeFile);
        } catch (IOException e) {
            extractor = null;
        }
    }

    AudioFileDecoder(String encodeFile, int outChannel) {
        super(encodeFile, outChannel);
        try {
            extractor.setDataSource(encodeFile);
        } catch (IOException e) {
            extractor = null;
        }
    }

    AudioFileDecoder(Context ctx, String assetFileName, int outChannel) {
        super(ctx, assetFileName, outChannel);
        if (ctx != null && assetFileName != null) {
            try {
                //AssetFileDescriptor afd = ctx.getResources().openRawResourceFd(resId);
                AssetFileDescriptor afd = ctx.getAssets().openFd(assetFileName);

                if (afd == null) return;
                extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            } catch (IOException e) {
                extractor = null;
            }
        }
    }

    AudioFileDecoder(AssetFileDescriptor afd, int outChannel) {
        super(afd, outChannel);
        if (afd == null) return;
        try {
            extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            extractor = null;
        }
    }

    AudioFileDecoder(FileDescriptor fd, int outChannel) {
        super(fd, outChannel);
        if (fd == null) return;
        try {
            extractor.setDataSource(fd);
        } catch (IOException e) {
            extractor = null;
        }
    }

    @Override
    public void startDecode() {
        if (extractor == null) {
            return;
        }
        decodeThread = new Thread() {

            @Override
            public void run() {
                isDecodeFinish = false;
                long beginTime = System.currentTimeMillis();

                MediaFormat mediaFormat = null;
                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("audio/")) {
                        extractor.selectTrack(i);
                        mediaFormat = format;
                        break;
                    }
                }

                if (mediaFormat == null) {
                    extractor.release();
                    return;
                }
                RawAudioInfo rawAudioInfo = new RawAudioInfo();
                rawAudioInfo.channel = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                rawAudioInfo.sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                FileOutputStream fosDecoder = null;
                if (mOutFile != null) {
                    rawAudioInfo.tempRawFile = mOutFile;
                    try {
                        fosDecoder = new FileOutputStream(rawAudioInfo.tempRawFile);
                    } catch (IOException e) {
                        Log.d(TAG, "e:"+e);
                        return;
                    }
                }

                String mediaMime = mediaFormat.getString(MediaFormat.KEY_MIME);
                MediaCodec codec;
                try {
                    codec = MediaCodec.createDecoderByType(mediaMime);
                } catch (IOException e) {
                    Log.d(TAG, "e:"+e);
                    return;
                }
                codec.configure(mediaFormat, null, null, 0);
                codec.start();

                ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
                ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

                final double audioDurationUs = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                final long kTimeOutUs = 5000;
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                boolean sawInputEOS = false;
                boolean sawOutputEOS = false;
                isThreadStart = true;

                try{
                    while (!sawOutputEOS && isThreadStart) {
                        if (!sawInputEOS) {
                            int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                            if (inputBufIndex >= 0) {
                                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                                int sampleSize = extractor.readSampleData(dstBuf, 0);
                                if (sampleSize < 0) {
                                    Log.i(TAG, "saw input EOS.");
                                    sawInputEOS = true;
                                    codec.queueInputBuffer(inputBufIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                                } else {
                                    long presentationTimeUs = extractor.getSampleTime();
                                    codec.queueInputBuffer(inputBufIndex,0,sampleSize,presentationTimeUs,0);
                                    extractor.advance();
                                }
                            }
                        }
                        int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
                        if (res >= 0) {
                            int outputBufIndex = res;
                            // Simply ignore codec config buffers.
                            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!= 0) {
                                Log.i(TAG, "audio encoder: codec config buffer");
                                codec.releaseOutputBuffer(outputBufIndex, false);
                                continue;
                            }

                            if (info.size != 0) {

                                ByteBuffer outBuf = codecOutputBuffers[outputBufIndex];

                                outBuf.position(info.offset);
                                outBuf.limit(info.offset + info.size);
                                byte[] data = new byte[info.size];
                                outBuf.get(data);


                                if (rawAudioInfo.channel == 2 && mOutChannel == AudioFormat.CHANNEL_IN_MONO) {
                                    byte[] monoData = getStereoToMonoPcm(data);
                                    synchronized (syncData) {
                                        dataList.add(monoData);
                                    }
                                    totalRawSize += monoData.length;
                                } else {
                                    synchronized (syncData) {
                                        dataList.add(data);
                                    }
                                    totalRawSize += data.length;
                                }
                                if (fosDecoder != null) {
                                    try {
                                        fosDecoder.write(data);
                                    } catch (IOException e) {
                                        Log.d(TAG, "e:"+e);
                                        return;
                                    }
                                }
                                if(mOnAudioDecoderListener != null) {
                                    if (isFirstDataReady) {
                                        isFirstDataReady = false;
                                        mOnAudioDecoderListener.onDataReady();
                                    }
                                    mOnAudioDecoderListener.onDecode(data, info.presentationTimeUs / audioDurationUs);
                                }
                            }

                            codec.releaseOutputBuffer(outputBufIndex, false);

                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                sawOutputEOS = true;
                            }

                        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            codecOutputBuffers = codec.getOutputBuffers();
                            Log.i(TAG, "output buffers have changed.");
                        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            MediaFormat oformat = codec.getOutputFormat();
                            Log.i(TAG, "output format has changed to " + oformat);
                        }
                    }
                    rawAudioInfo.size = totalRawSize;

                    if(mOnAudioDecoderListener != null)
                        mOnAudioDecoderListener.onDecode(null, 1);

                    isDecodeFinish = true;
                } finally {
                    try {
                        isThreadStart = false;
                        if (fosDecoder != null) {
                            fosDecoder.close();
                        }
                        if (codec != null) {
                            codec.stop();
                            codec.release();
                        }
                        if (extractor != null) {
                            extractor.release();
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "e:"+e);
                    }
                }
            }
        };
        decodeThread.start();
    }

    @Override
    public void reset(boolean isRestart) {
        isThreadStart = false;
        try {
            decodeThread.join();
        } catch (InterruptedException ignored) {
        }
        synchronized (syncData) {
            dataList.clear();
            totalRawSize = 0;
            totalGetSize = 0;
        }
        isDecodeFinish = false;
        isFirstDataReady = true;
        extractor = null;
        if (isRestart) {
            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(mAssetFileDescriptor.getFileDescriptor(), mAssetFileDescriptor.getStartOffset(), mAssetFileDescriptor.getLength());
            } catch (IOException e) {
                extractor = null;
            }
        }
    }

    @Override
    public byte[] getAudioBuffer(final int size) {
        try {
            if (totalRawSize - totalGetSize < size) {
                if (isDecodeFinish) { // the last data
                    if (mOnAudioDecoderListener != null) {
                        mOnAudioDecoderListener.onDataFinish();
                    }
                } else {
                    return null;
                }
            } else if (totalGetSize >= totalRawSize) {
                if (mOnAudioDecoderListener != null) {
                    mOnAudioDecoderListener.onDataFinish();
                }
                return null;
            }
            byte[] result = null;
            synchronized (syncData) {
                result = AudioMixHelper.getExternalAudioDataByte(dataList, size);
                if (result != null) totalGetSize += result.length;
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] getStereoToMonoPcm(byte[] data) {
        if (data == null)
            return null;
        byte[] process = new byte[data.length / 2];
        for(int i = 0, j = 0; i<data.length / 2; i += 2, j += 4) {
            process[i] = data[j];
            process[i + 1] = data[j + 1];
        }
        return process;
    }
}
