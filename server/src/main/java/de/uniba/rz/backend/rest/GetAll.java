package de.uniba.rz.backend.rest;

import de.uniba.rz.entities.Ticket;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("tickets")
public class GetAll {
    @GET
    public List<Ticket> getTickets() {
        return TicketService.getAllTicket();
    }
}
