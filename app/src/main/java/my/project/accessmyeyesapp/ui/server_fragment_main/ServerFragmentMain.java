package my.project.accessmyeyesapp.ui.server_fragment_main;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import my.project.accessmyeyesapp.R;
import my.project.accessmyeyesapp.databinding.ServerFragmentMainBinding;

public class ServerFragmentMain extends Fragment {

    private ServerFragmentMainBinding binding;
    private static ConstraintLayout layoutFree;
    private static ConstraintLayout layoutBusy;
    private static TextView lblUsuarioActual;
    private static TextView lblTiempo;
    private static int segundos = 0;
    private static int minutos = 0;
    private static boolean isRunning = false;
    private static Handler handler;
    private static TextView lblIP;
    private SurfaceView videoView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = ServerFragmentMainBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Bundle args = getArguments();
        layoutBusy = root.findViewById(R.id.layoutBusy);
        layoutFree = root.findViewById(R.id.layoutFree);
        lblUsuarioActual = root.findViewById(R.id.lblUsuarioActual);
        lblIP = root.findViewById(R.id.lblIP);
        lblTiempo = root.findViewById(R.id.lblTiempo);

        videoView = root.findViewById(R.id.videoViewer);
        Log.e("SETTED AS", String.valueOf(videoView));
        if (args != null) {
            segundos = 0;
            minutos = 0;
            String usuario = args.getString("usuario");
            String ip = args.getString("IP");
            Log.d("USUARIO INYECTADO: ",usuario);
            layoutBusy.setVisibility(View.VISIBLE);
            layoutFree.setVisibility(View.INVISIBLE);
            lblUsuarioActual.setText(usuario);
            lblIP.setText(ip);
            lblTiempo.setText("00:00");
            startTimer();
        }


        handler = new Handler();
        return root;
    }

    public static void clear(){
        layoutFree.setVisibility(View.VISIBLE);
        layoutBusy.setVisibility(View.INVISIBLE);
        isRunning = false;
    }
    private static void startTimer() {
        isRunning = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    segundos++;
                    if (segundos == 60) {
                        segundos = 0;
                        minutos++;
                    }
                    actualizarTiempo();
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private static void actualizarTiempo() {
        String tiempo = String.format("%02d:%02d", minutos, segundos);
        lblTiempo.setText(tiempo);
    }
    public SurfaceView getVideoView() {
        Log.d("ID", String.valueOf(videoView));
        return videoView;
    }
    public static void hang(){

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isRunning = false;
        binding = null;
    }

}