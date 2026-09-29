package gt.com.ro.devumgapp.academico.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** DTOs generated from the academic schemas published by the backend OpenAPI document. */
public final class AcademicDtos {
    private AcademicDtos() { }

    public static class DocentePerfil {
        public long id;
        public String codigo;
        public String nombre;
        public String apellido;
        public String email;
        public String telefono;
        public String especialidad;
        public boolean activo;
    }

    public static class EstudiantePerfil {
        public long id;
        public String codigo;
        public String nombres;
        public String apellidos;
        public String correo;
        public String telefono;
        public String direccion;
        public boolean activo;
    }

    public static class Carrera {
        public long id;
        public String codigo;
        public String nombre;
        public String descripcion;
        public int duracionAnios;
        public boolean activo;
    }

    public static class Curso {
        public long id;
        public String codigo;
        public String nombre;
        public String descripcion;
        public int creditos;
        public int horasSemanales;
        public int cicloAnio;
        public boolean activo;
        public long carreraId;
        public String carreraCodigo;
        public String carreraNombre;
    }

    public static class CursoPlan {
        public long id;
        public String codigo;
        public String nombre;
        public String descripcion;
        public int creditos;
        public int horasSemanales;
        public int cicloAnio;
        public boolean inscrito;
        public Long docenteId;
        public String docenteNombre;
    }

    public static class PlanCarrera {
        public Carrera carrera;
        public List<CursoPlan> cursosDisponibles;
        public List<Curso> cursosInscritos;
        public int totalCreditosPlan;
        public int totalCreditosInscritos;
    }

    public static class DocenteCurso {
        public long id;
        public String codigo;
        public String nombre;
        public String apellido;
        public String email;
        public String especialidad;
        public long cursoId;
        public String cursoCodigo;
        public String cursoNombre;
        public int cicloAnio;
    }

    public static class Inscripcion {
        public long id;
        public long estudianteId;
        public String estudianteCodigo;
        public String estudianteNombre;
        public long cursoId;
        public String cursoCodigo;
        public String cursoNombre;
        public long carreraId;
        public String carreraCodigo;
        public String carreraNombre;
        public String grado;
        public String seccion;
        public int cicloAnio;
        public String fechaInscripcion;
        public String estado;
        public String observaciones;
        public boolean activo;
    }

    public static class Nota {
        public long id;
        public long estudianteId;
        public String estudianteCodigo;
        public String estudianteNombre;
        public long cursoId;
        public String cursoCodigo;
        public String cursoNombre;
        public int cicloAnio;
        public String tipoEvaluacion;
        public double calificacion;
        public String observaciones;
        public boolean activo;
    }

    public static class Promedio {
        public long estudianteId;
        @SerializedName("promedio") public double promedio;
        public int cantidadNotas;
    }
}
