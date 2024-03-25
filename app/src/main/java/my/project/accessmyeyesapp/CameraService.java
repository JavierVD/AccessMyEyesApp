package my.project.accessmyeyesapp;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class CameraService {
    private int mServerPort;
    private String mServerIP;
    private Context mContext;
    private Handler mHandler;
    private VideoClient mActivityInstance;
    private Socket socket;

    public CameraService(Context context, Socket socket, Handler handler){
        super();
        mContext=context;
        mHandler = handler;
        this.socket = socket;
        mActivityInstance = (VideoClient)mContext;
    }
    public void run(){
        try {
            while (true){
                Socket s = socket;
                new Thread(new ServerSocketThread(s)).start();
            }
        }catch(Exception e){
            Log.d("ServerThread", "run: erro");
        }
    }

    public class ServerSocketThread implements Runnable{
        Socket s = null;
        OutputStream os = null;
        public ServerSocketThread(Socket s) throws IOException {
            this.s = s;
        }
        @Override
        public void run() {
            if(s !=null){
                String clientIp = s.getInetAddress().toString().replace("/", "");
                int clientPort = s.getPort();
                System.out.println("====client ip====="+clientIp);
                System.out.println("====client port====="+clientPort);
                try {

                    s.setKeepAlive(true);
                    os = s.getOutputStream();
                    while(true){
                        //服务器端向客户端发送数据
                        //dos.write(mPreview.mFrameBuffer.);
                        DataOutputStream dos = new DataOutputStream(os);
                        dos.writeInt(4);
                        dos.writeUTF("#@@#");
                        dos.writeInt(mActivityInstance.mPreview.mFrameBuffer.size());
                        dos.writeUTF("-@@-");
                        dos.flush();
                        System.out.println(mActivityInstance.mPreview.mFrameBuffer.size());
                        dos.write(mActivityInstance.mPreview.mFrameBuffer.toByteArray());
                        //System.out.println("outlength"+mPreview.mFrameBuffer.length);
                        dos.flush();
                        Thread.sleep(1000/15);
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
        }

}
