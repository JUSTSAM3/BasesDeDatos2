package co.edu.unbosque.reservasbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Configuracion del boilerplate de conexion para MongoDB.
 * Habilita repositorios especificos de Mongo y auditoria de documentos.
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "co.edu.unbosque.reservasbackend.repository.mongo")
public class MongoConfig {
}
