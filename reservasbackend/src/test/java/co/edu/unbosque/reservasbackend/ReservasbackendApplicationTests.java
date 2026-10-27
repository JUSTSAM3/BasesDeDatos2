package co.edu.unbosque.reservasbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=none"
})
class ReservasbackendApplicationTests {

	@MockitoBean
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}
}
