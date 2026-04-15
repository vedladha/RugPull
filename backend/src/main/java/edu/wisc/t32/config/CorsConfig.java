package edu.wisc.t32.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS (Cross-Origin Resource Sharing) configuration.
 */
@Configuration
public class CorsConfig {

  /**
   * Configures global CORS rules.
   *
   * <p>Allows requests to all endpoints ("/**") from the local development server
   * (<a href="http://localhost:3000">http://localhost:3000</a>),
   * supporting standard HTTP methods and credentials.
   *
   * @return a {@link WebMvcConfigurer} containing the CORS mappings
   */
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
      }
    };
  }
}

