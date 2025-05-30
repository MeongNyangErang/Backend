package com.meongnyangerang.meongnyangerang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class MeongnyangerangApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeongnyangerangApplication.class, args);
	}

}
