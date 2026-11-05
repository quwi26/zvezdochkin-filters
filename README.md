# Zvezdochkin Filters

Клиентский фильтр чата для Minecraft 1.21.1 (Fabric). Скрывает сообщения,
совпавшие с regex-правилами: спам от модов, служебные сообщения серверов,
анонсы достижений и т. п.

Это порт [Fokuso](https://github.com/QazCetelic/Fokuso) 1.2.0 с Minecraft
1.20.1. Логика фильтрации и формат списков сохранены, код переведён на
Mojang-маппинги и API 1.21.1.

## Требования

- Minecraft 1.21.1
- Fabric Loader 0.16.0+
- Fabric API 0.116.0+1.21.1
- Java 21

## Команды

| Команда | Что делает |
| --- | --- |
| `/chatfilters list` | Показывает загруженные списки и число включённых правил |
| `/chatfilters reload` | Перечитывает списки из `config/zvezdochkin-filters` |
| `/chatfilters toggle <list> <true\|false>` | Включает или выключает список целиком |

## Списки фильтров

Файл `.filters` - UTF-8, одна строка = один regex. Пустые строки и строки,
начинающиеся с `#`, игнорируются.

Сокращения:

| Сокращение | Значение |
| --- | --- |
| `\p` | префикс ника в ванильном чате, `<ник>` |
| `\u` | ник игрока, `[a-zA-Z0-9_]{3,16}` |

Пример:

```text
# Скрыть анонсы достижений
\u has made the advancement \[.*\]
```

### Встроенные списки (в jar)

Лежат в `src/main/resources/assets/zvezdochkin-filters/filters/`. Чтобы
добавить новый список, положите туда `<имя>.filters` и допишите имя файла в
`index.txt`. В игре группа будет называться `builtin-<имя>`.

Сейчас встроены:

- `vanilla-spam.filters` - анонсы достижений, испытаний и целей на английском
  и русском (группа `builtin-vanilla-spam`);
- `mod-spam.filters` - уведомления Yuushya и TabChannel (группа
  `builtin-mod-spam`).

### Пользовательские списки

Каталог `config/zvezdochkin-filters/` создаётся при первом запуске. Туда можно
класть свои `.filters`; они читаются поверх встроенных. При первом запуске
создаётся `example.filters` с закомментированными примерами.

## Сборка

```bash
./gradlew build
```

Готовый jar - `build/libs/zvezdochkin-filters-1.0.0.jar`.

## Лицензия

Проект распространяется под **GNU Lesser General Public License v3.0**
(см. `LICENSE`).

Оригинальный Fokuso © QazCetelic, также LGPL-3.0. Порт и адаптация для
Minecraft 1.21.1 - zvezdochkin. Исходный код порта доступен в этом
репозитории; при распространении jar исходники должны оставаться доступными
в соответствии с LGPL-3.0.
