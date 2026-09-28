package gt.com.ro.devumgapp.usuario.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

public class RolesRequest {

    @SerializedName("rolIds")
    public Set<Long> rolIds; // Para PUT /{id}/roles

    public RolesRequest(Set<Long> rolIds) {
        this.rolIds = rolIds;
    }
}
