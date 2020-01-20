package com.bitcoin.indexer.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import io.reactivex.Single;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.basePackage("com.bitcoin.indexer.controllers"))
				.paths(PathSelectors.any())
				.build()
				.genericModelSubstitutes(ResponseEntity.class, Single.class)
				.apiInfo(getApiInfo());
	}

	private ApiInfo getApiInfo() {
		return new ApiInfo(
				"Bitcoin.com SLP-Indexer",
				"Query the Bitcoin.com SLP-Indexer MongoDb",
				"0.0.1",
				"",
				new Contact("", "", ""),
				"",
				"",
				Collections.emptyList()
		);
	}
}