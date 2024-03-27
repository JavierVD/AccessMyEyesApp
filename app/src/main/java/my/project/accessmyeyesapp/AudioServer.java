package my.project.accessmyeyesapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;

import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;


public class AudioServer {
    private static final String TAG = "VideoClient";
    private static final int AUDIO_SAMPLE_RATE = 11025;
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_BUFFER_SIZE = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT);
    private Socket socket;
    private ServerActivity serverActivity;
    private boolean isStreaming = false;
    private OutputStream outputStream;
    private DataOutputStream dos;
    private AudioRecord audioRecord;


    public AudioServer(Socket socket, ServerActivity serverActivity) {
        this.socket = socket;
        this.serverActivity = serverActivity;
    }

    public void startStream() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                isStreaming = true;

                if (ActivityCompat.checkSelfPermission(serverActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT, AUDIO_BUFFER_SIZE);
                byte[] buffer = new byte[AUDIO_BUFFER_SIZE];

                audioRecord.startRecording();
                while (isStreaming) {
                    int bytesRead = audioRecord.read(buffer, 0, AUDIO_BUFFER_SIZE);
                    if (bytesRead > 0) {
                        byte[] audio = Arrays.copyOf(buffer, bytesRead);
                        new SendDataAudioTask().execute(audio);
                    }
                }
            }
        }).start();
    }

    public void stopStream(){

        try {
            isStreaming = false;

            if(audioRecord != null)
                audioRecord.stop();
            audioRecord = null;
            if(outputStream != null)
                outputStream.close();
            if(dos != null)
                dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private class SendDataAudioTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... params) {
            byte[] audio = params[0];
            sendAudioThrougSocket(socket, audio);
            return null;
        }
    }

    private void sendAudioThrougSocket(Socket socket, byte[] audio) {
        try {
            if(socket.isConnected()){
                outputStream = socket.getOutputStream();
                dos = new DataOutputStream(outputStream);
                dos.writeInt(4);
                dos.writeUTF("*@@*");
                dos.writeInt(audio.length);
                dos.writeUTF("(@@)");
                dos.flush();
                dos.write(audio);
                dos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
