package co.edu.unbosque.reservasbackend.config;

import co.edu.unbosque.reservasbackend.nosql.service.EventoNoSqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Inicializador automatico para MongoDB al arrancar Spring Boot.
 * Si la VM de MongoDB esta encendida y accesible, asegura los indices
 * e inserta los documentos semilla en detalles_evento y resenas cuando estan vacias.
 * Si la VM aun no esta encendida, registra un aviso sin detener el backend.
 */
@Component
public class MongoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(MongoDataInitializer.class);

    private final EventoNoSqlService eventoNoSqlService;

    @Value("${app.mongo.auto-init:true}")
    private boolean autoInit;

    public MongoDataInitializer(EventoNoSqlService eventoNoSqlService) {
        this.eventoNoSqlService = eventoNoSqlService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!autoInit) {
            return;
        }
        try {
            Map<String, Object> resultado = eventoNoSqlService.inicializarEsquemaYDatosSemilla();
            log.info("[MongoDB Init] Colecciones e indices verificados correctamente: {}", resultado);
        } catch (Exception ex) {
            log.warn("[MongoDB Init] No se pudo inicializar MongoDB al arranque (verifique que el contenedor/VM este activo): {}",
                    ex.getMessage());
        }
    }
}

