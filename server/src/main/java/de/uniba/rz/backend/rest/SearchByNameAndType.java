package de.uniba.rz.backend.rest;

import de.uniba.rz.entities.Ticket;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.ArrayList;
import java.util.List;

@Path("search")
public class SearchByNameAndType {
    @GET
    public List<Ticket> getTickets(@QueryParam("text") String text, @QueryParam("type") String type) {
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
        if(type != null && !type.isEmpty()) {
            for(Ticket ticket : tickets) {
                if(ticket.getType().toString().equals(type)) {
                    results.add(ticket);
                }
            }
        }
        return results;
    }
}
