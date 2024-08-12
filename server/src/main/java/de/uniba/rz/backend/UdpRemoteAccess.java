package de.uniba.rz.backend;

import de.uniba.rz.entities.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpRemoteAccess extends Thread implements RemoteAccess {

    private TicketStore ticketStore;
    private DatagramSocket serverSocket;
    private AtomicBoolean flag = new AtomicBoolean(true);
    private final int port;
    private final String host;
    private final Semaphore semaphore;

    public UdpRemoteAccess(String host, int port) {
        this.host = host;
        this.port = port;
        this.semaphore = new Semaphore(1);

        try {
            serverSocket = new DatagramSocket(new InetSocketAddress(host, port));
        } catch (SocketException e) {
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
        serverSocket.close();
    }

    public void run() {
        System.out.println("UDP Server is running");
        try {
            while (flag.get()) {
                receiveRequest();
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!(e instanceof SocketException)) {
                System.err.println(e.getMessage());
            }
        } catch (TicketException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveRequest() throws IOException, ClassNotFoundException, TicketException {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        serverSocket.receive(packet);

        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(buffer, 0, packet.getLength()));
        String requestType = inputStream.readUTF();
        Object payload = inputStream.readObject();
        inputStream.close();

        new UdpHandler(packet, serverSocket, ticketStore, requestType, payload, semaphore).start();
    }
}
