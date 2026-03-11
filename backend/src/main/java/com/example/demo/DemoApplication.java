package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The main entry point for the Spring Boot application.
 *
 * <p>This class bootstraps the application and also serves as a basic REST controller
 * to provide a simple testing endpoint.
 */
@SpringBootApplication
@RestController
public class DemoApplication {

  /**
   * The main method that launches the Spring Boot application.
   *
   * @param args command-line arguments passed during application startup
   */
  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  /**
   * Handles HTTP GET requests to the "/hello" endpoint.
   *
   * <p>Returns a personalized greeting based on the provided name parameter, or a default
   * greeting if no name is provided.
   *
   * @param name the name to include in the greeting, defaults to "World"
   * @return a formatted greeting string
   */
  @GetMapping("/hello")
  public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
    return String.format("Hello %s!", name);
  }
}
