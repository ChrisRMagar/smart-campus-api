package com.smartcampus.application;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application entry point.
 * Bootstraps an embedded Grizzly HTTP server, registers Jersey with Jackson
 * for JSON serialisation, and starts listening on port 8080.
 *
 * Run with:  java -jar target/smart-campus-api-1.0.0.jar
 * API base:  http://localhost:8080/api/v1
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static void main(String[] args) throws IOException {

        // Building Jersey ResourceConfig: scan the top-level package so that all resources, exception mappers and filters are discovered automatically.
         
        final ResourceConfig config = new ResourceConfig()
                .packages("com.smartcampus")        // auto-scan all sub-packages
                .register(JacksonFeature.class);    // enable Jackson JSON support

        // Creating and starting the Grizzly HTTP server
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI), config, false);

        server.start();

        LOGGER.info("=============================================================");
        LOGGER.info(" Smart Campus API started successfully!");
        LOGGER.info(" Base URI : http://localhost:8080/api/v1");
        LOGGER.info(" Discovery: GET http://localhost:8080/api/v1");
        LOGGER.info(" Rooms    : GET http://localhost:8080/api/v1/rooms");
        LOGGER.info(" Sensors  : GET http://localhost:8080/api/v1/sensors");
        LOGGER.info(" Press CTRL+C to stop the server.");
        LOGGER.info("=============================================================");

        // Keeping server alive until interrupted
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Smart Campus API...");
            server.shutdown();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
