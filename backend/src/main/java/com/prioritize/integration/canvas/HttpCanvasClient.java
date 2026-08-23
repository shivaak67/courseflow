package com.prioritize.integration.canvas;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.prioritize.config.CanvasProperties;
import com.prioritize.exception.ApiException;

import org.springframework.http.HttpStatus;

@Component
@ConditionalOnProperty(name = "app.canvas.mock-enabled", havingValue = "false", matchIfMissing = true)
public class HttpCanvasClient implements CanvasClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public HttpCanvasClient(CanvasProperties properties) {
        String baseUrl = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().replaceAll("/$", "");
        String token = properties.getApiToken() == null ? "" : properties.getApiToken();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<CanvasCourseData> listCourses() {
        List<Map<String, Object>> rows = getPaginated("/api/v1/courses?enrollment_state=active&per_page=100");
        List<CanvasCourseData> courses = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            if (id == null) {
                continue;
            }
            String name = stringValue(row.get("name"));
            if (name == null || name.isBlank()) {
                continue;
            }
            courses.add(new CanvasCourseData(
                    String.valueOf(id),
                    name,
                    stringValue(row.get("course_code")),
                    extractTerm(row.get("term"))));
        }
        return courses;
    }

    @Override
    public List<CanvasAssignmentData> listAssignments(String canvasCourseId) {
        String path = "/api/v1/courses/" + canvasCourseId
                + "/assignments?per_page=100&include[]=submission&order_by=due_at";
        List<Map<String, Object>> rows = getPaginated(path);
        List<CanvasAssignmentData> assignments = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            Object published = row.get("published");
            if (id == null || Boolean.FALSE.equals(published)) {
                continue;
            }
            String title = stringValue(row.get("name"));
            if (title == null || title.isBlank()) {
                continue;
            }
            SubmissionState submission = parseSubmission(row.get("submission"));
            assignments.add(new CanvasAssignmentData(
                    String.valueOf(id),
                    title,
                    stringValue(row.get("description")),
                    parseInstant(row.get("due_at")),
                    parseDouble(row.get("points_possible")),
                    submission.submitted(),
                    submission.completed()));
        }
        return assignments;
    }

    private List<Map<String, Object>> getPaginated(String path) {
        List<Map<String, Object>> all = new ArrayList<>();
        String next = path;
        try {
            while (next != null) {
                var response = restClient.get()
                        .uri(next)
                        .retrieve()
                        .toEntity(LIST_OF_MAPS);
                List<Map<String, Object>> body = response.getBody();
                if (body != null) {
                    all.addAll(body);
                }
                next = parseNextLink(response.getHeaders().getFirst(HttpHeaders.LINK));
            }
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to reach Canvas LMS");
        }
        return all;
    }

    private static String parseNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        for (String part : linkHeader.split(",")) {
            String segment = part.trim();
            if (!segment.contains("rel=\"next\"")) {
                continue;
            }
            int start = segment.indexOf('<');
            int end = segment.indexOf('>');
            if (start >= 0 && end > start) {
                String url = segment.substring(start + 1, end);
                URI uri = URI.create(url);
                String query = uri.getRawQuery();
                return uri.getPath() + (query == null || query.isBlank() ? "" : "?" + query);
            }
        }
        return null;
    }

    private static String extractTerm(Object termNode) {
        if (termNode instanceof Map<?, ?> map) {
            Object name = map.get("name");
            return name == null ? null : String.valueOf(name);
        }
        return null;
    }

    private static SubmissionState parseSubmission(Object submissionNode) {
        if (!(submissionNode instanceof Map<?, ?> map)) {
            return new SubmissionState(false, false);
        }
        String workflow = stringValue(map.get("workflow_state"));
        if (workflow == null) {
            return new SubmissionState(false, false);
        }
        boolean submitted = switch (workflow) {
            case "submitted", "pending_review", "graded" -> true;
            default -> false;
        };
        boolean completed = "graded".equals(workflow);
        return new SubmissionState(submitted, completed);
    }

    private static Instant parseInstant(Object value) {
        String raw = stringValue(value);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Double parseDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String raw = stringValue(value);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String stringValue(Object value) {
        if (value == null || value instanceof JsonNode node && node.isNull()) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equals(text) ? null : text;
    }

    private record SubmissionState(boolean submitted, boolean completed) {
    }
}
