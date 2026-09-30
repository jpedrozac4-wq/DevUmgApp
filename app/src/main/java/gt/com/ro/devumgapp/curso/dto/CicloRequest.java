package gt.com.ro.devumgapp.curso.dto;

import com.google.gson.annotations.SerializedName;

public class CicloRequest {
    @SerializedName("cicloId") public long cicloId;
    public CicloRequest(long cicloId){this.cicloId=cicloId;}
}
