# 🥁 MuzMachina – Sieciowa maszyna perkusyjna z czatem

**MuzMachina** to interaktywny program do tworzenia rytmicznych kompozycji perkusyjnych w czasie rzeczywistym. Umożliwia nie tylko budowanie rytmów, ale również **wysyłanie ich do innych użytkowników** przez sieć oraz prowadzenie rozmów na czacie. Projekt oparty na przykładzie z książki _"Java. Rusz głową!"_, rozszerzony o funkcje komunikacji sieciowej.

## 🎹 Funkcje

- ✅ Interfejs graficzny oparty na Swing (siatka rytmiczna 16x16)
- ✅ 16 instrumentów perkusyjnych (Bass Drum, Hi-Hat, Snare, Tomy itp.)
- ✅ Odtwarzanie rytmu (Start/Stop/Szybciej/Wolniej)
- ✅ Wysyłanie kompozycji przez sieć
- ✅ Wbudowany **czat sieciowy**
- ✅ Możliwość dołączenia do wspólnego serwera podając nazwę użytkownika

## 🖼️ Zrzut ekranu

![MuzMachina screenshot](./screenshot.png)

## 🚀 Jak uruchomić?

### 1. Serwer

Uruchom plik `SerwerMuzMachina.java` na hoście:
```bash
javac SerwerMuzMachina.java
java SerwerMuzMachina
