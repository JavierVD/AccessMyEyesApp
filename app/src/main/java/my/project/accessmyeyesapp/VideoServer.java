package my.project.accessmyeyesapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class VideoServer {

    private Socket socket;
    private SurfaceView surfaceView;
    private Handler handler;
    private MediaPlayer mediaPlayer; // Declare mediaPlayer outside threads

    public VideoServer(Socket socket, SurfaceView surfaceView) {
        this.socket = socket;
        this.surfaceView = surfaceView;
        this.handler = new Handler(surfaceView.getContext().getMainLooper());
        this.mediaPlayer = new MediaPlayer(); // Initialize mediaPlayer here
    }

    public void iniciarRecepcion() {
        new Thread(() -> {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                while (true) {
                    int bytesLeidos = dis.readInt();
                    byte[] bytesImagen = new byte[bytesLeidos];
                    dis.readFully(bytesImagen);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytesImagen, 0, bytesLeidos);
                    handler.post(() -> {
                        SurfaceHolder surfaceHolder = surfaceView.getHolder();
                        if (surfaceHolder.getSurface().isValid()) {
                            Canvas canvas = null;
                            try {
                                canvas = surfaceHolder.lockCanvas();
                                if (canvas != null) {
                                    if (bitmap != null) {
                                        Log.d("BITMAP: ", String.valueOf(bitmap));
                                        canvas.drawBitmap(bitmap, 0, 0,new Paint(3));
                                    } else {
                                        Log.e("BITMAP ERROR", "EL BITMAP ES NULL");
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (canvas != null) {
                                    surfaceHolder.unlockCanvasAndPost(canvas);
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                // Create a buffer to hold received audio data
                byte[] buffer = new byte[1024]; // Adjust buffer size as needed
                int bytesRead;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                // Read audio data in chunks and store in a ByteArrayOutputStream
                while ((bytesRead = dis.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                // Get all received data as a byte array
                byte[] audioData = byteArrayOutputStream.toByteArray();

                // Create a temporary file to store the audio data
                File tempAudioFile = File.createTempFile("audio", ".mp3"); // Adjust extension based on format
                FileOutputStream fileOutputStream = new FileOutputStream(tempAudioFile);
                fileOutputStream.write(audioData);
                fileOutputStream.close();

                // Get a FileDescriptor for the temporary file
                FileDescriptor fileDescriptor = new FileInputStream(tempAudioFile).getFD();

                if (mediaPlayer != null) { // Check if mediaPlayer is initialized
                    mediaPlayer.setDataSource(fileDescriptor);
                    mediaPlayer.prepareAsync(); // Prepare asynchronously to avoid blocking the thread

                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.start(); // Start playback after preparation
                        }
                    });
                } else {
                    // Handle the case where mediaPlayer is not initialized (error or not created)
                    Log.e("VideoServer", "mediaPlayer is not initialized for audio playback");
                }

                // Delete the temporary file after playback is complete (optional)
                tempAudioFile.delete();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void detenerRecepcion() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        // Interrupt the video receiving thread (if applicable)
        // You might need a flag or mechanism to signal the thread to stop
    }

}
