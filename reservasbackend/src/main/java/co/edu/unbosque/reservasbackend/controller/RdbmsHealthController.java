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
}
