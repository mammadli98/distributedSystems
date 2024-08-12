package de.uniba.rz.backend.rest;

import de.uniba.rz.entities.Status;
import de.uniba.rz.entities.Ticket;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("tickets")
public class Update {

    @PUT
    @Path("{ticket-id}")
    public Response updateTicket(@PathParam("ticket-id") int id, @QueryParam("new-status") String newStatus) {
        Ticket ticket = TicketService.getTicket(id);

        if (ticket == null) {
            throw new WebApplicationException("No cat found with id: " + id, 404);
        }
        Status status = Status.valueOf(newStatus);
        Ticket resultTicket = TicketService.updateTicket(id, status);

        return Response.ok().entity(resultTicket).build();
    }
}

