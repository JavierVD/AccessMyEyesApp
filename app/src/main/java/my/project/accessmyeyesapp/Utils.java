package my.project.accessmyeyesapp;

import java.util.List;
import java.util.UUID;

import my.project.accessmyeyesapp.ui.server_fragment_list.AlertaCard;

public class Utils {
    public static String generarIDUnico() {
        return UUID.randomUUID().toString();
    }

    public static boolean existeID(String id, List<AlertaCard> listaAlertas) {
        for (AlertaCard alerta : listaAlertas) {
            if (alerta.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
