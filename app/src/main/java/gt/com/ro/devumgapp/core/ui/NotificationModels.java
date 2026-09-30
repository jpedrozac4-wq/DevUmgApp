package gt.com.ro.devumgapp.notificacion;
public final class NotificationModels { private NotificationModels(){} public static class Item { public long id; public String tipo,titulo,mensaje,destinoTipo,fechaCreacion; public Long destinoId; public boolean leida; } public static class Count { public long noLeidas; } public static class Device { public String token; } }
