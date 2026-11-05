/*
 * Порт Fokuso (https://github.com/QazCetelic/Fokuso), © QazCetelic, LGPL-3.0.
 * Адаптация для Minecraft 1.21.1 - zvezdochkin.
 */
package ru.zvezdochkin.filters.client;

import java.util.regex.Pattern;

/**
 * Фильтр по регулярному выражению.
 *
 * <p>Поддерживает сокращения из Fokuso: обратный слэш + {@code p} - префикс
 * ника ({@code <ник>}), обратный слэш + {@code u} - ник игрока. Подстановка
 * выполняется один раз при загрузке списка, {@link Pattern} тоже компилируется
 * один раз.</p>
 */
public final class RegexChatFilter extends ChatFilter {
    /** Префикс ника в формате ванильного чата. */
    public static final String USERNAME_PREFIX_REGEX = "<[^>\\s\\n]{2,}>";
    /** Ник игрока: 3-16 символов, латиница, цифры и подчёркивание. */
    public static final String USERNAME_REGEX = "[a-zA-Z0-9_]{3,16}";

    private final String source;
    private final Pattern pattern;

    /**
     * @param regex регулярное выражение из файла фильтров
     * @throws java.util.regex.PatternSyntaxException если выражение битое
     */
    public RegexChatFilter(String regex) {
        this.source = regex;
        String expanded = regex
                .replace("\\p", USERNAME_PREFIX_REGEX)
                .replace("\\u", USERNAME_REGEX);
        this.pattern = Pattern.compile(expanded);
    }

    @Override
    public boolean matches(String text) {
        // find(), а не matches(): фильтр ищет подстроку, как Pattern.asPredicate()
        // в оригинальном Fokuso.
        return this.pattern.matcher(text).find();
    }

    public String getSource() {
        return this.source;
    }

    @Override
    public String toString() {
        return "/" + this.source + "/";
    }
}
