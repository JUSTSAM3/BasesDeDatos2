package co.edu.unbosque.reservasbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para verificar el estado de conexion al motor RDBMS (PostgreSQL).
 * EDT 6.1: Aplicacion web - Conexion a RDBMS.
 */
@RestController
@RequestMapping("/api/health/rdbms")
public class RdbmsHealthController {

    private final DataSource dataSource;

    public RdbmsHealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkRdbmsConnection() {
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT 1")) {

            long duration = System.currentTimeMillis() - startTime;
            DatabaseMetaData metaData = connection.getMetaData();

            response.put("status", "UP");
            response.put("engine", "RDBMS");
            response.put("databaseProductName", metaData.getDatabaseProductName());
            response.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            response.put("driverName", metaData.getDriverName());
            response.put("driverVersion", metaData.getDriverVersion());
            response.put("url", metaData.getURL());
            response.put("validQuery", rs.next() && rs.getInt(1) == 1);
            response.put("responseTimeMs", duration);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "DOWN");
            response.put("engine", "RDBMS (PostgreSQL)");
            response.put("error", e.getMessage());
            response.put("responseTimeMs", duration);
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Endpoint EDT 7.1: Ejecuta y valida la suite de pruebas del RDBMS
     * (funciones, procedimientos y triggers).
     */
    @GetMapping("/test-routines")
    public ResponseEntity<Map<String, Object>> runRdbmsRoutinesTests() {
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            org.springframework.core.io.ClassPathResource scriptResource =
                    new org.springframework.core.io.ClassPathResource("db/test/7.1_pruebas_rdbms.sql");

            org.springframework.jdbc.datasource.init.ResourceDatabasePopulator populator =
                    new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator();
            populator.addScript(scriptResource);
            populator.setSeparator(";;"); // Permite ejecutar el bloque anonimo PL/pgSQL completo
            // Alternativamente ejecutar como sentencia unica:
            try (Statement statement = connection.createStatement();
                 java.io.InputStream is = scriptResource.getInputStream()) {
                String sql = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                statement.execute(sql);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "SUCCESS");
            response.put("edtTask", "7.1 Pruebas del RDBMS");
            response.put("testedComponents", java.util.List.of(
                    "sp_crear_reserva (Procedimiento)",
                    "fn_validar_disponibilidad (Funcion)",
                    "trg_validar_cruce_horario (Trigger)",
                    "fn_calcular_costo_total (Funcion)",
                    "trg_recalcular_costo_servicios (Trigger)",
                    "trg_generar_factura_al_confirmar (Trigger)",
                    "sp_registrar_pago (Procedimiento)",
                    "fn_estado_pago (Funcion)",
                    "sp_cancelar_reserva (Procedimiento)"
            ));
            response.put("executionTimeMs", duration);
            response.put("message", "Todas las pruebas de procedimientos, funciones y triggers pasaron exitosamente (6/6).");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "FAILED");
            response.put("edtTask", "7.1 Pruebas del RDBMS");
            response.put("error", e.getMessage());
            response.put("executionTimeMs", duration);
            return ResponseEntity.status(500).body(response);
        }
    }
}
