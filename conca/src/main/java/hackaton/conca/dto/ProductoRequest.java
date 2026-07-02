package hackaton.conca.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductoRequest {

    @NotBlank
    @Size(max = 50)
    private String codigo;

    @NotBlank
    @Size(max = 200)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal precio;

    @NotNull
    @Min(0)
    private Integer stockActual;

    @NotNull
    @Min(0)
    private Integer stockMinimo;

    @NotNull
    private Integer categoriaId;
}
