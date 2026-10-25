# FocuSense

FocuSense è una focus utility Android local-first: aiuta a lasciare in pace il telefono durante studio e lavoro e analizza quali condizioni di rumore e luce ricorrono quando l'utente dichiara di essersi concentrato meglio.

## FocuSense 1.0 RC1

Il prodotto ruota attorno a quattro concetti separati:

- **Environment Score** — rumore + luce;
- **Focus Integrity** — movimento del telefono, pickup e uscite dall'app a schermo acceso;
- **Focus percepito** — rating 1–5 dell'utente, usato come ground truth personale;
- **Session Score** — riepilogo 60% Integrity + 40% ambiente.

### Focus Display AMOLED

Durante una sessione, per default FocuSense usa un display minimale:
- nero reale;
- timer residuo;
- barra pastello che si estingue;
- luminosità molto bassa;
- controlli nascosti fino al tap;
- nessun annuncio.

Spegnere manualmente lo schermo **non** penalizza la sessione. Il tempo a schermo spento contribuisce al Deep Focus.

### Focus Integrity

Modalità:
- Relaxed — 15% instabilità tollerata;
- Standard — 8%;
- Strict — 3%.

Il motore applica hysteresis/cooldown ai pickup per evitare di contare lo stesso movimento più volte.

### Focus Core / Sanctuary

Le sessioni completate producono Focus Core procedurali:

`Spark -> Orb -> Crystal -> Nova -> Prism`

Una sessione abbandonata può lasciare un `Fractured Core` invece di azzerare tutto. I Core vengono conservati nel Sanctuary insieme a XP, livelli, streak, obiettivi e badge.

## Best Conditions

Best Conditions non seleziona più le sessioni sulla base dello score automatico. Usa le sessioni valutate dall'utente e privilegia rating 4–5/5, evitando la precedente circolarità.

Gli insight sono descrittivi e non causali.

## Sensori e privacy

- microfono: solo livello numerico dBFS, nessun audio conservato;
- luce: lux quando disponibile;
- accelerometro + giroscopio: stabilità/movimento del telefono;
- nessuna misurazione reale della postura corporea;
- Foreground Service solo durante la sessione;
- Room + DataStore local-first.

## Monetizzazione

### Free
- focus sessions;
- Focus Core e Sanctuary;
- analytics;
- banner AdMob soltanto fuori dalla sessione.

### Pro Lifetime
Product ID previsto:

`focussense_pro_lifetime`

Pro non effettua richieste pubblicitarie.

La build supporta:
- Google Mobile Ads;
- UMP consent;
- Play Billing one-time;
- restore purchase.

### Configurazione production ancora richiesta

GitHub secrets:

- `FOCUSENSE_ADMOB_APP_ID`
- `FOCUSENSE_ADMOB_BANNER_ID`

Senza entrambi, la release resta **fail-closed** e non richiede ads.

Servono inoltre:
- app configurata in AdMob;
- messaggio UMP/privacy pubblicato;
- privacy policy reale;
- prodotto Play `focussense_pro_lifetime`;
- test sandbox acquisto/ripristino.

## Build / release

Package ID stabile:

`com.utilitylab.focussense`

Le build `main` usano:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

La release usa R8 + resource shrinking e mantiene il size gate.

## Provenienza

La prima implementazione dei sensori è stata ispirata/adattata da MindBricks (MIT). La licenza del donor è conservata in:

`app/src/main/assets/licenses/MindBricks_LICENSE.txt`

## Evidence boundary

CI green non certifica:
- pickup detection fisica;
- comportamento OEM/background;
- consumo batteria;
- Focus Display su AMOLED reale;
- UMP production;
- AdMob production;
- Play Billing sandbox.

Queste capability richiedono validazione sullo stesso artifact firmato.
