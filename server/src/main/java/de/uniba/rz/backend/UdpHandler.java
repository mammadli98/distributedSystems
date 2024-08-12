package de.uniba.rz.backend;

import de.uniba.rz.entities.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class UdpHandler extends Thread {

    DatagramPacket packet;
    TicketStore ticketStore;
    Object payload;
    DatagramSocket serverSocket;
    String requestType;
    Semaphore semaphore;

    public UdpHandler(DatagramPacket packet, DatagramSocket serverSocket, TicketStore ticketStore, String requestType, Object object, Semaphore semaphore) {
        this.packet = packet;
        this.ticketStore = ticketStore;
        this.payload = object;
        this.serverSocket = serverSocket;
        this.requestType = requestType;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();
            try {
                process();
            } finally {
                semaphore.release();
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!(e instanceof SocketException)) {
                System.err.println(e.getMessage());
            }
        } catch (TicketException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void process() throws IOException, ClassNotFoundException, TicketException {
        switch (requestType) {
            case "CREATE":
                Ticket newTicket = (Ticket) payload;
                ticketStore.storeNewTicket(newTicket.getReporter(), newTicket.getTopic(), newTicket.getDescription(), newTicket.getType(), newTicket.getPriority());
                reply(packet.getSocketAddress(), newTicket);
                break;
            case "GET_ALL":
                List<Ticket> allTickets = ticketStore.getAllTickets();
                reply(packet.getSocketAddress(), allTickets);
                break;
            case "GET_BY_ID":
                int ticketId = (Integer) payload;
                Ticket ticket = getTicketById(ticketId);
                reply(packet.getSocketAddress(), ticket);
                break;
            case "ACCEPT":
                int acceptTicketId = (Integer) payload;
                Ticket acceptedTicket = acceptTicket(acceptTicketId);
                reply(packet.getSocketAddress(), acceptedTicket);
                break;
            case "REJECT":
                int rejectedTicketId = (Integer) payload;
                Ticket rejectedTicket = rejectTicket(rejectedTicketId);
                reply(packet.getSocketAddress(), rejectedTicket);
                break;
            case "CLOSE":
                int closedTicketId = (Integer) payload;
                Ticket closedTicket = closeTicket(closedTicketId);
                reply(packet.getSocketAddress(), closedTicket);
                break;
            case "GET_BY_NAME":
                String name = (String) payload;
                List<Ticket> ticketsByName = getTicketsByName(name);
                reply(packet.getSocketAddress(), ticketsByName);
                break;
            case "GET_BY_NAME_AND_TYPE":
                NameAndTypePayload nameAndTypePayload = (NameAndTypePayload) payload;
                List<Ticket> ticketsByNameAndType = getTicketsByNameAndType(nameAndTypePayload.getName(), nameAndTypePayload.getType());
                reply(packet.getSocketAddress(), ticketsByNameAndType);
                break;
            default:
                System.err.println("Unknown request type: " + requestType);
        }
    }

    private void reply(SocketAddress clientAddress, Object response) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(response);
        os.flush();
        byte[] messageData = out.toByteArray();
        DatagramPacket responsePacket = new DatagramPacket(messageData, messageData.length, clientAddress);
        serverSocket.send(responsePacket);
        os.close();
        out.close();
    }

    private Ticket getTicketById(int id) throws TicketException {
        return ticketStore.getAllTickets().stream()
                .filter(ticket -> ticket.getId() == id)
                .findFirst()
                .orElseThrow(() -> new TicketException("Ticket not found with ID: " + id));
    }

    private Ticket acceptTicket(int id) throws TicketException {
        Ticket ticket = getTicketById(id);

        if (ticket == null) {
            throw new TicketException("Ticket not found with ID: " + id);
        }

        if (ticket.getStatus() != Status.NEW) {
            throw new TicketException("Ticket status must be NEW to accept. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(Status.ACCEPTED);
        return ticket;
    }

    private Ticket rejectTicket(int id) throws TicketException {
        Ticket ticket = getTicketById(id);

        if (ticket == null) {
            throw new TicketException("Ticket not found with ID: " + id);
        }

        if (ticket.getStatus() != Status.NEW) {
            throw new TicketException("Ticket status must be NEW to reject. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(Status.REJECTED);
        return ticket;
    }

    private Ticket closeTicket(int id) throws TicketException {
        Ticket ticket = getTicketById(id);

        if (ticket == null) {
            throw new TicketException("Ticket not found with ID: " + id);
        }

        if (ticket.getStatus() != Status.ACCEPTED) {
            throw new TicketException("Ticket status must be ACCEPTED to close. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(Status.CLOSED);
        return ticket;
    }

    private List<Ticket> getTicketsByName(String name) {
        return ticketStore.getAllTickets().stream()
                .filter(ticket -> ticket.getReporter().contains(name))
                .collect(Collectors.toList());
    }

    private List<Ticket> getTicketsByNameAndType(String name, Type type) {
        return ticketStore.getAllTickets().stream()
                .filter(ticket -> ticket.getReporter().contains(name) && ticket.getType() == type)
                .collect(Collectors.toList());
    }
}
