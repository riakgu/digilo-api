package com.riakgu.digilo;

import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, TestMockConfig.class})
class DigiloApplicationTests {

	@Test
	void contextLoads() {
	}

}
