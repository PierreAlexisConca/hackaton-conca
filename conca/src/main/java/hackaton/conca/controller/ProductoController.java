package hackaton.conca.controller;

import hackaton.conca.dto.ProductoRequest;
import hackaton.conca.dto.ProductoResponse;
import hackaton.conca.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    // ─── Listar solo activos ───
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(productoService.listarActivos());
    }

    // ─── Listar todos (activos + inactivos) ───
    @GetMapping("/todos")
    public ResponseEntity<List<ProductoResponse>> listarTodos() {
        return ResponseEntity.ok(productoService.listarTodos());
    }

    // ─── Obtener por ID ───
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    // ─── Crear ───
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.crear(request, "sistema"));
    }

    // ─── Actualizar ───
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request, "sistema"));
    }

    // ─── Eliminado lógico ───
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Restaurar producto inactivo ───
    @PatchMapping("/{id}/restaurar")
    public ResponseEntity<ProductoResponse> restaurar(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.restaurar(id));
    }

    // ─── Importar CSV / Excel (upsert por código) ───
    @PostMapping("/importar")
    public ResponseEntity<String> importar(@RequestParam("file") MultipartFile file) {
        int count = productoService.importarDesdeArchivo(file);
        return ResponseEntity.ok("Se procesaron " + count + " productos correctamente.");
    }

    // ─── Exportar PDF ───
    @GetMapping("/exportar/pdf")
    public ResponseEntity<byte[]> exportarPdf() throws Exception {
        byte[] pdf = productoService.exportarPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=productos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ─── Exportar Excel ───
    @GetMapping("/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel() throws Exception {
        byte[] excel = productoService.exportarExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=productos.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
