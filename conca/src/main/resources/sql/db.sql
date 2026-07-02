-- ============================================================
-- GestiStock — SQL Server 2022
-- Script idempotente: ejecutar cuantas veces sea necesario
-- ============================================================

-- PASO 0: Crear BD si no existe
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'GestiStockDB')
    CREATE DATABASE GestiStockDB;
GO
USE GestiStockDB;
GO

-- ============================================================
-- TABLAS
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Roles')
CREATE TABLE Roles (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    nombre         VARCHAR(50)  NOT NULL UNIQUE,
    descripcion    VARCHAR(200) NULL,
    activo         BIT          NOT NULL DEFAULT 1,
    creado_en      DATETIME2    NOT NULL DEFAULT GETDATE(),
    modificado_en  DATETIME2    NOT NULL DEFAULT GETDATE(),
    creado_por     VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    modificado_por VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Usuarios')
CREATE TABLE Usuarios (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    username       VARCHAR(100) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    nombre         VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    rol_id         INT          NOT NULL,
    activo         BIT          NOT NULL DEFAULT 1,
    creado_en      DATETIME2    NOT NULL DEFAULT GETDATE(),
    modificado_en  DATETIME2    NOT NULL DEFAULT GETDATE(),
    creado_por     VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    modificado_por VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT FK_Usuarios_Roles FOREIGN KEY (rol_id) REFERENCES Roles(id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Categorias')
CREATE TABLE Categorias (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL UNIQUE,
    descripcion    VARCHAR(200) NULL,
    activo         BIT          NOT NULL DEFAULT 1,
    creado_en      DATETIME2    NOT NULL DEFAULT GETDATE(),
    modificado_en  DATETIME2    NOT NULL DEFAULT GETDATE(),
    creado_por     VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    modificado_por VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Productos')
CREATE TABLE Productos (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    codigo         VARCHAR(50)   NOT NULL UNIQUE,
    nombre         VARCHAR(200)  NOT NULL,
    descripcion    VARCHAR(500)  NULL,
    precio         DECIMAL(10,2) NOT NULL
                   CONSTRAINT CHK_Precio_Positivo CHECK (precio >= 0),
    stock_actual   INT           NOT NULL DEFAULT 0
                   CONSTRAINT CHK_Stock_NoNegativo CHECK (stock_actual >= 0),
    stock_minimo   INT           NOT NULL DEFAULT 0,
    categoria_id   INT           NOT NULL,
    activo         BIT           NOT NULL DEFAULT 1,
    creado_en      DATETIME2     NOT NULL DEFAULT GETDATE(),
    modificado_en  DATETIME2     NOT NULL DEFAULT GETDATE(),
    creado_por     VARCHAR(100)  NOT NULL DEFAULT 'SYSTEM',
    modificado_por VARCHAR(100)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT FK_Productos_Categorias FOREIGN KEY (categoria_id) REFERENCES Categorias(id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'TiposMovimiento')
CREATE TABLE TiposMovimiento (
    id     INT IDENTITY(1,1) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'MovimientosInventario')
CREATE TABLE MovimientosInventario (
    id                 INT IDENTITY(1,1) PRIMARY KEY,
    producto_id        INT          NOT NULL,
    tipo_movimiento_id INT          NOT NULL,
    cantidad           INT          NOT NULL
                       CONSTRAINT CHK_Cantidad_Positiva CHECK (cantidad > 0),
    stock_antes        INT          NOT NULL,
    stock_despues      INT          NOT NULL,
    motivo             VARCHAR(300) NULL,
    usuario_id         INT          NOT NULL,
    creado_en          DATETIME2    NOT NULL DEFAULT GETDATE(),
    modificado_en      DATETIME2    NOT NULL DEFAULT GETDATE(),
    creado_por         VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    modificado_por     VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT FK_Mov_Producto FOREIGN KEY (producto_id)        REFERENCES Productos(id),
    CONSTRAINT FK_Mov_Tipo     FOREIGN KEY (tipo_movimiento_id) REFERENCES TiposMovimiento(id),
    CONSTRAINT FK_Mov_Usuario  FOREIGN KEY (usuario_id)         REFERENCES Usuarios(id)
);
GO

-- ============================================================
-- DATOS MAESTROS
-- ============================================================

-- Roles
IF NOT EXISTS (SELECT 1 FROM Roles WHERE nombre = 'ADMIN')
    INSERT INTO Roles (nombre, descripcion) VALUES ('ADMIN', 'Administrador del sistema');
IF NOT EXISTS (SELECT 1 FROM Roles WHERE nombre = 'EMPLEADO')
    INSERT INTO Roles (nombre, descripcion) VALUES ('EMPLEADO', 'Empleado de almacen');
GO

-- Categorias
IF NOT EXISTS (SELECT 1 FROM Categorias WHERE nombre = 'Herramientas Manuales')
    INSERT INTO Categorias (nombre, descripcion)
    VALUES ('Herramientas Manuales', 'Martillos, llaves, destornilladores');
IF NOT EXISTS (SELECT 1 FROM Categorias WHERE nombre = 'Materiales Electricos')
    INSERT INTO Categorias (nombre, descripcion)
    VALUES ('Materiales Electricos', 'Cables, interruptores, enchufes');
IF NOT EXISTS (SELECT 1 FROM Categorias WHERE nombre = 'Pinturas y Acabados')
    INSERT INTO Categorias (nombre, descripcion)
    VALUES ('Pinturas y Acabados', 'Pinturas, brochas, rodillos');
GO

-- Tipos de Movimiento
IF NOT EXISTS (SELECT 1 FROM TiposMovimiento WHERE nombre = 'ENTRADA')
    INSERT INTO TiposMovimiento (nombre) VALUES ('ENTRADA');
IF NOT EXISTS (SELECT 1 FROM TiposMovimiento WHERE nombre = 'SALIDA')
    INSERT INTO TiposMovimiento (nombre) VALUES ('SALIDA');
IF NOT EXISTS (SELECT 1 FROM TiposMovimiento WHERE nombre = 'AJUSTE')
    INSERT INTO TiposMovimiento (nombre) VALUES ('AJUSTE');
GO

-- Usuarios (password en texto plano — login es frontend-only)
IF NOT EXISTS (SELECT 1 FROM Usuarios WHERE email = 'admin@gestistock.com')
    INSERT INTO Usuarios (username, password, nombre, email, rol_id)
    VALUES ('admin', '12345', 'Administrador', 'admin@gestistock.com', 1);

IF NOT EXISTS (SELECT 1 FROM Usuarios WHERE email = 'juan@gestistock.com')
    INSERT INTO Usuarios (username, password, nombre, email, rol_id)
    VALUES ('empleado1', '67890', 'Juan Perez', 'juan@gestistock.com', 2);
GO

-- Productos iniciales
IF NOT EXISTS (SELECT 1 FROM Productos WHERE codigo = 'MART-001')
    INSERT INTO Productos (codigo, nombre, descripcion, precio, stock_actual, stock_minimo, categoria_id)
    VALUES ('MART-001', 'Martillo de Acero 16oz', 'Martillo de carpintero', 25.90, 50, 10, 1);

IF NOT EXISTS (SELECT 1 FROM Productos WHERE codigo = 'DEST-001')
    INSERT INTO Productos (codigo, nombre, descripcion, precio, stock_actual, stock_minimo, categoria_id)
    VALUES ('DEST-001', 'Destornillador estrella #2', 'Punta Phillips', 8.50, 100, 20, 1);

IF NOT EXISTS (SELECT 1 FROM Productos WHERE codigo = 'CABLE-001')
    INSERT INTO Productos (codigo, nombre, descripcion, precio, stock_actual, stock_minimo, categoria_id)
    VALUES ('CABLE-001', 'Cable NYM 2x2.5mm', 'Cable electrico x metro', 3.20, 200, 50, 2);

IF NOT EXISTS (SELECT 1 FROM Productos WHERE codigo = 'PINT-001')
    INSERT INTO Productos (codigo, nombre, descripcion, precio, stock_actual, stock_minimo, categoria_id)
    VALUES ('PINT-001', 'Pintura Latex Blanco 1Gal', 'Pintura interior', 45.00, 30, 5, 3);
GO

-- ============================================================
-- PROCEDIMIENTO ALMACENADO: Registrar movimiento de inventario
-- ============================================================
CREATE OR ALTER PROCEDURE sp_RegistrarMovimiento
    @producto_id        INT,
    @tipo_movimiento_id INT,
    @cantidad           INT,
    @motivo             VARCHAR(300),
    @usuario_id         INT,
    @resultado_msg      VARCHAR(200) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        DECLARE @stock_actual INT, @stock_nuevo INT, @tipo_nombre VARCHAR(50);

        SELECT @stock_actual = stock_actual
        FROM   Productos
        WHERE  id = @producto_id AND activo = 1;

        IF @stock_actual IS NULL
        BEGIN
            SET @resultado_msg = 'ERROR: Producto no encontrado o inactivo.';
            ROLLBACK; RETURN;
        END

        SELECT @tipo_nombre = nombre FROM TiposMovimiento WHERE id = @tipo_movimiento_id;

        IF @tipo_nombre IN ('ENTRADA', 'AJUSTE')
            SET @stock_nuevo = @stock_actual + @cantidad;
        ELSE IF @tipo_nombre = 'SALIDA'
        BEGIN
            IF @stock_actual < @cantidad
            BEGIN
                SET @resultado_msg = 'ERROR: Stock insuficiente. Actual: ' + CAST(@stock_actual AS VARCHAR);
                ROLLBACK; RETURN;
            END
            SET @stock_nuevo = @stock_actual - @cantidad;
        END

        UPDATE Productos
        SET    stock_actual   = @stock_nuevo,
               modificado_en  = GETDATE(),
               modificado_por = (SELECT username FROM Usuarios WHERE id = @usuario_id)
        WHERE  id = @producto_id;

        INSERT INTO MovimientosInventario
            (producto_id, tipo_movimiento_id, cantidad, stock_antes, stock_despues,
             motivo, usuario_id, creado_por, modificado_por)
        VALUES
            (@producto_id, @tipo_movimiento_id, @cantidad, @stock_actual, @stock_nuevo,
             @motivo, @usuario_id,
             (SELECT username FROM Usuarios WHERE id = @usuario_id),
             (SELECT username FROM Usuarios WHERE id = @usuario_id));

        SET @resultado_msg = 'OK: Movimiento registrado. Stock nuevo: ' + CAST(@stock_nuevo AS VARCHAR);
        COMMIT;
    END TRY
    BEGIN CATCH
        ROLLBACK;
        SET @resultado_msg = 'ERROR: ' + ERROR_MESSAGE();
    END CATCH
END;
GO

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT 'Roles'     AS tabla, COUNT(*) AS registros FROM Roles    UNION ALL
SELECT 'Usuarios',              COUNT(*) FROM Usuarios            UNION ALL
SELECT 'Categorias',            COUNT(*) FROM Categorias          UNION ALL
SELECT 'Productos activos',     COUNT(*) FROM Productos WHERE activo = 1 UNION ALL
SELECT 'Productos inactivos',   COUNT(*) FROM Productos WHERE activo = 0 UNION ALL
SELECT 'TiposMovimiento',       COUNT(*) FROM TiposMovimiento     UNION ALL
SELECT 'Movimientos',           COUNT(*) FROM MovimientosInventario;
GO
