/*
 * Порт Fokuso (https://github.com/QazCetelic/Fokuso), © QazCetelic, LGPL-3.0.
 * Адаптация для Minecraft 1.21.1 - zvezdochkin.
 */
package ru.zvezdochkin.filters.client;

/**
 * Базовый фильтр чата.
 *
 * <p>Фильтр получает уже развёрнутый текст сообщения (без §-кодов), поэтому
 * ему не нужно знать про {@code Component}.</p>
 */
public abstract class ChatFilter {
    private boolean enabled = true;

    /**
     * @param text текст сообщения в том виде, в каком его видит игрок
     * @return {@code true}, если сообщение нужно скрыть
     */
    public abstract boolean matches(String text);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
