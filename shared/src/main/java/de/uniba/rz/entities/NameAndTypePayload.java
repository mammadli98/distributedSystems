package de.uniba.rz.entities;

import java.io.Serializable;

public class NameAndTypePayload implements Serializable {
    private String name;
    private Type type;

    public NameAndTypePayload(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }
}
