package de.uniba.rz.backend.rest;

import de.uniba.rz.entities.Ticket;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
@Path("tickets")
public class Create {

    @POST
    public Response create(Ticket newTicket, @Context UriInfo uriInfo) {
        Ticket ticket = TicketService.addTicket(newTicket);

        if (ticket == null) {
            throw new WebApplicationException("Invalid request body" , 400);
        }

        UriBuilder path = uriInfo.getAbsolutePathBuilder();
        path.path(Integer.toString(ticket.getId()));
        return Response.created(path.build()).build();
    }
}
