package nl.blockmock.domain;

public enum AmqpExchangeType {
    DIRECT,   // Routes messages based on exact routing key match
    FANOUT,   // Broadcasts messages to all bound queues
    TOPIC,    // Routes messages based on wildcard pattern matching
    HEADERS   // Routes messages based on header attributes
}
