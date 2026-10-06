package co.edu.unbosque.reservasbackend.controller;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para verificar el estado de la conexion del boilerplate a MongoDB.
 */
@RestController
@RequestMapping("/api/health/mongo")
public class MongoHealthController {

    private final MongoTemplate mongoTemplate;

    public MongoHealthController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkMongoConnection() {
        Map<String, Object> response = new HashMap<>();
        try {
            Document pingCommand = new Document("ping", 1);
            Document pingResult = mongoTemplate.getDb().runCommand(pingCommand);

            response.put("status", "UP");
            response.put("database", mongoTemplate.getDb().getName());
            response.put("ping", pingResult.get("ok"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }
}
