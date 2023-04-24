package org.atlassian.pojo;


import java.util.Objects;

/** A message object that Flink recognizes as a valid POJO. */
public class Message {

    public String user;
    public String message;
    public String environment;
    public String service_id;
    public String timestamp;

    /** A Flink POJO must have a no-args default constructor */
    public Message() {}

    public Message(String user, String message, String environment, String service_id, String timestamp) {
        this.user = user;
        this.message = message;
        this.environment = environment;
        this.service_id = service_id;
        this.timestamp = timestamp;
    }

    /** Used for printing during development */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Message other = (Message) o;
        return user.equals(other.user)
                && message.equals(other.message)
                && environment.equals(other.environment)
                && service_id.equals(other.service_id)
                && timestamp.equals(other.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, message, environment, service_id, timestamp);
    }

    @Override
    public String toString() {
        return "Message{" + "user='" + user + '\'' + ", message='" + message + '\'' + ", environment='" + environment + '\'' + ", service_id='" + service_id + '\'' + "timestamp=" + timestamp + '\'' +'}';
    }
}
