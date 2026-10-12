package com.ballastlane.pokeproject.api;

import com.ballastlane.pokeproject.bl.AuthBl;
import com.ballastlane.pokeproject.dto.AuthResponse;
import com.ballastlane.pokeproject.dto.BallastResponse;
import com.ballastlane.pokeproject.dto.CredentialsRequest;
import com.ballastlane.pokeproject.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Registration and authentication (F05). */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, JWT login and the current user")
public class AuthController {

    private final AuthBl authBl;

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Create a TRAINER account",
            description = """
                    Registers a new user. Self-registration always creates the TRAINER role.

                    **User story:** F05 - User registration and authentication.

                    **Validations:**
                    - `username` and `password` are required (400, code 1000).
                    - Username 3-30 letters, digits or underscore, case-insensitive; password 8-72 characters with at \
                    least one letter and one digit (400, code 1007, with one message per field).
                    - Username already taken (409, code 3004).

                    **Role:** public.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created; response = username and role"),
            @ApiResponse(responseCode = "400", description = "Invalid data (1000 / 1001 / 1007)"),
            @ApiResponse(responseCode = "409", description = "Username taken (3004)")})
    public ResponseEntity<BallastResponse<UserResponse>> register(@Valid @RequestBody CredentialsRequest request) {
        // mixed data: the username is logged, the password never
        log.info("[REQUEST] Register user - username={}", request.username());

        UserResponse body = authBl.register(request);

        log.info("[REQUEST] Register user responded - username={}, role={}", body.username(), body.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(BallastResponse.success(body));
    }

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Exchange credentials for a bearer token",
            description = """
                    Returns a signed JWT (HS256) with the user's role, valid for 2 hours.

                    **User story:** F05 - User registration and authentication.

                    **Validations:**
                    - `username` and `password` are required (400, code 1000).
                    - Unknown user or wrong password (401, code 4002): same message and similar response time in \
                    both cases, so the response does not reveal which usernames exist.

                    **Role:** public.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "response = accessToken, tokenType, expiresAt, user"),
            @ApiResponse(responseCode = "400", description = "Missing fields (1000 / 1001)"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password (4002)")})
    public ResponseEntity<BallastResponse<AuthResponse>> login(@Valid @RequestBody CredentialsRequest request) {
        log.info("[REQUEST] Login attempt - username={}", request.username());

        AuthResponse body = authBl.login(request);

        // the token is sensitive: only who logged in and until when
        log.info("[REQUEST] Login responded - username={}, role={}, expiresAt={}", body.user().username(),
                body.user().role(), body.expiresAt());
        return ResponseEntity.ok(BallastResponse.success(body));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Current user, read from the token",
            security = @SecurityRequirement(name = "bearer"),
            description = """
                    Returns the username and role carried by the bearer token, without a database lookup.

                    **User story:** F05 - User registration and authentication.

                    **Validations:**
                    - Missing, expired or invalid token (401, code 4001).

                    **Role:** any authenticated user.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "response = username and role"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token (4001)")})
    public ResponseEntity<BallastResponse<UserResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        log.info("[REQUEST] Get current user - username={}", jwt.getSubject());

        List<String> roles = jwt.getClaimAsStringList("roles");
        UserResponse body = authBl.currentUser(jwt.getSubject(), roles == null || roles.isEmpty() ? null : roles.getFirst());

        log.info("[REQUEST] Get current user responded - username={}, role={}", body.username(), body.role());
        return ResponseEntity.ok(BallastResponse.success(body));
    }
}
