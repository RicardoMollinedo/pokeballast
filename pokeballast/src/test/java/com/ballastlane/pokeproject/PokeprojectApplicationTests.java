package com.ballastlane.pokeproject;

import com.ballastlane.pokeproject.bl.port.PokemonCatalogPort;
import com.ballastlane.pokeproject.service.PokeApiResilience;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ballastlane.pokeproject.support.TestData.pokemonData;
import static com.ballastlane.pokeproject.support.TestData.speciesData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End to end through every real layer (security with real JWT validation, controllers, bl,
 * JPA, Flyway, H2). Only the PokeAPI port is replaced, so the test never depends on the network.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PokeprojectApplicationTests {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private PokemonCatalogPort catalog;
    @Autowired
    private PokeApiResilience resilience;
    @Autowired
    private JdbcTemplate jdbc;
    @Value("${security.jwt.secret}")
    private String secret;
    @Value("${security.jwt.issuer}")
    private String issuer;

    @Test
    void importUpdateFilterAndDeleteAPokemonThroughEveryLayer() throws Exception {
        when(catalog.fetchPokemon("mew")).thenReturn(pokemonData(151, "mew"));
        when(catalog.fetchSpecies("mew")).thenReturn(speciesData(null));
        String trainer = bearer("ash", "TRAINER", Instant.now().plusSeconds(600));
        String admin = bearer("admin", "ADMIN", Instant.now().plusSeconds(600));

        MvcResult created = mvc.perform(post("/api/v1/local-pokemon").header("Authorization", trainer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idOrName\":\"Mew\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.response.version").value(0))
                .andReturn();
        String id = extractId(created.getResponse().getContentAsString());

        mvc.perform(post("/api/v1/local-pokemon").header("Authorization", trainer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idOrName\":\"mew\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseCode").value("3001"));

        mvc.perform(put("/api/v1/local-pokemon/" + id).header("Authorization", trainer)
                        .header("x-user", "ash").header("x-channel", "WEB")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localizedName\":\"ミュウ\",\"region\":\"Kanto\",\"tags\":[\"Mythical\"],\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.tags[0]").value("mythical"))
                .andExpect(jsonPath("$.response.version").value(1));

        mvc.perform(put("/api/v1/local-pokemon/" + id).header("Authorization", trainer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tags\":[],\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseCode").value("3002"));

        mvc.perform(get("/api/v1/local-pokemon").param("tag", "mythical"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.content[0].localizedName").value("ミュウ"));

        mvc.perform(delete("/api/v1/local-pokemon/" + id).header("Authorization", trainer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("4003"));
        mvc.perform(delete("/api/v1/local-pokemon/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/local-pokemon/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("2002"));
    }

    @Test
    void registerLoginAndUseTheIssuedToken() throws Exception {
        when(catalog.fetchPokemon("jirachi")).thenReturn(pokemonData(385, "jirachi"));
        when(catalog.fetchSpecies("jirachi")).thenReturn(speciesData(null));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Brock\",\"password\":\"Onix12345\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.response.username").value("brock"))
                .andExpect(jsonPath("$.response.role").value("TRAINER"));
        String stored = jdbc.queryForObject("select password_hash from app_user where username = 'brock'", String.class);
        assertThat(stored).as("only a salted BCrypt hash (cost 12) is stored, never the password")
                .startsWith("$2a$12$").doesNotContain("Onix12345");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"BROCK\",\"password\":\"Onix12345\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseCode").value("3004"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"brock\",\"password\":\"wrong-pass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4002"));

        MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"brock\",\"password\":\"Onix12345\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Matcher token = Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(login.getResponse().getContentAsString());
        assertThat(token.find()).isTrue();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token.group(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.username").value("brock"));
        mvc.perform(post("/api/v1/local-pokemon").header("Authorization", "Bearer " + token.group(1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idOrName\":\"jirachi\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void resilienceLimitsComeFromApplicationYaml() {
        assertThat(resilience.retry().getRetryConfig().getMaxAttempts()).isEqualTo(3);
        assertThat(resilience.bulkhead().getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(20);
        assertThat(resilience.rateLimiter().getRateLimiterConfig().getLimitForPeriod()).isEqualTo(50);
        assertThat(resilience.circuitBreaker().getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50f);
    }

    @Test
    void behindTheHttpsProxyAbsoluteUrlsUseThePublicSchemeAndHost() throws Exception {
        when(catalog.fetchPokemon("celebi")).thenReturn(pokemonData(251, "celebi"));
        when(catalog.fetchSpecies("celebi")).thenReturn(speciesData(null));

        mvc.perform(post("/api/v1/local-pokemon").header("Authorization", bearer("ash", "TRAINER", Instant.now().plusSeconds(600)))
                        .header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "localhost:3443")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idOrName\":\"celebi\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://localhost:3443/api/v1/local-pokemon/")));
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void expiredOrTamperedTokensAreRejected() throws Exception {
        String expired = bearer("ash", "TRAINER", Instant.now().minusSeconds(600));
        String tampered = bearer("ash", "TRAINER", Instant.now().plusSeconds(600)) + "x";

        for (String token : List.of(expired, tampered)) {
            mvc.perform(post("/api/v1/local-pokemon").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"idOrName\":\"mew\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.responseCode").value("4001"));
        }
    }

    @Test
    void swaggerDocumentationIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/local-pokemon/{id}'].put.description").isNotEmpty());
    }

    /** Signs a token like F05's login will: HS256, issuer, subject and the "roles" claim. */
    private String bearer(String username, String role, Instant expiresAt) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(username)
                .issuedAt(expiresAt.minusSeconds(1200)).expiresAt(expiresAt).claim("roles", List.of(role)).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String extractId(String json) {
        Matcher matcher = ID.matcher(json);
        assertThat(matcher.find()).as("id in %s", json).isTrue();
        return matcher.group(1);
    }
}
