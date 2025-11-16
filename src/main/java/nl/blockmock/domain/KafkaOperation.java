package nl.blockmock.domain;

public enum KafkaOperation {
    PRODUCE,  // Produce messages to topic
    CONSUME,  // Consume messages from topic
    BOTH      // Both produce and consume
}
