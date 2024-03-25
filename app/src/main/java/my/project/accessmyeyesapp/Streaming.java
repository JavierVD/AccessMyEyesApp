package my.project.accessmyeyesapp;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import android.Manifest;
import android.widget.Toast;

public class Streaming {

    private Socket _socket;
    private DataOutputStream _outputStream;
    private Activity _activity;
    private Context _context;
    private static final int PERMISSION_REQUEST_CODE = 101;
    private AudioRecord audioRecord;
    private boolean isRecording = false;

    public Streaming(Socket socket, Activity activity, Context context) {
        this._socket = socket;
        this._activity = activity;
        this._context = context;
    }

    public void startStreaming() {
        new MediaStreamTask().execute();
    }

    public void stopStreaming() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (this._socket != null && !this._socket.isClosed()) {
            try {
                this._socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class MediaStreamTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                _outputStream = new DataOutputStream(new BufferedOutputStream(_socket.getOutputStream()));
                Log.d("STREAMING", "Audio streaming started");
                startAudioStreaming();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                stopStreaming();
            }
            return null;
        }

        private boolean checkPermissions() {
            if (ContextCompat.checkSelfPermission(_activity, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(_activity,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
            return true;
        }

        private void startAudioStreaming() {
            int bufferSize = AudioRecord.getMinBufferSize(
                    44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

            int permissionCheck = ContextCompat.checkSelfPermission(_context, Manifest.permission.RECORD_AUDIO);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(_context, "Permiso para grabar audio no concedido", Toast.LENGTH_SHORT).show();
                return;
            }

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, 44100,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            byte[] buffer = new byte[bufferSize];
            audioRecord.startRecording();
            isRecording = true;

            while (isRecording) {
                int bytesRead = audioRecord.read(buffer, 0, bufferSize);
                if (bytesRead > 0) {
                    try {
                        _outputStream.write(buffer, 0, bytesRead);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
    }
}
