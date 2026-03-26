package nl.blockmock.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.AmqpMockEndpoint;
import nl.blockmock.domain.Block;
import nl.blockmock.domain.MockEndpoint;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * CRUD and lifecycle management for blocks.
 * A block groups mock endpoints together; starting/stopping a block enables or disables
 * all its endpoints and activates or deactivates AMQP consumers where applicable.
 */
@ApplicationScoped
public class BlockService {

    @Inject
    MockEndpointService mockEndpointService;

    @Inject
    AmqpConnectionService amqpConnectionService;

    @Transactional
    public Block create(Block block) {
        block.persist();
        return block;
    }

    @Transactional
    public Block update(Block block) {
        return Block.getEntityManager().merge(block);
    }

    @Transactional
    public void delete(Long id) {
        Block.deleteById(id);
    }

    public Optional<Block> findById(Long id) {
        return Block.findByIdOptional(id);
    }

    public List<Block> findAll() {
        return Block.listAll();
    }

    public Optional<Block> findByName(String name) {
        return Block.find("name", name).firstResultOptional();
    }

    @Transactional
    public void addEndpointToBlock(Long blockId, Long endpointId) {
        Block block = Block.findById(blockId);
        MockEndpoint endpoint = MockEndpoint.findById(endpointId);

        if (block == null) {
            throw new IllegalArgumentException("Block not found with id: " + blockId);
        }
        if (endpoint == null) {
            throw new IllegalArgumentException("MockEndpoint not found with id: " + endpointId);
        }

        block.addEndpoint(endpoint);
        block.persist();
    }

    @Transactional
    public void removeEndpointFromBlock(Long blockId, Long endpointId) {
        Block block = Block.findById(blockId);
        MockEndpoint endpoint = MockEndpoint.findById(endpointId);

        if (block == null) {
            throw new IllegalArgumentException("Block not found with id: " + blockId);
        }
        if (endpoint == null) {
            throw new IllegalArgumentException("MockEndpoint not found with id: " + endpointId);
        }

        block.removeEndpoint(endpoint);
        block.persist();
    }

    /**
     * Enables all endpoints in the block. For AMQP endpoints with pattern RECEIVE or
     * REQUEST_REPLY, also starts an AMQP consumer on the broker. Idempotent if already running.
     */
    @Transactional
    public void startBlock(Long blockId) {
        Block block = Block.findById(blockId);
        if (block == null) {
            throw new IllegalArgumentException("Block not found with id: " + blockId);
        }

        for (MockEndpoint endpoint : block.getEndpoints()) {
            endpoint.setEnabled(true);
            endpoint.persist();

            if (endpoint instanceof AmqpMockEndpoint amqp && isConsumerPattern(amqp)) {
                amqpConnectionService.startConsumer(amqp);
            }
        }
    }

    /**
     * Disables all endpoints in the block and closes any open AMQP consumers.
     * Called automatically by {@link TestSuiteService} when no other run is still active.
     */
    @Transactional
    public void stopBlock(Long blockId) {
        Block block = Block.findById(blockId);
        if (block == null) {
            throw new IllegalArgumentException("Block not found with id: " + blockId);
        }

        for (MockEndpoint endpoint : block.getEndpoints()) {
            endpoint.setEnabled(false);
            endpoint.persist();

            if (endpoint instanceof AmqpMockEndpoint amqp && isConsumerPattern(amqp)) {
                amqpConnectionService.stopConsumer(amqp);
            }
        }
    }

    private boolean isConsumerPattern(AmqpMockEndpoint endpoint) {
        String pattern = endpoint.getAmqpPattern();
        return "RECEIVE".equals(pattern) || "REQUEST_REPLY".equals(pattern);
    }

    public Set<MockEndpoint> getBlockEndpoints(Long blockId) {
        Block block = Block.findById(blockId);
        if (block == null) {
            throw new IllegalArgumentException("Block not found with id: " + blockId);
        }
        return block.getEndpoints();
    }

    public List<Block> findBlocksForEndpoint(Long endpointId) {
        return Block.find("SELECT b FROM Block b JOIN b.endpoints e WHERE e.id = ?1", endpointId).list();
    }
}
