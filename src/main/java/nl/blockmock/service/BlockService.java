package nl.blockmock.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.Block;
import nl.blockmock.domain.MockEndpoint;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class BlockService {

    @Inject
    MockEndpointService mockEndpointService;

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

    @Transactional
    public void startBlock(Long blockId) {
        Block block = Block.findById(blockId);
        if (block == null) {
            throw new IllegalArgumentException("Block not found with id: " + blockId);
        }

        // Enable all endpoints in this block
        for (MockEndpoint endpoint : block.getEndpoints()) {
            endpoint.setEnabled(true);
            endpoint.persist();
        }
    }

    @Transactional
    public void stopBlock(Long blockId) {
        Block block = Block.findById(blockId);
        if (block == null) {
            throw new IllegalArgumentException("Block not found with id: " + blockId);
        }

        // Disable all endpoints in this block
        for (MockEndpoint endpoint : block.getEndpoints()) {
            endpoint.setEnabled(false);
            endpoint.persist();
        }
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
