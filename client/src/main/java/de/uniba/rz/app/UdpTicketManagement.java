package de.uniba.rz.app;

import de.uniba.rz.entities.Priority;
import de.uniba.rz.entities.Ticket;
import de.uniba.rz.entities.TicketException;
import de.uniba.rz.entities.Type;
import de.uniba.rz.entities.NameAndTypePayload;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UdpTicketManagement implements TicketManagementBackend, TicketSearchBackend {

    private HashMap<Integer, Ticket> localTicketStore = new HashMap<>();
    private AtomicInteger nextId;
    private final String host;
    private final int port;
    private final InetSocketAddress serverAddress;
    private DatagramSocket clientSocket;
    private final byte[] buffer = new byte[2048];

    public UdpTicketManagement(String host, int port) {
        this.host = host;
        this.port = port;
        this.nextId = new AtomicInteger(1);
        this.serverAddress = new InetSocketAddress(host, port);
        try {
            this.clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void triggerShutdown() {
        clientSocket.close();
    }

    @Override
    public Ticket createNewTicket(String reporter, String topic, String description, Type type, Priority priority) throws TicketException {
        Ticket createdTicket = new Ticket(nextId.getAndIncrement(), reporter, topic, description, type, priority);
        localTicketStore.put(createdTicket.getId(), createdTicket);
        sendRequest("CREATE", createdTicket.clone());
        Object reply = receiveReply();
        return (Ticket) reply;
    }

    @Override
    public List<Ticket> getAllTickets() throws TicketException {
        sendRequest("GET_ALL", null);
        Object reply = receiveReply();
        return (List<Ticket>) reply;
    }

    private Object receiveReply() throws TicketException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            clientSocket.setSoTimeout(30000);
            clientSocket.receive(packet);
            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(buffer, 0, packet.getLength()));
            Object reply = inputStream.readObject();
            inputStream.close();
            return reply;
        } catch (SocketTimeoutException e) {
            throw new TicketException("Couldn't receive reply from server! Try again.");
        } catch (IOException | ClassNotFoundException e) {
            throw new TicketException("Error: " + e.getMessage());
        }
    }

    private void sendRequest(String requestType, Object payload) throws TicketException {
        DatagramPacket packet;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ObjectOutputStream os = new ObjectOutputStream(out)) {
            os.writeUTF(requestType);
            os.writeObject(payload);
            os.flush();
            byte[] messageData = out.toByteArray();
            packet = new DatagramPacket(messageData, messageData.length, serverAddress);
            clientSocket.send(packet);
        } catch (IOException e) {
            throw new TicketException("Error: " + e.getMessage());
        }
    }

    @Override
    public Ticket getTicketById(int id) throws TicketException {
        sendRequest("GET_BY_ID", id);
        Object reply = receiveReply();
        return (Ticket) reply;
    }

    @Override
    public Ticket acceptTicket(int id) throws TicketException {
        sendRequest("ACCEPT", id);
        Object reply = receiveReply();
        return (Ticket) reply;
    }

    @Override
    public Ticket rejectTicket(int id) throws TicketException {
        sendRequest("REJECT", id);
        Object reply = receiveReply();
        return (Ticket) reply;
    }

    @Override
    public Ticket closeTicket(int id) throws TicketException {
        sendRequest("CLOSE", id);
        Object reply = receiveReply();
        return (Ticket) reply;
    }

    @Override
    public List<Ticket> getTicketsByName(String name) throws TicketException {
        sendRequest("GET_BY_NAME", name);
        Object reply = receiveReply();
        return (List<Ticket>) reply;
    }

    @Override
    public List<Ticket> getTicketsByNameAndType(String name, Type type) throws TicketException {
        sendRequest("GET_BY_NAME_AND_TYPE", new NameAndTypePayload(name, type));
        Object reply = receiveReply();
        return (List<Ticket>) reply;
    }
}
