package de.uniba.rz.backend;

import de.uniba.rz.entities.Priority;
import de.uniba.rz.entities.Status;
import de.uniba.rz.entities.Ticket;
import de.uniba.rz.entities.Type;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadSafeTicketStore implements TicketStore{

    private static AtomicInteger nextTicketId =new AtomicInteger(0);
    private ConcurrentHashMap<Integer, Ticket> ticketList = new ConcurrentHashMap<>();

        @Override
        public Ticket storeNewTicket(String reporter, String topic, String description, Type type, Priority priority) {
            System.out.println("Creating new Ticket from Reporter: " + reporter + " with the topic \"" + topic + "\"");
            Ticket newTicket = new Ticket(nextTicketId.getAndIncrement(), reporter, topic, description, type, priority);
            ticketList.put(newTicket.getId(), newTicket);
            return newTicket;
        }

        @Override
        public void updateTicketStatus(int ticketId, Status newStatus) {
            Ticket ticket = ticketList.get(ticketId);
            ticket.setStatus(newStatus);
        }

        @Override
        public List<Ticket> getAllTickets() {
            return ticketList.values().stream().toList();
        }
}
