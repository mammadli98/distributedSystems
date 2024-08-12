package de.uniba.rz.entities;

import jakarta.xml.bind.annotation.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"id", "reporter", "topic", "description", "type", "priority", "status"})
public class TicketREST {
    @XmlAttribute(required = true)
    private int id;
    @XmlAttribute(required = true)
    private String reporter;
    @XmlAttribute(required = true)
    private String topic;
    @XmlAttribute(required = true)
    private String description;
    @XmlAttribute(required = true)
    private Type type;
    @XmlAttribute(required = true)
    private Priority priority;
    @XmlAttribute(required = true)
    private Status status;

        public TicketREST() {}

        public TicketREST(Ticket ticket) {
            this.id = ticket.getId();
            this.reporter = ticket.getReporter();
            this.topic = ticket.getTopic();
            this.description = ticket.getDescription();
            this.type = ticket.getType();
            this.priority = ticket.getPriority();
            this.status = ticket.getStatus();
        }


    public TicketREST(int id, String reporter, String topic, String description, Type type, Priority priority) {
            super();
            this.id = id;
            this.reporter = reporter;
            this.topic = topic;
            this.description = description;
            this.type = type;
            this.priority = priority;
            this.setStatus(Status.NEW);
        }

        public TicketREST(int id, String reporter, String topic, String description, Type type, Priority priority,
                      Status status) {
            super();
            this.id = id;
            this.reporter = reporter;
            this.topic = topic;
            this.description = description;
            this.type = type;
            this.priority = priority;
            this.status = status;
        }

        public int getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Priority getPriority() {
            return priority;
        }

        public void setPriority(Priority priority) {
            this.priority = priority;
        }

        public String getReporter() {
            return reporter;
        }

        public Status getStatus() {
            return status;
        }

        public String getTopic() {
            return topic;
        }

        public Type getType() {
            return type;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setReporter(String reporter) {
            this.reporter = reporter;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "Ticket #" + id + ": " + topic + " (reported by: " + reporter + ")\n" + "Status: " + status + "\t Type:"
                    + type + "\t Priority: " + priority + "\n" + "Description:\n" + description;
        }

        @Override
        public Object clone() {
            return new de.uniba.rz.entities.Ticket(this.id, this.reporter, this.topic, this.description, this.type, this.priority, this.status);
        }
        public Ticket restToTicket(){

            return new Ticket(this.getId(), this.getReporter(), this.getTopic(), this.getDescription(), this.getType(), this.getPriority());
        }

    }

