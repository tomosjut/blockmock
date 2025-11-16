package nl.blockmock.domain;

public enum WebSocketOperation {
    ECHO,      // Echo back received messages
    BROADCAST, // Broadcast messages to all connected clients
    CUSTOM     // Custom behavior defined by auto-response
}
