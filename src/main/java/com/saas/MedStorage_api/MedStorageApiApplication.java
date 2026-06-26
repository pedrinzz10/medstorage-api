package com.saas.MedStorage_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedStorageApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedStorageApiApplication.class, args);
	}

}
