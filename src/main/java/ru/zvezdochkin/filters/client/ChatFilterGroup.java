/*
 * Порт Fokuso (https://github.com/QazCetelic/Fokuso), © QazCetelic, LGPL-3.0.
 * Адаптация для Minecraft 1.21.1 - zvezdochkin.
 */
package ru.zvezdochkin.filters.client;

import java.util.List;

/**
 * Именованный список фильтров, который включается и выключается целиком.
 */
public final class ChatFilterGroup {
    private final String name;
    private final List<RegexChatFilter> filters;
    private boolean enabled = true;

    public ChatFilterGroup(String name, List<RegexChatFilter> filters) {
        this.name = name;
        this.filters = List.copyOf(filters);
    }

    public String getName() {
        return this.name;
    }

    public int size() {
        return this.filters.size();
    }

    public int enabledCount() {
        return (int) this.filters.stream().filter(ChatFilter::isEnabled).count();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @param text текст сообщения
     * @return {@code true}, если хотя бы один включённый фильтр списка совпал
     */
    public boolean matches(String text) {
        if (!this.enabled) {
            return false;
        }

        for (RegexChatFilter filter : this.filters) {
            if (filter.isEnabled() && filter.matches(text)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return this.name + " (" + this.enabledCount() + "/" + this.filters.size() + ")";
    }
}
