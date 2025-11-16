package nl.blockmock.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.blockmock.domain.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@ApplicationScoped
public class HttpMockService {

    @Inject
    MockEndpointService mockEndpointService;

    @Inject
    RequestLogService requestLogService;

    public HttpMockResponse handleRequest(String method, String path,
                                         Map<String, String> headers,
                                         Map<String, String> queryParams,
                                         String body,
                                         String clientIp) {

        // Find matching endpoint
        List<MockEndpoint> endpoints = mockEndpointService.findEnabledByProtocol(ProtocolType.HTTP);

        MockEndpoint matchedEndpoint = null;
        MockResponse matchedResponse = null;

        for (MockEndpoint endpoint : endpoints) {
            if (matchesEndpoint(endpoint, method, path)) {
                // Find matching response within this endpoint
                matchedResponse = findMatchingResponse(endpoint, headers, queryParams, body);
                if (matchedResponse != null) {
                    matchedEndpoint = endpoint;
                    break;
                }
            }
        }

        // Log the request
        RequestLog log = createRequestLog(matchedEndpoint, matchedResponse,
                                         method, path, headers, queryParams, body, clientIp);

        if (matchedResponse != null) {
            log.setMatched(true);
            log.setResponseStatusCode(matchedResponse.getResponseStatusCode());
            log.setResponseHeaders(matchedResponse.getResponseHeaders());
            log.setResponseBody(matchedResponse.getResponseBody());
            log.setResponseDelayMs(matchedResponse.getResponseDelayMs());

            requestLogService.log(log);

            return new HttpMockResponse(
                matchedResponse.getResponseStatusCode() != null ? matchedResponse.getResponseStatusCode() : 200,
                matchedResponse.getResponseHeaders() != null ? matchedResponse.getResponseHeaders() : new HashMap<>(),
                matchedResponse.getResponseBody(),
                matchedResponse.getResponseDelayMs() != null ? matchedResponse.getResponseDelayMs() : 0
            );
        }

        // No match found - return 404
        log.setMatched(false);
        log.setResponseStatusCode(404);
        log.setResponseBody("No mock found for: " + method + " " + path);

        requestLogService.log(log);

        return new HttpMockResponse(404, new HashMap<>(),
                                   "No mock found for: " + method + " " + path, 0);
    }

    private boolean matchesEndpoint(MockEndpoint endpoint, String method, String path) {
        HttpConfig config = endpoint.getHttpConfig();
        if (config == null) {
            return false;
        }

        // Check HTTP method
        if (!config.getMethod().name().equalsIgnoreCase(method)) {
            return false;
        }

        // Check path
        if (config.getPathRegex()) {
            Pattern pattern = Pattern.compile(config.getPath());
            return pattern.matcher(path).matches();
        } else {
            return config.getPath().equals(path);
        }
    }

    private MockResponse findMatchingResponse(MockEndpoint endpoint,
                                              Map<String, String> headers,
                                              Map<String, String> queryParams,
                                              String body) {
        // Get responses sorted by priority (highest first)
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

    private boolean matchesResponse(MockResponse response,
                                    Map<String, String> headers,
                                    Map<String, String> queryParams,
                                    String body) {
        // Check headers - all specified headers must match
        if (response.getMatchHeaders() != null && !response.getMatchHeaders().isEmpty()) {
            for (Map.Entry<String, String> entry : response.getMatchHeaders().entrySet()) {
                String headerValue = headers.get(entry.getKey().toLowerCase());
                if (headerValue == null || !headerValue.equals(entry.getValue())) {
                    return false;
                }
            }
        }

        // Check query params - all specified params must match
        if (response.getMatchQueryParams() != null && !response.getMatchQueryParams().isEmpty()) {
            for (Map.Entry<String, String> entry : response.getMatchQueryParams().entrySet()) {
                String paramValue = queryParams.get(entry.getKey());
                if (paramValue == null || !paramValue.equals(entry.getValue())) {
                    return false;
                }
            }
        }

        // Check body matching - supports regex or exact match
        if (response.getMatchBody() != null && !response.getMatchBody().isEmpty()) {
            if (body == null) {
                return false;
            }

            String matchPattern = response.getMatchBody();

            // Check if it's a regex pattern (starts and ends with /)
            if (matchPattern.startsWith("/") && matchPattern.endsWith("/") && matchPattern.length() > 2) {
                String regex = matchPattern.substring(1, matchPattern.length() - 1);
                try {
                    if (!Pattern.compile(regex).matcher(body).find()) {
                        return false;
                    }
                } catch (Exception e) {
                    // Invalid regex, treat as exact match
                    if (!body.equals(matchPattern)) {
                        return false;
                    }
                }
            } else {
                // Exact match or contains
                if (!body.contains(matchPattern)) {
                    return false;
                }
            }
        }

        // TODO: Implement script matching

        return true;
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
