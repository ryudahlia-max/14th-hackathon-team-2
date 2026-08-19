package com.team2.wellness.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ParsingController())
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    void malformedEnumJsonIsBadRequestInsteadOfInternalServerError() throws Exception {
        mvc.perform(post("/test/enum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"NOT_A_REAL_VALUE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void malformedUuidPathIsBadRequestInsteadOfInternalServerError() throws Exception {
        mvc.perform(get("/test/uuid/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void oversizedMultipartIsPayloadTooLarge() {
        var response = handler.handleMaxUploadSize(
                new MaxUploadSizeExceededException(10),
                mock(HttpServletRequest.class)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
    }

    @RestController
    static class ParsingController {
        enum Value { VALID }
        record Body(Value value) { }

        @GetMapping("/test/uuid/{id}")
        UUID uuid(@PathVariable UUID id) { return id; }

        @PostMapping("/test/enum")
        Body body(@RequestBody Body body) { return body; }
    }
}
