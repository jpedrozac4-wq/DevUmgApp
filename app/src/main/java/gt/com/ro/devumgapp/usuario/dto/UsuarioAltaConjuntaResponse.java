package gt.com.ro.devumgapp.usuario.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import gt.com.ro.devumgapp.rol.dto.RolResumenResponse;

public class UsuarioAltaConjuntaResponse {
    @SerializedName("usuarioId") public long usuarioId;
    @SerializedName("roles") public List<RolResumenResponse> roles;
    @SerializedName("docenteId") public Long docenteId;
    @SerializedName("estudianteId") public Long estudianteId;
}
