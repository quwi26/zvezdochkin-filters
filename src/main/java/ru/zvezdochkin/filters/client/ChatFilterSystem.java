/*
 * Порт Fokuso (https://github.com/QazCetelic/Fokuso), © QazCetelic, LGPL-3.0.
 * Адаптация для Minecraft 1.21.1 - zvezdochkin.
 */
package ru.zvezdochkin.filters.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Загрузка списков фильтров и проверка сообщений.
 *
 * <p>Списки берутся из двух мест:</p>
 * <ul>
 *     <li>встроенные - {@code /assets/zvezdochkin-filters/filters/*.filters}
 *     внутри jar, список файлов перечислен в {@code index.txt};</li>
 *     <li>пользовательские - {@code config/zvezdochkin-filters/*.filters}.</li>
 * </ul>
 */
public final class ChatFilterSystem {
    public static final String MOD_ID = "zvezdochkin-filters";
    public static final String FILE_EXTENSION = ".filters";

    private static final String BUILTIN_DIR = "/assets/" + MOD_ID + "/filters/";
    private static final String BUILTIN_INDEX = BUILTIN_DIR + "index.txt";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Список неизменяемый, поэтому volatile достаточно: читатели видят либо
     * старый, либо новый набор целиком.
     */
    private static volatile List<ChatFilterGroup> groups;

    private ChatFilterSystem() {
    }

    /**
     * @return загруженные списки; при первом обращении читает их с диска и из jar
     */
    public static List<ChatFilterGroup> getGroups() {
        List<ChatFilterGroup> current = groups;
        if (current == null) {
            synchronized (ChatFilterSystem.class) {
                current = groups;
                if (current == null) {
                    current = loadAll();
                    groups = current;
                }
            }
        }
        return current;
    }

    /**
     * @param message сообщение, которое Minecraft собирается добавить в чат
     * @return {@code true}, если сообщение нужно скрыть
     */
    public static boolean filter(Component message) {
        // Свои сообщения не скрываем: на серверах с ванильным форматом чата
        // они выглядят как "<ник> текст". Игрока может ещё не быть в мире.
        if (isOwnMessage(message)) {
            return false;
        }

        String text = message.getString();
        for (ChatFilterGroup group : getGroups()) {
            if (group.matches(text)) {
                LOGGER.debug("Скрыто сообщение: {}", text);
                return true;
            }
        }

        return false;
    }

    /** Перечитывает встроенные и пользовательские списки. */
    public static void reload() {
        groups = loadAll();
    }

    public static Optional<ChatFilterGroup> findGroup(String name) {
        return getGroups().stream()
                .filter(group -> group.getName().equals(name))
                .findFirst();
    }

    private static List<ChatFilterGroup> loadAll() {
        List<ChatFilterGroup> result = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        loadBuiltinLists(result, usedNames);
        loadUserLists(result, usedNames);

        int rules = result.stream().mapToInt(ChatFilterGroup::size).sum();
        LOGGER.info("Загружено {} списков фильтров, {} правил.", result.size(), rules);
        return List.copyOf(result);
    }

    private static void loadBuiltinLists(List<ChatFilterGroup> result, Set<String> usedNames) {
        for (String fileName : readBuiltinIndex()) {
            if (!fileName.endsWith(FILE_EXTENSION)) {
                LOGGER.warn("В index.txt указан не .filters файл: {}", fileName);
                continue;
            }

            String resource = BUILTIN_DIR + fileName;
            try (InputStream stream = ChatFilterSystem.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    LOGGER.warn("Встроенный список не найден в jar: {}", resource);
                    continue;
                }

                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    String baseName = "builtin-" + stripExtension(fileName);
                    List<RegexChatFilter> filters = readFilters(reader, resource);
                    if (filters.isEmpty()) {
                        LOGGER.debug("Встроенный список {} пуст, пропущен.", resource);
                        continue;
                    }
                    result.add(new ChatFilterGroup(uniqueName(sanitize(baseName), usedNames), filters));
                }
            } catch (IOException e) {
                LOGGER.warn("Не удалось прочитать встроенный список {}", resource, e);
            }
        }
    }

    private static List<String> readBuiltinIndex() {
        try (InputStream stream = ChatFilterSystem.class.getResourceAsStream(BUILTIN_INDEX)) {
            if (stream == null) {
                LOGGER.warn("Не найден {} - встроенные фильтры не загружены.", BUILTIN_INDEX);
                return List.of();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();
            }
        } catch (IOException e) {
            LOGGER.warn("Не удалось прочитать {}", BUILTIN_INDEX, e);
            return List.of();
        }
    }

    private static void loadUserLists(List<ChatFilterGroup> result, Set<String> usedNames) {
        File configDir = new File(new File(Minecraft.getInstance().gameDirectory, "config"), MOD_ID);

        if (!configDir.exists() && !configDir.mkdirs()) {
            LOGGER.warn("Не удалось создать каталог {}", configDir);
            return;
        }
        if (!configDir.isDirectory()) {
            LOGGER.warn("{} существует, но это не каталог.", configDir);
            return;
        }

        createExampleFile(configDir);

        try (Stream<Path> paths = Files.walk(configDir.toPath())) {
            List<Path> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(FILE_EXTENSION))
                    .sorted()
                    .toList();

            for (Path file : files) {
                String baseName = stripExtension(file.getFileName().toString());
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    List<RegexChatFilter> filters = readFilters(reader, file.toString());
                    if (filters.isEmpty()) {
                        LOGGER.debug("Список {} пуст, пропущен.", file);
                        continue;
                    }
                    result.add(new ChatFilterGroup(uniqueName(sanitize(baseName), usedNames), filters));
                } catch (IOException e) {
                    LOGGER.warn("Не удалось прочитать список {}", file, e);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Не удалось прочитать каталог {}", configDir, e);
        }
    }

    private static List<RegexChatFilter> readFilters(Reader reader, String sourceName) throws IOException {
        List<RegexChatFilter> filters = new ArrayList<>();
        BufferedReader buffered = reader instanceof BufferedReader br ? br : new BufferedReader(reader);

        String line;
        int lineNumber = 0;
        while ((line = buffered.readLine()) != null) {
            lineNumber++;
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }

            try {
                filters.add(new RegexChatFilter(line));
            } catch (PatternSyntaxException e) {
                LOGGER.warn("Битый regex в {}:{}, строка пропущена: {}",
                        sourceName, lineNumber, e.getDescription());
            }
        }

        return filters;
    }

    private static boolean isOwnMessage(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        String playerName = minecraft.player.getName().getString();
        return message.getString().startsWith("<" + playerName + "> ");
    }

    private static void createExampleFile(File configDir) {
        File example = new File(configDir, "example.filters");
        if (example.exists()) {
            return;
        }

        String content = """
                # Пример пользовательского списка Zvezdochkin Filters.
                # Одна строка - один regex. Пустые строки и строки с # игнорируются.
                # Сокращения: \\u - ник игрока, \\p - префикс ника (<ник>).
                #
                # Пример: скрыть сообщения о достижениях.
                # \\u has made the advancement \\[.*\\]
                """;

        try {
            Files.writeString(example.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Не удалось создать пример фильтров {}", example, e);
        }
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
    }

    /** Приводит имя файла к виду, который удобно вводить в команде. */
    private static String sanitize(String name) {
        String sanitized = name.trim().replaceAll("[^A-Za-z0-9_.-]+", "-");
        return sanitized.isEmpty() ? "list" : sanitized;
    }

    private static String uniqueName(String baseName, Set<String> usedNames) {
        if (usedNames.add(baseName)) {
            return baseName;
        }

        int suffix = 2;
        while (!usedNames.add(baseName + "-" + suffix)) {
            suffix++;
        }
        return baseName + "-" + suffix;
    }
}
