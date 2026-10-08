package co.edu.unbosque.reservasbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@SpringBootTest
class ReservasbackendApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

}
