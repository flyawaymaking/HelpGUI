# HelpGUI

**HelpGUI** — это плагин для Minecraft, который заменяет стандартную команду `/help` на стильное и настраиваемое GUI-меню.  
Игроки получают удобный визуальный интерфейс со ссылками, командами, описаниями и интерактивными кнопками.

---

## ✨ Основные возможности

- 🔹 Полностью настраиваемое меню помощи через `config.yml`
- 🔹 Поддержка градиентных и HEX-цветов в названии и описаниях предметов
- 🔹 Возможность привязывать разные команды к ЛКМ и ПКМ
- 🔹 Удобное управление через команду `/helpgui reload`
- 🔹 Поддержка **Paper** (Minecraft 1.21.8)
- 🔹 Поддержка **Custom Heads (PLAYER_HEAD)** для уникального дизайна

![helpGUI.png](assets/screenshots/helpGUI.png)

---

## ⚙️ Команды

| Команда | Описание | Права |
|----------|-----------|--------|
| `/help` | Открывает GUI-меню помощи | — |
| `/helpgui reload` | Перезагружает конфигурацию плагина | `customhelpgui.admin` |

---

## 🔑 Права доступа

| Permission | Описание | По умолчанию |
|-------------|-----------|--------------|
| `customhelpgui.admin` | Позволяет управлять плагином и перезагружать конфигурацию | `op` |

---

## 🧩 Пример конфигурации (`config.yml`)

```yml
# Custom Help GUI Configuration
# После изменений используйте: /helpgui reload

gui:
title: "<gradient:#00FFAA:#00FFFF>Помощь и информация</gradient>"
rows: 6

items:
  website:
    slot: 10
    material: "DIAMOND"
    name: "<gradient:#00FFFF:#FF00FF>Наш сайт</gradient>"
    lore:
      - "&#AAAAAAПосетите наш &#00FFFFофициальный сайт"
      - "<gradient:#FFAA00:#FFFF00>для получения информации</gradient>"
      - ""
      - "&eЛКМ: &fОтправить ссылку в чат"
      - "&eПКМ: &fОткрыть сайт в браузере"
    left-click-command: "msg %player% Сайт: example.com"
    right-click-command: "msg %player% Чтобы открыть сайт, скопируйте: example.com"

  discord:
    slot: 16
    material: "PLAYER_HEAD"
    name: "<gradient:#AA00FF:#00AAFF>Наш Discord</gradient>"
    lore:
      - "&#FFFFFFПрисоединяйтесь к нашему"
      - "<gradient:#FF5555:#FFAA00>Discord сообществу</gradient>"
      - ""
      - "&eЛКМ: &fПолучить ссылку приглашения"
      - "&eПКМ: &fСкопировать ссылку"
    left-click-command: "msg %player% Discord: discord.gg/example"
    right-click-command: "msg %player% Чтобы скопировать: discord.gg/example"
texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2U1YjVmNmFkMjYyYjJmNGFmYjU4YTJkMjgxY2M0Y2U1YzY3MjlhY2Q0Y2Y2Y2U2ZTVlY2Q3M2Q4Y2YzYyJ9fX0="
```

---

## 📦 Установка

1. Скачайте **последний релиз** из раздела [Releases](../../releases)
2. Поместите `.jar` в папку `/plugins`
3. Перезапустите сервер
4. Настройте `config.yml` под ваш сервер
5. Используйте `/helpgui reload`, чтобы применить изменения без перезапуска

---

## 📄 Лицензия

Плагин распространяется под лицензией MIT.
