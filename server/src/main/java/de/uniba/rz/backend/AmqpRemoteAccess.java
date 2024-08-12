package de.uniba.rz.backend;

import com.rabbitmq.client.*;
import de.uniba.rz.entities.*;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AmqpRemoteAccess extends Thread implements RemoteAccess {

    private static final String REQUEST_QUEUE_NAME = "ticket_requests";
    private static final String EXCHANGE_NAME = "ticket_updates";

    private TicketStore ticketStore;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private AtomicBoolean flag = new AtomicBoolean(true);

    public AmqpRemoteAccess(String host) {
        this.factory = new ConnectionFactory();
        factory.setHost(host);
        try {
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            channel.queueDeclare(REQUEST_QUEUE_NAME, false, false, false, null);
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        } catch (IOException | TimeoutException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void prepareStartup(TicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    @Override
    public void shutdown() {
        flag.set(false);
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            System.err.println(e.getMessage());
        }
    }

    public void run() {
        System.out.println("AMQP Server is running");
        try {
            while (flag.get()) {
                receiveRequest();
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!(e instanceof EOFException)) {
                System.err.println(e.getMessage());
            }
        } catch (TicketException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveRequest() throws IOException, ClassNotFoundException, TicketException {
        GetResponse response = channel.basicGet(REQUEST_QUEUE_NAME, true);
        if (response != null) {
            byte[] body = response.getBody();
            AMQP.BasicProperties props = response.getProps();
            String replyTo = props.getReplyTo();
            String correlationId = props.getCorrelationId();

            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(body));
            String requestType = inputStream.readUTF();
            Object payload = inputStream.readObject();
            inputStream.close();

            Object result = processRequest(requestType, payload);

            sendResponse(replyTo, correlationId, result);
            publishUpdate(result);
        }
    }

    private Object processRequest(String requestType, Object payload) throws TicketException {
        switch (requestType) {
            case "CREATE":
                Ticket newTicket = (Ticket) payload;
                ticketStore.storeNewTicket(newTicket.getReporter(), newTicket.getTopic(), newTicket.getDescription(), newTicket.getType(), newTicket.getPriority());
                return newTicket;
            case "GET_ALL":
                return ticketStore.getAllTickets();
            case "GET_BY_ID":
                int ticketId = (Integer) payload;
                return getTicketById(ticketId);
            case "ACCEPT":
                int acceptTicketId = (Integer) payload;
                return acceptTicket(acceptTicketId);
            case "REJECT":
                int rejectedTicketId = (Integer) payload;
                return rejectTicket(rejectedTicketId);
            case "CLOSE":
                int closedTicketId = (Integer) payload;
                return closeTicket(closedTicketId);
            case "GET_BY_NAME":
                String name = (String) payload;
                return getTicketsByName(name);
            case "GET_BY_NAME_AND_TYPE":
                NameAndTypePayload nameAndTypePayload = (NameAndTypePayload) payload;
                return getTicketsByNameAndType(nameAndTypePayload.getName(), nameAndTypePayload.getType());
            default:
                throw new TicketException("Unknown request type: " + requestType);
        }
    }

    private void sendResponse(String replyTo, String correlationId, Object response) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(response);
        os.flush();
        byte[] messageData = out.toByteArray();

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(correlationId)
                .build();

        channel.basicPublish("", replyTo, replyProps, messageData);
    }

    private void publishUpdate(Object update) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(update);
        os.flush();
        byte[] messageData = out.toByteArray();

        channel.basicPublish(EXCHANGE_NAME, "", null, messageData);
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
