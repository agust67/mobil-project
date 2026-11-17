# mobil-project

Mòbil controlat per al nen: Pixel amb GrapheneOS i un llançador propi que fa de
Device Owner. Tota la gestió viu al homelab.

- `llancador/`: l'app del llançador (Android Studio, Kotlin).

## Provar el llançador a l'emulador

1. Obre el projecte `llancador` a Android Studio i engega l'emulador.
2. Prem ▶ per instal·lar l'app. Ha de dir «Aquest mòbil encara no està gestionat».
3. Converteix-la en Device Owner. Al terminal d'Android Studio (PowerShell):

   ```
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell dpm set-device-owner cat.casa.llancador/.AdministradorDispositiu
   ```

   Ha de respondre `Success: Device owner set to package ...`. Només funciona si
   l'emulador no té cap compte afegit.
4. Torna a obrir l'app: ha de dir «Aquest mòbil està gestionat».
5. Per desfer-ho (només en proves), prem el botó «Proves: deixar de gestionar el mòbil».
