package de.uniba.rz.app;

import com.rabbitmq.client.*;
import de.uniba.rz.entities.*;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class AmqpTicketManagement implements TicketManagementBackend, TicketSearchBackend {

    private static final String REQUEST_QUEUE_NAME = "ticket_requests";
    private static final String EXCHANGE_NAME = "ticket_updates";

    private AtomicInteger nextId;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String replyQueueName;
    private BlockingQueue<Delivery> replyQueue;
    private BlockingQueue<Delivery> updatesQueue;

    public AmqpTicketManagement(String host) {
        this.factory = new ConnectionFactory();
        factory.setHost(host);
        try {
            this.nextId = new AtomicInteger(1);
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            this.replyQueueName = channel.queueDeclare().getQueue();
            this.replyQueue = new ArrayBlockingQueue<>(1);
            this.updatesQueue = new ArrayBlockingQueue<>(1);

            channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    replyQueue.offer(new Delivery(body, properties));
                }
            });

            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            String updatesQueueName = channel.queueDeclare().getQueue();
            channel.queueBind(updatesQueueName, EXCHANGE_NAME, "");
            channel.basicConsume(updatesQueueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    updatesQueue.offer(new Delivery(body, properties));
                    handleUpdate(new Delivery(body, properties));
                }
            });
        } catch (IOException | TimeoutException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleUpdate(Delivery delivery) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(delivery.getBody()))) {
            Object update = inputStream.readObject();
            if (update instanceof Ticket) {
                Ticket ticket = (Ticket) update;
                System.out.println("Received update for ticket: " + ticket);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling update: " + e.getMessage());
        }
    }

    @Override
    public void triggerShutdown() {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public Ticket createNewTicket(String reporter, String topic, String description, Type type, Priority priority) throws TicketException {
        Ticket createdTicket = new Ticket(nextId.getAndIncrement(), reporter, topic, description, type, priority);
        return (Ticket) sendRequest("CREATE", createdTicket);
    }

    @Override
    public List<Ticket> getAllTickets() throws TicketException {
        return (List<Ticket>) sendRequest("GET_ALL", null);
    }

    private Object sendRequest(String requestType, Object payload) throws TicketException {
        try {
            String correlationId = UUID.randomUUID().toString();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeUTF(requestType);
            os.writeObject(payload);
            os.flush();
            byte[] messageData = out.toByteArray();

            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(correlationId)
                    .replyTo(replyQueueName)
                    .build();

            channel.basicPublish("", REQUEST_QUEUE_NAME, props, messageData);

            while (true) {
                Delivery delivery = replyQueue.take();
                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                    try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(delivery.getBody()))) {
                        return inputStream.readObject();
                    }
                }
            }
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            throw new TicketException("Error: " + e.getMessage());
        }
    }

    @Override
    public Ticket getTicketById(int id) throws TicketException {
        return (Ticket) sendRequest("GET_BY_ID", id);
    }

    @Override
    public Ticket acceptTicket(int id) throws TicketException {
        return (Ticket) sendRequest("ACCEPT", id);
    }

    @Override
    public Ticket rejectTicket(int id) throws TicketException {
        return (Ticket) sendRequest("REJECT", id);
    }

    @Override
    public Ticket closeTicket(int id) throws TicketException {
        return (Ticket) sendRequest("CLOSE", id);
    }

    @Override
    public List<Ticket> getTicketsByName(String name) throws TicketException {
        return (List<Ticket>) sendRequest("GET_BY_NAME", name);
    }

    @Override
    public List<Ticket> getTicketsByNameAndType(String name, Type type) throws TicketException {
        return (List<Ticket>) sendRequest("GET_BY_NAME_AND_TYPE", new NameAndTypePayload(name, type));
    }

    private static class Delivery {
        private final byte[] body;
        private final AMQP.BasicProperties properties;

        public Delivery(byte[] body, AMQP.BasicProperties properties) {
            this.body = body;
            this.properties = properties;
        }

        public byte[] getBody() {
            return body;
        }

        public AMQP.BasicProperties getProperties() {
            return properties;
        }
    }
}
