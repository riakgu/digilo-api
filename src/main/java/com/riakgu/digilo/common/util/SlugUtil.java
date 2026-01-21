package com.riakgu.digilo.common.util;

public class SlugUtil {
    public static String normalize(String input) {
        return input.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}