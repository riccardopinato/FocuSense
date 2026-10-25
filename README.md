# FocuSense

FocuSense è una utility Android local-first per osservare come rumore, luce e movimento del telefono si associano alle sessioni di studio/lavoro.

## Stato

Baseline ricostruita e consolidata dagli step già validati fino alla 0.6, con hardening Lean Release 0.8.

Funzioni principali:
- sessioni con rumore dBFS, luce, accelerometro e giroscopio;
- Focus Score deterministico con sensori disponibili e calibrazione personale;
- storico e dettaglio sessione;
- insight temporali;
- contesti e tag;
- gamification leggera: XP, livelli, streak, obiettivi e badge;
- Best Conditions Dashboard;
- profilo Google opzionale;
- Room local-first e DataStore;
- Foreground Service soltanto durante la sessione.

Limiti espliciti:
- non registra né conserva audio;
- non è un fonometro certificato;
- il movimento riguarda il telefono, non misura la postura del corpo;
- gli insight descrivono associazioni storiche, non causalità o diagnosi.

## Stack

Kotlin, Jetpack Compose, Material 3, Room, DataStore, Android Credential Manager.

## Release size

La release usa R8 + resource shrinking. MPAndroidChart, JitPack e `material-icons-extended` sono stati rimossi; il trend è disegnato con Compose Canvas e le icone di navigazione sono VectorDrawable locali.

Budget prodotto:
- target APK release universale: <= 25 MiB;
- hard review gate: > 30 MiB.

## Build firmate

Le build su `main` usano i repository secrets:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

La keystore non deve mai essere committata.

Package ID stabile: `com.utilitylab.focussense`.

Per aggiornare un APK già installato senza disinstallazione servono:
1. stesso package ID;
2. stessa chiave di firma;
3. `versionCode` crescente.

## Provenienza

La prima implementazione dei sensori è stata ispirata/adattata da MindBricks (MIT). La licenza del donor è conservata in `app/src/main/assets/licenses/MindBricks_LICENSE.txt`.
