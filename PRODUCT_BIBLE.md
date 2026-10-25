# FocuSense — Product Bible 1.0

## Promessa

FocuSense aiuta a lasciare in pace il telefono durante studio e lavoro e impara quali condizioni ambientali ricorrono quando l'utente dichiara di essersi concentrato meglio.

## Core loop

1. L'utente sceglie durata, contesto e disciplina Relaxed / Standard / Strict.
2. La sessione parte in Focus Display AMOLED: nero reale, timer e barra pastello.
3. Rumore e luce producono l'Environment Score.
4. Pickup, movimento e uscite dall'app a schermo acceso riducono la Focus Integrity.
5. Spegnere manualmente lo schermo non è una penalità e contribuisce al Deep Focus.
6. Al termine nasce un Focus Core persistente.
7. L'utente assegna un rating di focus 1–5.
8. I rating personali alimentano Best Conditions.
9. I Core costruiscono il Sanctuary e alimentano XP, livelli, streak, obiettivi e badge.

## Metriche separate

- Environment Score: rumore + luce.
- Focus Integrity: disciplina nell'uso del telefono.
- Focus percepito: rating utente 1–5, ground truth personale.
- Session Score: riepilogo composito 60% Integrity + 40% ambiente.

Non usare il Session Score come ground truth per Best Conditions.

## Focus Integrity

Modalità:
- Relaxed: 15% instabilità tollerata.
- Standard: 8%.
- Strict: 3%.

Segnali:
- tempo instabile;
- pickup con hysteresis/cooldown;
- uscite dall'app mentre lo schermo è attivo;
- tempo fuori app.

Lo schermo spento non riduce Integrity.

## Focus Core

Stadi:
NONE -> SPARK -> ORB -> CRYSTAL -> NOVA -> PRISM.

Sessioni abbandonate possono lasciare un FRACTURED Core invece di cancellare ogni progresso.

Il Sanctuary è la collezione persistente dei Core.

## UX sessione

Default:
- sfondo #000000;
- luminosità molto bassa;
- timer grande;
- barra di tempo residuo pastello;
- Integrity secondaria;
- nessun annuncio;
- controlli nascosti fino al tap;
- leggero drift della UI per ridurre staticità AMOLED.

L'utente può spegnere manualmente lo schermo.

## Privacy

- audio non registrato né salvato;
- solo valori numerici dBFS;
- dati local-first;
- nessuna inferenza medica;
- movimento = movimento telefono, non postura corporea.

## Monetizzazione

Free:
- sessioni focus;
- Focus Core / Sanctuary;
- analytics essenziali;
- banner AdMob fuori dalle sessioni.

Pro Lifetime:
- nessuna richiesta pubblicitaria;
- base pronta per future analytics/temi premium.

Vincoli:
- UMP prima delle richieste ad;
- debug/QA solo test traffic;
- mai ads nella sessione focus;
- release senza AdMob production config = ads disabilitati fail-closed.

Product ID Play:
`focussense_pro_lifetime`

## Non obiettivi 1.0

- AI generativa;
- social feed;
- multiplayer;
- AccessibilityService/app blocker;
- backend obbligatorio;
- audio recording;
- diagnosi di produttività o attenzione.
