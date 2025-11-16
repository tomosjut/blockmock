package nl.blockmock.domain;

public enum AmqpOperation {
    PUBLISH,  // Mock will accept published messages
    CONSUME,  // Mock will provide messages to consumers
    BOTH      // Mock supports both publish and consume
}
