package my.project.accessmyeyesapp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class Receiver extends Thread {
    private static final String TAG = "AudioReceiver";
    private Socket _socket;
    private DataInputStream inputStream;
    private AudioTrack audioTrack;
    private boolean isRunning;

    public Receiver(Socket socket) {
        this._socket = socket;
        isRunning = true;
    }

    @Override
    public void run() {
        try {
            inputStream = new DataInputStream(new BufferedInputStream(_socket.getInputStream()));

            int bufferSize = AudioTrack.getMinBufferSize(
                    44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC, 44100,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize, AudioTrack.MODE_STREAM);

            audioTrack.play();

            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            while (isRunning && (bytesRead = inputStream.read(buffer)) != -1) {
                audioTrack.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stopAudio();
        }
    }

    public void stopAudio() {
        isRunning = false;
        try {
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
            if (inputStream != null) inputStream.close();
            if (_socket != null) _socket.close();
            Log.d(TAG, "Audio playback stopped");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
