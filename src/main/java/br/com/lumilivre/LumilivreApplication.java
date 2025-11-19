package br.com.lumilivre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LumilivreApplication {

	public static void main(String[] args) {
		SpringApplication.run(LumilivreApplication.class, args);
	}

}