package de.uniba.rz.backend.rest;

import com.sun.net.httpserver.HttpServer;
import de.uniba.rz.backend.RemoteAccess;
import de.uniba.rz.backend.TicketStore;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import java.net.URI;
import java.util.Properties;

public class RESTRemoteAccess implements RemoteAccess {

    private TicketStore ticketStore;
    HttpServer httpServer;
    private static Properties properties = Configuration.loadProperties();


    /**
     * Generic startup method which might be used to prepare the actual execution
     *
     * @param ticketStore reference to the {@link TicketStore} which is used by the application
     */
    @Override
    public void prepareStartup(TicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }


    /**
     * Triggers the graceful shutdown of the system.
     */
    @Override
    public void shutdown() {
        httpServer.stop(0);
    }

    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        String serverUri = properties.getProperty("serverUri");

        URI baseUri = UriBuilder.fromUri(serverUri).build();
        ResourceConfig config = ResourceConfig.forApplicationClass(ExamplesApi.class);
        httpServer = JdkHttpServerFactory.createHttpServer(baseUri, config);

        System.out.println("Server ready to serve your JAX-RS requests...");
    }
}
