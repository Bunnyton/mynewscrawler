package org.example.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Task implements Runnable{

    public enum Type {
        UNKNOWN(-1),
        GET_LINKS(0),
        PARSE_PAGE(1),
        ELASTIC_SEARCH_ADD(2),
        ELASTIC_SEARCH_CHECK_LINK(3);

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

