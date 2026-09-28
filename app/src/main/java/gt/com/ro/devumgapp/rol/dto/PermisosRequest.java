package gt.com.ro.devumgapp.rol.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

public class PermisosRequest {

    @SerializedName("permisoIds")
    public Set<Long> permisoIds; // Para PUT /{id}/permisos

    public PermisosRequest() {
    }

    public PermisosRequest(Set<Long> permisoIds) {
        this.permisoIds = permisoIds;
    }
}
