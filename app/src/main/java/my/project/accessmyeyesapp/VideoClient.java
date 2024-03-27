package my.project.accessmyeyesapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.TextureView;

import androidx.core.app.ActivityCompat;

import com.xlythe.view.camera.CameraView;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;


public class VideoClient {
    private static final String TAG = "VideoClient";
    private static final int MY_CAMERA_PERMISSION_CODE = 1001;
    private static final int AUDIO_SAMPLE_RATE = 11025;
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_BUFFER_SIZE = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT);
    private Socket socket;
    private ClientActivity clientActivity;
    private boolean isStreaming = false;
    private CameraView camera;
    private OutputStream outputStream;
    private DataOutputStream dos;
    private AudioRecord audioRecord;


    public VideoClient(Socket socket, CameraView camera, ClientActivity clientActivity) {
        this.socket = socket;
        this.camera = camera;
        this.clientActivity = clientActivity;
    }

    private byte[] captureTextureViewImage(TextureView textureView) {
        Bitmap bitmap = textureView.getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] byteArr = byteArrayOutputStream.toByteArray();
        return byteArr;
    }

    public void startStream() {
        final long intervalMillis = 1000 / 30;

        final Handler handler = new Handler();
        isStreaming = true;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (ActivityCompat.checkSelfPermission(clientActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(clientActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (isStreaming) {
                    byte[] imageData = captureTextureViewImage(camera.asTextureView());
                    new SendDataTask().execute(imageData);
                }

                handler.postDelayed(this, intervalMillis);
            }
        }, intervalMillis);

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (ActivityCompat.checkSelfPermission(clientActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
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
            audioRecord.stop();
            audioRecord = null;
            outputStream.close();
            dos.close();
            isStreaming = false;
            camera.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private class SendDataTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... params) {
            byte[] imageData = params[0];
            sendImageThroughSocket(socket, imageData);
            return null;
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


    private void sendImageThroughSocket(Socket socket, byte[] imageData) {
        try {
            outputStream = socket.getOutputStream();
            dos = new DataOutputStream(outputStream);

            dos.writeInt(4);
            dos.writeUTF("#@@#");
            dos.writeInt(imageData.length);
            dos.writeUTF("-@@-");
            dos.flush();
            dos.write(imageData);
            dos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendAudioThrougSocket(Socket socket, byte[] audio) {
        try {
            if(socket != null && socket.isConnected()){
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
