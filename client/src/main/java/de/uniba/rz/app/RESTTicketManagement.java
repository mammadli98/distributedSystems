package de.uniba.rz.app;

import de.uniba.rz.entities.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RESTTicketManagement implements TicketManagementBackend, TicketSearchBackend{

    private AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Method to create a new Ticket based on the provided information
     *
     * @param reporter    the name of the reporter
     * @param topic       the topic of the ticket
     * @param description a textual description of the problem
     * @param type        the {@link Type} of the ticket to be created
     * @param priority    the {@link Priority} of the problem
     * @return a {@link Ticket} representation of the newly created ticket
     * @throws TicketException if the creation failed
     */
    @Override
    public Ticket createNewTicket(String reporter, String topic, String description, Type type, Priority priority) throws TicketException {
        Client client = ClientBuilder.newClient();
        TicketREST ticket = new TicketREST(nextId.getAndIncrement(), reporter, topic, description, type, priority); // create object
        Entity<TicketREST> entity = Entity.xml(ticket); // marshal to json or xml
        Response response = client.target("http://localhost:9999")
                .path("/tickets")
                .request(MediaType.APPLICATION_XML) // Alternative: MediaType.APPLICATION_XML
                .header("Content-Type", "application/json") // Further headers possible
                .post(entity); // Alternatives: put()
        System.out.println("Status: " + response.getStatus() +
                "\nLocation: " + response.getLocation());
        client.close();
        return ticket.restToTicket();
    }

    /**
     * Returns a list of {@link Ticket}s currently available in the system.
     *
     * @return the list of {@link Ticket}s
     * @throws TicketException if technical problems occur
     */
    @Override
    public List<Ticket> getAllTickets() throws TicketException {
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:9999")
                .path("/tickets")
                .request(MediaType.APPLICATION_XML) // Alternative: MediaType.APPLICATION_XML
                .get(); // Alternatives: post(), put(), delete()
        client.close();
        if (response.getStatus() == 200) {
            List<TicketREST> list = response.readEntity(new GenericType<List<TicketREST>>() {});
            List<Ticket> tickets = new ArrayList<>();
            for (TicketREST ticket : list) {
                tickets.add(ticket.restToTicket());
            }
            return tickets;
        }
        return List.of();
    }

    /**
     * Returns a single {@link Ticket} with the given {@code id}
     *
     * @param id the Id of the ticket to be accepted
     * @return a {@link Ticket} representation of the ticket
     * @throws TicketException thrown if the ticket with the {@code id} is unknown
     */
    @Override
    public Ticket getTicketById(int id) throws TicketException {
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:9999")
                .path("/tickets").path(Integer.toString(id)) // http://localhost:9999/cats/5
                .request(MediaType.APPLICATION_XML) // Alternative: MediaType.APPLICATION_XML
                .get(); // Alternatives: post(), put(), delete()
        client.close();
        if (response.getStatus() == 200) {
            return response.readEntity(TicketREST.class).restToTicket();
        } else {
            System.err.println("A problem occured while retrieving ticket with id " + id);
        }
        return null;
    }

    /**
     * Method to accept a Ticket, i.e., changing the {@link Status} to
     * {@code Status.ACCEPTED}
     * <p>
     * Throws an exception if this status change is not possible (i.e., the
     * current status is not {@code Status.NEW}) or if the {@code id} refers to
     * a {@link Ticket} that does not exist.
     *
     * @param id the Id of the ticket to be accepted
     * @return a {@link Ticket} representation of the modified ticket
     * @throws TicketException thrown if the status change is not allowed or the ticket with
     *                         the {@code id} is unknown
     */
    @Override
    public Ticket acceptTicket(int id) throws TicketException {
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:9999")
                .path("/tickets").path(Integer.toString(id)) // http://localhost:9999/cats/5
                .queryParam(String.valueOf(Status.ACCEPTED))
                .request(MediaType.APPLICATION_XML) // Alternative: MediaType.APPLICATION_XML
                .get(); // Alternatives: post(), put(), delete()
        client.close();
        if (response.getStatus() == 200) {
            return response.readEntity(TicketREST.class).restToTicket();
        } else {
            System.err.println("A problem occured while updating ticket with id " + id);
        }
        return null;
    }

    /**
     * Method to reject a Ticket, i.e., changing the {@link Status} to
     * {@code Status.REJECTED}
     * <p>
     * Throws an exception if this status change is not possible (i.e., the
     * current status is not {@code Status.NEW}) or if the {@code id} refers to
     * a {@link Ticket} that does not exist.
     *
     * @param id the Id of the ticket to be rejected
     * @return a {@link Ticket} representation of the modified ticket
     * @throws TicketException thrown if the status change is not allowed or the ticket with
     *                         the {@code id} is unknown
     */
    @Override
    public Ticket rejectTicket(int id) throws TicketException {
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:9999")
                .path("/tickets").path(Integer.toString(id)) // http://localhost:9999/cats/5
                .queryParam(String.valueOf(Status.REJECTED))
                .request(MediaType.APPLICATION_XML) // Alternative: MediaType.APPLICATION_XML
                .get(); // Alternatives: post(), put(), delete()
        client.close();
        if (response.getStatus() == 200) {
            return response.readEntity(TicketREST.class).restToTicket();
        } else {
            System.err.println("A problem occured while updating ticket with id " + id);
        }
        return null;
    }

    /**
     * Method to close a Ticket, i.e., changing the {@link Status} to
     * {@code Status.CLOSED}
     * <p>
     * Throws an exception if this status change is not possible (i.e., the
     * current status is not {@code Status.ACCEPTED}) or if the {@code id}
     * refers to a {@link Ticket} that does not exist.
     *
     * @param id the Id of the ticket to be accepted
     * @return a {@link Ticket} representation of the modified ticket
     * @throws TicketException thrown if the status change is not allowed or the ticket with
     *                         the {@code id} is unknown
     */
    @Override
    public Ticket closeTicket(int id) throws TicketException {
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:9999")
                .path("/tickets").path(Integer.toString(id)) // http://localhost:9999/cats/5
                .queryParam(String.valueOf(Status.CLOSED))
                .request(MediaType.APPLICATION_XML) // Alternative: MediaType.APPLICATION_XML
                .get(); // Alternatives: post(), put(), delete()
        client.close();
        if (response.getStatus() == 200) {
            return response.readEntity(TicketREST.class).restToTicket();
        } else {
            System.err.println("A problem occured while updating ticket with id " + id);
        }
        return null;
    }

    public List<Ticket> getTicketsByNameAndType(String name, Type type) throws TicketException {
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:9999")
                .path("/tickets")
                .queryParam(name)
                .queryParam(String.valueOf(type))
                .request(MediaType.APPLICATION_XML) // Alternative: MediaType.APPLICATION_XML
                .get(); // Alternatives: post(), put(), delete()
        client.close();
        if (response.getStatus() == 200) {
            List<TicketREST> list = response.readEntity(new GenericType<List<TicketREST>>() {});
            List<Ticket> tickets = new ArrayList<>();
            for (TicketREST ticket : list) {
                tickets.add(ticket.restToTicket());
            }        } else {
// TODO: handle other (possible) status codes as well
        }
        System.err.println("A problem occured while searching");
        return null;
    }
    public List<Ticket> getTicketsByName(String name) throws TicketException {
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:9999")
                .path("/tickets")
                .queryParam(name)
                .request(MediaType.APPLICATION_XML) // Alternative: MediaType.APPLICATION_XML
                .get(); // Alternatives: post(), put(), delete()
        client.close();
        if (response.getStatus() == 200) {
            return response.readEntity(new GenericType<List<Ticket>>() {});
        } else {
            List<TicketREST> list = response.readEntity(new GenericType<List<TicketREST>>() {});
            List<Ticket> tickets = new ArrayList<>();
            for (TicketREST ticket : list) {
                tickets.add(ticket.restToTicket());
            }        }
        System.err.println("A problem occured while searching");
        return null;
    }

    /**
     * Method to be called to trigger graceful shutdown of a system
     */
    @Override
    public void triggerShutdown() {

    }
}
