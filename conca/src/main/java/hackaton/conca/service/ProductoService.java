package hackaton.conca.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVReader;
import hackaton.conca.dto.ProductoRequest;
import hackaton.conca.dto.ProductoResponse;
import hackaton.conca.entity.Producto;
import hackaton.conca.repository.CategoriaRepository;
import hackaton.conca.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    // ─── Mapear entidad → DTO ───
    private ProductoResponse toResponse(Producto p) {
        ProductoResponse r = new ProductoResponse();
        r.setId(p.getId());
        r.setCodigo(p.getCodigo());
        r.setNombre(p.getNombre());
        r.setDescripcion(p.getDescripcion());
        r.setPrecio(p.getPrecio());
        r.setStockActual(p.getStockActual());
        r.setStockMinimo(p.getStockMinimo());
        r.setActivo(p.getActivo());
        r.setCreadoEn(p.getCreadoEn());
        r.setModificadoEn(p.getModificadoEn());
        r.setCreadoPor(p.getCreadoPor());
        r.setModificadoPor(p.getModificadoPor());
        if (p.getCategoria() != null) {
            r.setCategoriaNombre(p.getCategoria().getNombre());
            r.setCategoriaId(p.getCategoria().getId());
        }
        return r;
    }

    // ─── Solo activos (para la tabla principal) ───
    public List<ProductoResponse> listarActivos() {
        return productoRepository.findByActivoTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Todos: activos + inactivos ───
    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ProductoResponse obtenerPorId(Integer id) {
        return toResponse(productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id)));
    }

    public ProductoResponse crear(ProductoRequest request, String usuario) {
        Producto p = new Producto();
        p.setCodigo(request.getCodigo());
        p.setNombre(request.getNombre());
        p.setDescripcion(request.getDescripcion());
        p.setPrecio(request.getPrecio());
        p.setStockActual(request.getStockActual());
        p.setStockMinimo(request.getStockMinimo());
        p.setCategoria(categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada")));
        p.setActivo(true);
        p.setCreadoEn(LocalDateTime.now());
        p.setModificadoEn(LocalDateTime.now());
        p.setCreadoPor(usuario);
        p.setModificadoPor(usuario);
        return toResponse(productoRepository.save(p));
    }

    public ProductoResponse actualizar(Integer id, ProductoRequest request, String usuario) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        p.setCodigo(request.getCodigo());
        p.setNombre(request.getNombre());
        p.setDescripcion(request.getDescripcion());
        p.setPrecio(request.getPrecio());
        p.setStockActual(request.getStockActual());
        p.setStockMinimo(request.getStockMinimo());
        p.setCategoria(categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada")));
        p.setModificadoEn(LocalDateTime.now());
        p.setModificadoPor(usuario);
        return toResponse(productoRepository.save(p));
    }

    // ─── Eliminado lógico (activo = false) ───
    public void eliminar(Integer id) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        p.setActivo(false);
        p.setModificadoEn(LocalDateTime.now());
        p.setModificadoPor("sistema");
        productoRepository.save(p);
    }

    // ─── Restaurar (activo = true) ───
    public ProductoResponse restaurar(Integer id) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        p.setActivo(true);
        p.setModificadoEn(LocalDateTime.now());
        p.setModificadoPor("sistema");
        return toResponse(productoRepository.save(p));
    }

    // ─── Importar CSV o Excel (UPSERT por código) ───
    public int importarDesdeArchivo(MultipartFile file) {
        String filename = file.getOriginalFilename();
        int count = 0;
        try {
            if (filename != null && filename.endsWith(".csv")) {
                Reader reader = new InputStreamReader(file.getInputStream());
                try (CSVReader csvReader = new CSVReader(reader)) {
                    java.util.List<String[]> rows = csvReader.readAll();
                    for (int i = 1; i < rows.size(); i++) {
                        String[] row = rows.get(i);
                        if (row.length < 6) continue;
                        upsertProducto(
                            row[0].trim(),
                            row[1].trim(),
                            new BigDecimal(row[2].trim()),
                            Integer.parseInt(row[3].trim()),
                            Integer.parseInt(row[4].trim()),
                            Integer.parseInt(row[5].trim())
                        );
                        count++;
                    }
                }
            } else {
                Workbook wb = WorkbookFactory.create(file.getInputStream());
                Sheet sheet = wb.getSheetAt(0);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    upsertProducto(
                        row.getCell(0).getStringCellValue(),
                        row.getCell(1).getStringCellValue(),
                        BigDecimal.valueOf(row.getCell(2).getNumericCellValue()),
                        (int) row.getCell(3).getNumericCellValue(),
                        (int) row.getCell(4).getNumericCellValue(),
                        (int) row.getCell(5).getNumericCellValue()
                    );
                    count++;
                }
                wb.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al importar archivo: " + e.getMessage());
        }
        return count;
    }

    /**
     * Inserta si el código no existe, actualiza si ya existe.
     */
    private void upsertProducto(String codigo, String nombre, BigDecimal precio,
                                 int stockActual, int stockMinimo, int categoriaId) {
        Optional<Producto> existente = productoRepository.findByCodigo(codigo);
        Producto p = existente.orElse(new Producto());

        boolean esNuevo = p.getId() == null;

        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStockActual(stockActual);
        p.setStockMinimo(stockMinimo);
        p.setCategoria(categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RuntimeException("Categoría " + categoriaId + " no encontrada")));
        p.setActivo(true);
        p.setModificadoEn(LocalDateTime.now());
        p.setModificadoPor("import");

        if (esNuevo) {
            p.setCreadoEn(LocalDateTime.now());
            p.setCreadoPor("import");
        }

        productoRepository.save(p);
    }

    // ─── Exportar PDF ───
    public byte[] exportarPdf() throws Exception {
        List<Producto> lista = productoRepository.findByActivoTrue();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        doc.add(new Paragraph("Reporte de Productos — GestiStock", titleFont));
        doc.add(new Paragraph("Generado: " + LocalDateTime.now().toString().substring(0, 16)));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{10f, 25f, 20f, 10f, 10f, 15f});

        for (String header : new String[]{"Código", "Nombre", "Descripción", "Precio", "Stock", "Categoría"}) {
            com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new Phrase(header));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (Producto p : lista) {
            table.addCell(p.getCodigo());
            table.addCell(p.getNombre());
            table.addCell(p.getDescripcion() != null ? p.getDescripcion() : "");
            table.addCell("S/ " + p.getPrecio().toString());
            table.addCell(p.getStockActual().toString());
            table.addCell(p.getCategoria() != null ? p.getCategoria().getNombre() : "");
        }

        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    // ─── Exportar Excel ───
    public byte[] exportarExcel() throws Exception {
        List<Producto> lista = productoRepository.findByActivoTrue();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Productos");

        // Estilo encabezado
        org.apache.poi.ss.usermodel.CellStyle headerStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header = sheet.createRow(0);
        String[] cols = {"ID", "Código", "Nombre", "Descripción", "Precio", "Stock Actual", "Stock Mínimo", "Categoría", "Creado Por", "Modificado Por"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        int rowIdx = 1;
        for (Producto p : lista) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(p.getId());
            row.createCell(1).setCellValue(p.getCodigo());
            row.createCell(2).setCellValue(p.getNombre());
            row.createCell(3).setCellValue(p.getDescripcion() != null ? p.getDescripcion() : "");
            row.createCell(4).setCellValue(p.getPrecio().doubleValue());
            row.createCell(5).setCellValue(p.getStockActual());
            row.createCell(6).setCellValue(p.getStockMinimo());
            row.createCell(7).setCellValue(p.getCategoria() != null ? p.getCategoria().getNombre() : "");
            row.createCell(8).setCellValue(p.getCreadoPor());
            row.createCell(9).setCellValue(p.getModificadoPor());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }
}
