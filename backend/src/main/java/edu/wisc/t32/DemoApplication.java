package edu.wisc.t32;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo application entry point for springboot.
 */
@SpringBootApplication
@RestController
public class DemoApplication {
  /**
   * Main entry point for our application.
   *
   * @param args the cli args
   */
  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  /**
   * Hello demo endpoint.
   *
   * @param name the name endpoint
   * @return endpoint output
   */
  @GetMapping("/hello")
  public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
    return String.format("Hello %s!", name);
  }
}
