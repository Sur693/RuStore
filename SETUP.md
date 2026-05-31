# Инструкция по сборке и запуску

## Требования

| Инструмент | Версия | Где скачать |
|---|---|---|
| Android Studio | Hedgehog или новее | [developer.android.com](https://developer.android.com/studio) |
| JDK | 17 или новее | поставляется вместе с Android Studio |
| Android SDK | API 26–35 | устанавливается через Android Studio |
| Git | любая | [git-scm.com](https://git-scm.com) |

> Сервер развёрнут на Railway — запускать его локально не нужно.

---

## Шаг 1 — Клонировать репозиторий

```bash
git clone https://github.com/Sur693/RuStore.git
```

---

## Шаг 2 — Открыть проект в Android Studio

1. Запусти Android Studio
2. Нажми **Open** 
3. Выбери папку `RuStore/` (корневую)
4. Подожди пока Gradle синхронизируется

---

## Шаг 3 — Запустить эмулятор

1. **Tools → Device Manager → Create Device**
2. Выбери любой телефон (например Pixel 6)
3. Выбери образ системы — **API 26 или выше** (рекомендуется API 34)
4. Нажми **Finish**

---

## Шаг 4 — Запустить приложение

1. В верхней панели выбери эмулятор из списка устройств
2. Нажми **Run → Run 'app'** или `Shift + F10`

Приложение откроется и сразу загрузит данные с сервера.

---

## Запуск на реальном телефоне

Приложение подключается к серверу на Railway — работает с любого устройства с интернетом без дополнительных настроек.

1. Подключи телефон по USB и включи **USB-отладку** (Настройки → Для разработчиков)
2. Выбери устройство в Android Studio и нажми **Run**

---

## Структура проекта

```
RuStore/
├── app/                    ← Android-приложение (открывать в Android Studio)
│   └── src/main/
│       ├── java/com/rustore/app/
│       │   ├── data/       ← модели, API, репозитории
│       │   ├── service/    ← Foreground Service, PackageInstaller
│       │   └── ui/         ← экраны и компоненты Compose
│       └── res/            ← ресурсы Android
├── server/                 ← Ktor бэкенд (задеплоен на Railway)
│   ├── Dockerfile
│   ├── railway.toml
│   └── src/main/
│       ├── kotlin/         ← код сервера
│       └── resources/      ← APK, иконки, скриншоты
├── README.md
└── SETUP.md                ← этот файл
```

---
