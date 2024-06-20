package org.example.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

public abstract class Task implements Runnable{

    public enum Type {
        UNKNOWN(-1),
        GET_LINKS(0),
        PARSE_PAGE(1);

        Type(Integer code) { this.code = code; }

        public Integer getCode() { return code; }

        private final Integer code;

    }

    public Type getType() { return type; }

    public abstract void run();

    @Override
    public String toString() { return getType().toString(); }

    @JsonIgnore
    protected Type type;
}

