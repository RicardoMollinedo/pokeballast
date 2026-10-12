package com.ballastlane.pokeproject.api.filter;

/**
 * Audit data of the current request, taken from optional headers. Every field is {@code null} when
 * the header is absent or invalid. Informational only: the authenticated identity comes from the JWT.
 */
public record AuditContext(String clientIp, String user, Double latitude, Double longitude, String channel) {

    public static final String ATTRIBUTE = AuditContext.class.getName();
}
