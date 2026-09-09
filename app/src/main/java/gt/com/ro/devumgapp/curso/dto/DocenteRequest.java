package gt.com.ro.devumgapp.curso.dto;

import com.google.gson.annotations.SerializedName;

public class DocenteRequest {

    @SerializedName("docenteId")
    public Long docenteId;

    public DocenteRequest(Long docenteId) {
        this.docenteId = docenteId;
    }
}
