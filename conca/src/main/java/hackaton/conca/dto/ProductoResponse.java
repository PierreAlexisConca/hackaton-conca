package hackaton.conca.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductoResponse {
    private Integer id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stockActual;
    private Integer stockMinimo;
    private String categoriaNombre;
    private Integer categoriaId;
    private Boolean activo;
    private LocalDateTime creadoEn;
    private LocalDateTime modificadoEn;
    private String creadoPor;
    private String modificadoPor;
}
