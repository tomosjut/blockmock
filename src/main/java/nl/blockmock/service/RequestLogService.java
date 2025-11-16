package nl.blockmock.service;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.ProtocolType;
import nl.blockmock.domain.RequestLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RequestLogService {

    @Transactional
    public RequestLog log(RequestLog requestLog) {
        requestLog.persist();
        return requestLog;
    }

    public List<RequestLog> findAll() {
        return RequestLog.listAll(Sort.descending("receivedAt"));
    }

    public List<RequestLog> findRecent(int limit) {
        return RequestLog.findAll(Sort.descending("receivedAt"))
                .page(0, limit)
                .list();
    }

    public List<RequestLog> findByEndpoint(Long endpointId) {
        return RequestLog.list("mockEndpoint.id = ?1", Sort.descending("receivedAt"), endpointId);
    }

    public List<RequestLog> findByProtocol(ProtocolType protocol) {
        return RequestLog.list("protocol", Sort.descending("receivedAt"), protocol);
    }

    public List<RequestLog> findMatched(boolean matched) {
        return RequestLog.list("matched", Sort.descending("receivedAt"), matched);
    }

    public List<RequestLog> findBetween(LocalDateTime start, LocalDateTime end) {
        return RequestLog.list("receivedAt >= ?1 and receivedAt <= ?2",
                Sort.descending("receivedAt"), start, end);
    }

    public Optional<RequestLog> findById(Long id) {
        return RequestLog.findByIdOptional(id);
    }

    @Transactional
    public void deleteOlderThan(LocalDateTime cutoff) {
        RequestLog.delete("receivedAt < ?1", cutoff);
    }

    @Transactional
    public void deleteAll() {
        RequestLog.deleteAll();
    }

    public long countByEndpoint(Long endpointId) {
        return RequestLog.count("mockEndpoint.id", endpointId);
    }

    public long countMatched() {
        return RequestLog.count("matched", true);
    }

    public long countUnmatched() {
        return RequestLog.count("matched", false);
    }
}
