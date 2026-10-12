package com.ballastlane.pokeproject.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class LogSanitizer {

    private static final int MAX_LENGTH = 200;
    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n\\t]");

    /** Prevents log injection (forged log lines through CR/LF) and huge values from client input. */
    public String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String clean = LINE_BREAKS.matcher(value).replaceAll("_").strip();
        return clean.length() > MAX_LENGTH ? clean.substring(0, MAX_LENGTH) : clean;
    }
}
