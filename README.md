# Prestigio Weather Station v9 CA
Android 5.1 / API 22.

Naprawa SSL:
- do APK dołączony jest ISRG Root X1,
- SHA-256: 96:BC:EC:06:26:49:76:F3:74:60:77:9A:CF:28:C5:A7:CF:E8:A3:C0:AA:E1:1A:8F:FC:EE:05:C0:BD:DF:08:C6
- aplikacja tworzy własny zaufany KeyStore dla połączenia z Open-Meteo,
- nie wyłącza weryfikacji certyfikatów ani hostname verification,
- Conscrypt pozostaje providerem TLS.
