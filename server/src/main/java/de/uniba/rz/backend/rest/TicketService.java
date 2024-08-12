package de.uniba.rz.backend.rest;

import de.uniba.rz.backend.ThreadSafeTicketStore;
import de.uniba.rz.entities.Status;
import de.uniba.rz.entities.Ticket;

import java.util.List;


public class TicketService {
    private static final ThreadSafeTicketStore ticketStore = new ThreadSafeTicketStore();


    public static Ticket addTicket(Ticket newTicket) {
        if (newTicket == null) {
            return null;
        }
        ticketStore.storeNewTicket(newTicket.getReporter(), newTicket.getTopic(), newTicket.getDescription(), newTicket.getType(), newTicket.getPriority());
        return newTicket;
    }
    public static Ticket getTicket(int id) {
       for(Ticket ticket : getAllTicket()){
           if(ticket.getId() == id){
               return ticket;
           }
       }
       return null;
    }
    public static Ticket updateTicket(int ticketId, Status newStatus) {
         ticketStore.updateTicketStatus(ticketId, newStatus);
         return getTicket(ticketId);
    }
    public static List<Ticket> getAllTicket() {
        return ticketStore.getAllTickets();
    }
}
