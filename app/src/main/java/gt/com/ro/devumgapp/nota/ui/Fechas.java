package gt.com.ro.devumgapp.nota.ui;

/**
 * Formats backend ISO date strings for display in the grades module.
 */
final class Fechas {

    private Fechas() {
    }

    static String formatearFecha(String iso) {
        String fecha = iso == null ? "" : iso.trim();
        if (fecha.length() >= 10 && fecha.charAt(4) == '-' && fecha.charAt(7) == '-') {
            return fecha.substring(8, 10) + "/" + fecha.substring(5, 7) + "/" + fecha.substring(0, 4);
        }
        return fecha;
    }

    static String formatearFechaHora(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return "";
        }
        String fecha = formatearFecha(iso);
        if (iso.length() >= 16 && iso.charAt(10) == 'T') {
            return fecha + " " + iso.substring(11, 16);
        }
        return fecha;
    }
}
