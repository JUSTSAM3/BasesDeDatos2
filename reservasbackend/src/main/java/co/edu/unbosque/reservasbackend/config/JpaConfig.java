package co.edu.unbosque.reservasbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuracion de repositorios JPA (PostgreSQL).
 * Separa los repositorios relacionales de los repositorios NoSQL (MongoDB).
 */
@Configuration
@EnableJpaRepositories(basePackages = "co.edu.unbosque.reservasbackend.sql.repository")
public class JpaConfig {
}
