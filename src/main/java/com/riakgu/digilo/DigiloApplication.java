package com.riakgu.digilo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@ConfigurationPropertiesScan
public class DigiloApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigiloApplication.class, args);
	}

}
