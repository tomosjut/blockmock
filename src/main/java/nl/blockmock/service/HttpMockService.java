package nl.blockmock.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.HttpMockEndpoint;
import nl.blockmock.domain.MockEndpoint;
import nl.blockmock.domain.MockResponse;
import nl.blockmock.domain.ProtocolType;
import nl.blockmock.domain.RequestLog;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Matches incoming HTTP requests against enabled endpoints, selects a response,
 * logs the interaction, and updates endpoint metrics.
 * Unmatched requests return HTTP 404 and are logged as unmatched.
 */
@ApplicationScoped
public class HttpMockService {

    @Inject
    MockEndpointService mockEndpointService;

    @Inject
    RequestLogService requestLogService;

    @Transactional
    public HttpMockResponse handleRequest(String method, String path,
                                         Map<String, String> headers,
                                         Map<String, String> queryParams,
                                         String body,
                                         String clientIp) {

        List<MockEndpoint> endpoints = mockEndpointService.findEnabledByProtocol(ProtocolType.HTTP);

        HttpMockEndpoint matchedEndpoint = null;
        MockResponse matchedResponse = null;

        for (MockEndpoint endpoint : endpoints) {
            if (endpoint instanceof HttpMockEndpoint http && matchesEndpoint(http, method, path)) {
                matchedResponse = findMatchingResponse(endpoint, headers, queryParams, body);
                if (matchedResponse != null) {
                    matchedEndpoint = http;
                    break;
                }
            }
        }

        RequestLog log = createRequestLog(matchedEndpoint, matchedResponse,
                                         method, path, headers, queryParams, body, clientIp);

        if (matchedResponse != null) {
            log.setMatched(true);
            log.setResponseStatusCode(matchedResponse.getResponseStatusCode());
            log.setResponseHeaders(matchedResponse.getResponseHeaders());
            log.setResponseBody(matchedResponse.getResponseBody());
            log.setResponseDelayMs(matchedResponse.getResponseDelayMs());

            requestLogService.log(log);
            updateMetrics(matchedEndpoint, true);

            return new HttpMockResponse(
                matchedResponse.getResponseStatusCode() != null ? matchedResponse.getResponseStatusCode() : 200,
                matchedResponse.getResponseHeaders() != null ? matchedResponse.getResponseHeaders() : new HashMap<>(),
                matchedResponse.getResponseBody(),
                matchedResponse.getResponseDelayMs() != null ? matchedResponse.getResponseDelayMs() : 0
            );
        }

        log.setMatched(false);
        log.setResponseStatusCode(404);
        log.setResponseBody("No mock found for: " + method + " " + path);

        requestLogService.log(log);

        return new HttpMockResponse(404, new HashMap<>(),
                                   "No mock found for: " + method + " " + path, 0);
    }

    private boolean matchesEndpoint(HttpMockEndpoint endpoint, String method, String path) {
        if (endpoint.getHttpMethod() == null || endpoint.getHttpPath() == null) {
            return false;
        }

        if (!endpoint.getHttpMethod().name().equalsIgnoreCase(method)) {
            return false;
        }

        if (Boolean.TRUE.equals(endpoint.getHttpPathRegex())) {
            Pattern pattern = Pattern.compile(endpoint.getHttpPath());
            return pattern.matcher(path).matches();
        } else {
            return endpoint.getHttpPath().equals(path);
        }
    }

    private MockResponse findMatchingResponse(MockEndpoint endpoint,
                                              Map<String, String> headers,
                                              Map<String, String> queryParams,
                                              String body) {
        if (endpoint.getForcedResponse() != null) {
            return endpoint.getForcedResponse();
        }

        List<MockResponse> responses = endpoint.getResponses().stream()
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                .toList();

        for (MockResponse response : responses) {
            if (matchesResponse(response, headers, queryParams, body)) {
                return response;
            }
        }

        return null;
    }

    /**
     * Checks whether a request matches a response's criteria. Body matching supports
     * substring match or regex when the pattern is wrapped in {@code /pattern/}.
     * A response with no criteria set matches any request.
     */
    private boolean matchesResponse(MockResponse response,
                                    Map<String, String> headers,
                                    Map<String, String> queryParams,
                                    String body) {
        if (response.getMatchHeaders() != null && !response.getMatchHeaders().isEmpty()) {
            for (Map.Entry<String, String> entry : response.getMatchHeaders().entrySet()) {
                String headerValue = headers.get(entry.getKey().toLowerCase());
                if (headerValue == null || !headerValue.equals(entry.getValue())) {
                    return false;
                }
            }
        }

        if (response.getMatchQueryParams() != null && !response.getMatchQueryParams().isEmpty()) {
            for (Map.Entry<String, String> entry : response.getMatchQueryParams().entrySet()) {
                String paramValue = queryParams.get(entry.getKey());
                if (paramValue == null || !paramValue.equals(entry.getValue())) {
                    return false;
                }
            }
        }

        if (response.getMatchBody() != null && !response.getMatchBody().isEmpty()) {
            if (body == null) {
                return false;
            }

            String matchPattern = response.getMatchBody();

            if (matchPattern.startsWith("/") && matchPattern.endsWith("/") && matchPattern.length() > 2) {
                String regex = matchPattern.substring(1, matchPattern.length() - 1);
                try {
                    if (!Pattern.compile(regex).matcher(body).find()) {
                        return false;
                    }
                } catch (Exception e) {
                    if (!body.equals(matchPattern)) {
                        return false;
                    }
                }
            } else {
                if (!body.contains(matchPattern)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void updateMetrics(MockEndpoint endpoint, boolean matched) {
        if (endpoint == null) return;
        MockEndpoint managed = MockEndpoint.findById(endpoint.id);
        if (managed == null) return;
        managed.setTotalRequests(managed.getTotalRequests() + 1);
        if (matched) {
            managed.setMatchedRequests(managed.getMatchedRequests() + 1);
        } else {
            managed.setUnmatchedRequests(managed.getUnmatchedRequests() + 1);
        }
        managed.setLastRequestAt(LocalDateTime.now());
    }

    private RequestLog createRequestLog(MockEndpoint endpoint, MockResponse response,
                                       String method, String path,
                                       Map<String, String> headers,
                                       Map<String, String> queryParams,
                                       String body,
                                       String clientIp) {
        RequestLog log = new RequestLog();
        log.setMockEndpoint(endpoint);
        log.setMockResponse(response);
        log.setProtocol(ProtocolType.HTTP);
        log.setRequestMethod(method);
        log.setRequestPath(path);
        log.setRequestHeaders(headers);
        log.setRequestQueryParams(queryParams);
        log.setRequestBody(body);
        log.setClientIp(clientIp);
        return log;
    }

    public record HttpMockResponse(
        int statusCode,
        Map<String, String> headers,
        String body,
        int delayMs
    ) {}
}
