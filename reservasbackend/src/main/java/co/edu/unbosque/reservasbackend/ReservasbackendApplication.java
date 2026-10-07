package co.edu.unbosque.reservasbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "co.edu.unbosque.reservasbackend.sql.repository")
public class ReservasbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservasbackendApplication.class, args);
	}

}
