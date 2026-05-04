package com.zwy.gk_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GkBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GkBackendApplication.class, args);
	}

}