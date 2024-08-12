package de.uniba.rz.backend.rest;

import de.uniba.rz.entities.Ticket;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.ArrayList;
import java.util.List;

@Path("search")
public class SearchByName {
    @GET
    public List<Ticket> getTickets(@QueryParam("text") String text) {
        List<Ticket> tickets = new ArrayList<>();
        List<Ticket> results = new ArrayList<>();
        List<Ticket> ticketList = TicketService.getAllTicket();
        for(Ticket ticket : ticketList) {
            if(ticket.getDescription().contains(text)
                    || ticket.getStatus().toString().contains(text)
                    || ticket.getReporter().contains(text)){
                tickets.add(ticket);
            }
        }
        return results;
    }
}

