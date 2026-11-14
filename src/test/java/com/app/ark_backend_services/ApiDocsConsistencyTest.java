package com.app.ark_backend_services;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Keeps the docs honest: every operation in /v3/api-docs must be listed in docs/architecture.md, and every
 * /api/v1 path mentioned in the docs must exist in /v3/api-docs. Update the docs when this test fails.
 */
@AutoConfigureMockMvc
class ApiDocsConsistencyTest extends AbstractMongoIntegrationTest {

    @Autowired MockMvc mvc;

    private static String normalize(String path) {
        return path.replaceAll("\\{[^}]+}", "{}");
    }

    @Test
    void documentationMatchesTheOpenApiSpec() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Map<String, Object> paths = JsonPath.read(json, "$.paths");
        Set<String> specPaths = paths.keySet().stream().map(ApiDocsConsistencyTest::normalize).collect(Collectors.toSet());

        Path docs = Path.of("docs");
        String architecture = Files.readString(docs.resolve("architecture.md"));
        String guide = Files.readString(docs.resolve("api-guide.md"));

        // 1. every operation is in the architecture reference
        List<String> undocumented = new ArrayList<>();
        for (Map.Entry<String, Object> p : paths.entrySet()) {
            @SuppressWarnings("unchecked")
            Set<String> verbs = ((Map<String, Object>) p.getValue()).keySet();
            for (String verb : verbs) {
                if (!architecture.contains("| " + verb.toUpperCase() + " | `" + p.getKey() + "` |")) {
                    undocumented.add(verb.toUpperCase() + " " + p.getKey());
                }
            }
        }
        assertThat(undocumented).as("operations missing from docs/architecture.md").isEmpty();

        // 2. every documented path exists
        Pattern docPath = Pattern.compile("`(?:(?:GET|POST|PUT|PATCH|DELETE)(?:\\|(?:GET|POST|PUT|PATCH|DELETE))* )?(/api/v1[^`\\s]*)`");
        List<String> unknown = new ArrayList<>();
        for (String doc : List.of(architecture, guide)) {
            Matcher m = docPath.matcher(doc);
            while (m.find()) {
                String path = m.group(1).replaceAll("\\([^)]*\\)", "").split("\\?")[0];
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (path.equals("/api/v1")) {
                    continue; // the base URL, not an endpoint
                }
                if (!specPaths.contains(normalize(path))) {
                    unknown.add(path);
                }
            }
        }
        assertThat(unknown).as("documented paths that do not exist").isEmpty();
    }
}
