package me.lake.librestreaming.sample.music;

/**
 * Created by ycchang on 2017/8/28.
 */

public class AudioMixData {
    private byte[] audioData;
    private float weight;
    public AudioMixData(byte[] audioData, float weight) {
        this.audioData = audioData;
        this.weight = weight;
    }
    public byte[] getAudioData() {
        return this.audioData;
    }
    public float getWeight() {
        return this.weight;
    }
}
