package com.ballastlane.pokeproject.api.filter;

import com.ballastlane.pokeproject.util.LogSanitizer;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditHeadersFilterTest {

    private final AuditHeadersFilter filter = new AuditHeadersFilter(new LogSanitizer());

    @Test
    void capturesAllAuditHeadersIntoTheRequestAndTheMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pokemon");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        request.addHeader("x-user", "ash");
        request.addHeader("x-latitude", "-16.5");
        request.addHeader("x-longitude", "-68.15");
        request.addHeader("x-channel", "WEB");
        Map<String, String> mdcDuringRequest = new HashMap<>();
        FilterChain chain = (req, res) -> mdcDuringRequest.putAll(MDC.getCopyOfContextMap());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        AuditContext audit = (AuditContext) request.getAttribute(AuditContext.ATTRIBUTE);
        assertThat(audit).isEqualTo(new AuditContext("203.0.113.7", "ash", -16.5, -68.15, "WEB"));
        assertThat(mdcDuringRequest).containsEntry("clientIp", "203.0.113.7").containsEntry("xUser", "ash")
                .containsEntry("channel", "WEB");
        assertThat(MDC.getCopyOfContextMap()).as("MDC cleared after the request").isNullOrEmpty();
    }

    @Test
    void missingOrInvalidHeadersNeverFailTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pokemon");
        request.setRemoteAddr("198.51.100.1");
        request.addHeader("x-latitude", "91");
        request.addHeader("x-longitude", "east");
        request.addHeader("x-user", "ash\r\n[REQUEST] forged");
        boolean[] chainCalled = {false};

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> chainCalled[0] = true);

        AuditContext audit = (AuditContext) request.getAttribute(AuditContext.ATTRIBUTE);
        assertThat(chainCalled[0]).isTrue();
        assertThat(audit.clientIp()).as("falls back to the socket address").isEqualTo("198.51.100.1");
        assertThat(audit.latitude()).as("out of range").isNull();
        assertThat(audit.longitude()).as("not a number").isNull();
        assertThat(audit.channel()).isNull();
        assertThat(audit.user()).doesNotContain("\n", "\r");
    }
}
