package my.project.accessmyeyesapp;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;

public class VideoServer {
    private static final int SERVER_PORT = 8080;
    private Socket clientSocket;
    private InputStream inputStream;
    private Handler handler;
    private ImageView imageView;
    private ServerActivity serverActivity;

    public VideoServer(Socket socket, ImageView imageView) {
        this.clientSocket = socket;
        this.imageView = imageView;
        this.handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                byte[] data = (byte[]) msg.obj;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                imageView.setImageBitmap(bitmap);
                Log.d("SETTED", "IMAGE SETTED");
                imageView.setImageResource(R.drawable.enviar_datos);
                return true;
            }
        });

    }

    public void startServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    inputStream = clientSocket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byte[] imageData = Arrays.copyOf(buffer, bytesRead);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                        serverActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void stopServer() {
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}