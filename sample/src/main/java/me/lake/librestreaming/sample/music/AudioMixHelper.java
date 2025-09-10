package me.lake.librestreaming.sample.music;

import java.util.ArrayList;

/**
 * Created by ycchang on 2017/8/28.
 */

public class AudioMixHelper {

    /**
     * mix all audio pcm data in audioListData
     * pcm data source need input 16-bits, mono, 44100.
     *
     * @param audioMixDataList store all mix data
     * @return mix pcm data with input weight
     */

    public static byte[] getMixData(ArrayList<AudioMixData> audioMixDataList) {
        if (audioMixDataList != null && audioMixDataList.size() > 0) {
            int defaultLength;
            if (audioMixDataList.get(0).getAudioData() != null) {
                defaultLength = audioMixDataList.get(0).getAudioData().length; // use first data length as default length, remove not equal length
            } else {
                return null;
            }
            for (int i = 0; i < audioMixDataList.size(); i++) {
                if (defaultLength != audioMixDataList.get(i).getAudioData().length) {
                    audioMixDataList.remove(i);
                }
            }

            byte[] result = new byte[defaultLength];
            for (int i = 0; i < defaultLength; i += 2) {
                int newVal = 0;
                for(int j = 0; j < audioMixDataList.size(); j++) {
                    byte[] audioDataByte = audioMixDataList.get(j).getAudioData();
                    short val = (short) (((audioDataByte[i + 1] & 0xFF) << 8) | (audioDataByte[i] & 0xFF));
                    newVal += (int)(val * audioMixDataList.get(j).getWeight());
                }
                if (newVal > Short.MAX_VALUE) newVal = Short.MAX_VALUE;
                else if (newVal < Short.MIN_VALUE) newVal = Short.MIN_VALUE;
                result[i] = (byte) (newVal & 0xFF);
                result[i + 1] = (byte) ((newVal >> 8) & 0xFF);
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * get byte[] with needed size from list
     *
     * @param audioByteList store all pcm data by sequence
     * @param size needed byte size
     * @return pcm byte array. And it will fill all 0 to the end if data source is insufficient
     */

    public static byte[] getExternalAudioDataByte(ArrayList<byte[]> audioByteList, int size) {
        try {
            byte[] processData = null;
            int processDataPos = 0;
            byte[] result = new byte[size];
            int resultPos = 0;
            while (resultPos < size) {
                if (audioByteList.size() == 0) break;
                else processData = audioByteList.remove(0);

                if (processData == null) continue;
                int remainLen = processData.length;
                if (size - resultPos >= remainLen) { // fill all this processData to result
                    processDataPos = processData.length;
                } else {
                    remainLen = size - resultPos;
                    processDataPos += remainLen;
                }
                System.arraycopy(processData, 0, result, resultPos, remainLen);
                resultPos += remainLen;
                if (processDataPos >= processData.length) {
                    processDataPos = 0;
                    processData = null;
                }
            }
            if (processData != null) {
                int remainLen = processData.length - processDataPos;
                byte[] remainingData = new byte[remainLen];
                System.arraycopy(processData, processDataPos, remainingData, 0, remainLen);
                audioByteList.add(0, remainingData);
            }
            if (resultPos < size) {
                for (int i = resultPos; i < size; i++) {
                    result[i] = 0;
                }
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
