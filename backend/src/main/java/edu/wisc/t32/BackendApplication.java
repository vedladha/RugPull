package edu.wisc.t32;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Backend Application.
 */
@SpringBootApplication
public class BackendApplication {

  /**
   * Starts the Spring Boot application.
   * 
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }

}