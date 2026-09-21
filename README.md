# Prestigio Weather Station v7 TLS
Android 5.1 / API 22.

Zmiany:
- natywny interfejs Android (bez WebView)
- lokalne ikony PNG (bez emoji)
- prognoza godzinowa i 5 dni
- brak fikcyjnych danych przy braku połączenia
- Conscrypt 2.5.3 jest dołączany do APK jako nowoczesny provider TLS
- połączenie z Open-Meteo nie korzysta z przestarzałego silnika TLS Androida 5.1
- czytelny komunikat diagnostyczny przy błędzie połączenia
- cache ostatniej poprawnej prognozy
