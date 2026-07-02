package hackaton.conca.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "modificado_en", nullable = false)
    private LocalDateTime modificadoEn = LocalDateTime.now();

    @Column(name = "creado_por", nullable = false, length = 100)
    private String creadoPor = "SYSTEM";

    @Column(name = "modificado_por", nullable = false, length = 100)
    private String modificadoPor = "SYSTEM";
}
