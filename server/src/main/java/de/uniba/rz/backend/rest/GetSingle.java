package de.uniba.rz.backend.rest;

import de.uniba.rz.entities.Ticket;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;

@Path("tickets")
public class GetSingle {
    @GET
    @Path("{ticket-id}")
    public Ticket getTicket(@PathParam("ticket-id") int id) {
        Ticket ticket = TicketService.getTicket(id);

        if (ticket == null) {
            throw new WebApplicationException("No ticket found with id: " + id, 404);
        }
        return ticket;
    }
}
