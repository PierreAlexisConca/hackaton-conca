package hackaton.conca.repository;

import hackaton.conca.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByActivoTrue();
    List<Producto> findByActivoFalse();
    List<Producto> findAll();
    Optional<Producto> findByCodigo(String codigo);
}
