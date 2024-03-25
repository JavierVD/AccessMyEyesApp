package my.project.accessmyeyesapp;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.Socket;

import android.Manifest;
import android.widget.Toast;

public class VideoClient {
    private Socket socket;
    private DatagramSocket datagramSocket;
    private boolean transmitiendo;
    private Thread hiloVideo;
    private Thread hiloAudio;
    private Context context;
    private static final int MY_CAMERA_PERMISSION_CODE = 1001;
    private ClientActivity mActivityInstance;

    public VideoClient(Socket socket, Context context) {
        this.socket = socket;
        this.context = context;
        this.transmitiendo = false;
        mActivityInstance = (ClientActivity) context;
    }

    public void ServerSocketThread(){

            new Thread(() -> {
                Socket s = socket;
                OutputStream os = null;

                if(s !=null){
                    try {
                        s.setKeepAlive(true);
                        os = s.getOutputStream();
                        while(true){
                            DataOutputStream dos = new DataOutputStream(os);
                            dos.writeInt(4);
                            dos.writeUTF("#@@#");
                            dos.writeInt(mActivityInstance.mPreview.mFrameBuffer.size());
                            dos.writeUTF("-@@-");
                            dos.flush();
                            System.out.println(mActivityInstance.mPreview.mFrameBuffer.size());
                            dos.write(mActivityInstance.mPreview.mFrameBuffer.toByteArray());
                            dos.flush();
                            Thread.sleep(1000/15);

                            Log.d("Flushed", "yes");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            if (os!= null)
                                os.close();

                        } catch (Exception e2) {
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    System.out.println("socket is null");

                }
            });


    }

    public void iniciarTransmision() {
        if (!transmitiendo) {
            transmitiendo = true;

            hiloVideo = new Thread(() -> {
                try {
                    ActivityCompat.requestPermissions((Activity) context,
                            new String[]{Manifest.permission.CAMERA},
                            MY_CAMERA_PERMISSION_CODE);
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                    }
                    Camera camera = Camera.open();
                    Camera.Parameters parameters = camera.getParameters();
                    parameters.setPreviewSize(320,240);


                    while (transmitiendo) {
                        camera.setPreviewCallback((data, camera1) -> {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            String imagenBase64 = bitmapToBase64(bitmap);

                            Log.e("HOLAAAAAAAAAAA", imagenBase64);

                            try {
                                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                                dos.writeUTF(imagenBase64);
                                dos.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }

                    camera.stopPreview();
                    camera.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            hiloVideo.start();

            hiloAudio = new Thread(() -> {
                try {
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 2048);
                    audioRecord.startRecording();
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    byte[] bufferAudio = new byte[2048];

                    while (transmitiendo) {
                        int bytesLeidos = audioRecord.read(bufferAudio, 0, bufferAudio.length);
                        dos.write(bufferAudio, 0, bytesLeidos);
                    }
                    audioRecord.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            hiloAudio.start();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public void detenerTransmision() {
        if (transmitiendo) {
            transmitiendo = false;
            try {
                hiloVideo.interrupt();
                hiloAudio.interrupt();
                hiloVideo.join();
                hiloAudio.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
