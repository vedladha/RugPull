package edu.wisc.t32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The launcher for the $RPC crypto currency.
 *
 * <p>The main class in this application is not required to be running all the time instead this app
 * acts as a deployment for various crypto-currency needs on a single time run basis.
 */
public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  /**
   * Launches the $RPC deployment application.
   *
   * @param args the command line arguments given to the $RPC deployment
   */
  public static void main(String[] args) {
    LOGGER.info("Hello, World!");
    LOGGER.error("Error, World!");
  }

}
