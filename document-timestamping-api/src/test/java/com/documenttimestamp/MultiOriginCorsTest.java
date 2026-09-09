package com.documenttimestamp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "app.cors.allowed-origins=http://localhost:3000,https://timestamps.example.com")
class MultiOriginCorsTest {

    private static final String UPLOAD = "/api/v1/documents/";

    @Autowired
    private MockMvc mockMvc;

    private MvcResult preflight(String origin) throws Exception {
        return mockMvc.perform(options(UPLOAD)
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST"))
                .andReturn();
    }

    @Test
    void everyOriginInTheListIsAllowed() throws Exception {
        for (String origin : new String[]{"http://localhost:3000", "https://timestamps.example.com"}) {
            MvcResult result = preflight(origin);
            assertEquals(200, result.getResponse().getStatus(),
                    origin + " is configured and must survive preflight");
            assertEquals(origin, result.getResponse().getHeader("Access-Control-Allow-Origin"),
                    origin + " must be echoed back as the allowed origin");
        }
    }

    @Test
    void anOriginOutsideTheListIsStillRefused() throws Exception {
        MvcResult result = preflight("https://not-configured.example");

        assertEquals(403, result.getResponse().getStatus());
        assertNull(result.getResponse().getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void theRawPropertyValueIsNotItselfAnAllowedOrigin() throws Exception {
        MvcResult result = preflight("http://localhost:3000,https://timestamps.example.com");

        assertEquals(403, result.getResponse().getStatus(),
                "the unsplit property value must not match as a single origin");
    }
}
