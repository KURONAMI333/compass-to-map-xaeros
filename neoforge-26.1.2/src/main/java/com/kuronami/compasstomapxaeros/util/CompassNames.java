package com.kuronami.compasstomapxaeros.util;

/**
 * 構造物 / バイオーム ID と waypoint 表示名の変換ヘルパー。
 * サーバ側で {@link #prettify} を呼んで人間可読名を作り、クライアント側で
 * {@link #initialsOf} を呼んでミニマップ用の略号を作る。
 */
public final class CompassNames {

    private CompassNames() {}

    /**
     * {@code "minecraft:village"} → {@code "Village"}.
     * {@code "naturescompass:cherry_grove"} → {@code "Cherry Grove"}.
     * Modded namespace は捨てて path のみ Title Case 化する。
     */
    public static String prettify(String resourceId) {
        if (resourceId == null || resourceId.isEmpty()) return "Unknown";
        int colon = resourceId.indexOf(':');
        String path = colon < 0 ? resourceId : resourceId.substring(colon + 1);
        StringBuilder sb = new StringBuilder(path.length());
        boolean cap = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_' || c == '/') {
                sb.append(' ');
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Pretty name から ミニマップ上の 1-2 文字略号を作る。
     * 例: {@code "Village"} → {@code "V"}, {@code "Cherry Grove"} → {@code "CG"}.
     */
    public static String initialsOf(String pretty) {
        if (pretty == null || pretty.isEmpty()) return "?";
        StringBuilder sb = new StringBuilder(2);
        sb.append(Character.toUpperCase(pretty.charAt(0)));
        for (int i = 1; i < pretty.length() && sb.length() < 2; i++) {
            char c = pretty.charAt(i);
            if (Character.isUpperCase(c) || c == ' ') {
                char next = (c == ' ' && i + 1 < pretty.length()) ? pretty.charAt(i + 1) : c;
                if (next != ' ') sb.append(Character.toUpperCase(next));
            }
        }
        return sb.toString();
    }
}
