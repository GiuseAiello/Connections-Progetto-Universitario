# 🎮 Connections (il gioco delle parole) - Architettura Client-Server in Java



Un'applicazione distribuita sviluppata in Java basata su un'architettura **Client-Server multithreading**. 
Il progetto implementa un gioco multiplayer competitivo ispirato al format "Connections": i giocatori devono individuare gruppi di 4 parole collegate da un tema logico comune prima dello scadere del tempo.

## ✨ Funzionalità Principali

* **Architettura di Rete Ibrida:** Utilizzo di **TCP** per la comunicazione sincrona (login, invio proposte, richieste statistiche) tramite modalità *NIO (Non-Blocking I/O)* lato client, e **UDP (Unicast)** per la ricezione asincrona di notifiche dal server (scadenza timer, esiti partite, classifiche in tempo reale).
* **Gestione Concorrenza:** Server robusto basato su `CachedThreadPool` per la gestione indipendente dei client. Utilizzo estensivo di strutture Thread-Safe (`ConcurrentHashMap`, `CopyOnWriteArrayList`) e primitive di sincronizzazione (`synchronized`, `volatile`) per evitare *race conditions*.
* **Streaming JSON:** Lettura ottimizzata del dizionario di gioco tramite l'approccio streaming (`JsonReader` di Gson), garantendo un utilizzo minimo della memoria RAM anche con file di grandi dimensioni.
* **Persistenza dei Dati:** Salvataggio automatico di utenti registrati, profili storici, statistiche di gioco e archivio delle partite su file `.json` e `.txt`.
* **Graceful Shutdown:** Spegnimento controllato del server (tramite comando `exit`) con chiusura sicura dei socket e salvataggio preventivo dello stato dell'applicazione.

## 🛠️ Struttura del Progetto

```text
📂 Connections/
├── 📁 src/                   # Codice sorgente Java (.java)
├── 📁 lib/                   # Librerie esterne (gson-2.10.1.jar)
├── 📄 server.properties      # File di configurazione del Server
├── 📄 client.properties      # File di configurazione del Client
├── 📄 server-manifest.txt    # Manifest per la creazione dell'eseguibile Server
├── 📄 client-manifest.txt    # Manifest per la creazione dell'eseguibile Client
└── 📄 README.md
```
## 🚀 Requisiti e Configurazione
* Java Development Kit (JDK): Versione 11 o superiore.

* Libreria Gson: Assicurarsi che il file gson-2.10.1.jar sia presente nella cartella lib/.

### Configurazione:

* server.properties: Modificare per impostare porta_tcp, porta_udp, durata_partita e file_dizionario.

* client.properties: Modificare per impostare indirizzo_server e porta_tcp.

## ⚙️ Compilazione e Avvio
Aprire il terminale nella root del progetto ed eseguire i seguenti comandi.

### Compilazione
```bash
# Crea la cartella per i file compilati (se non esiste)
mkdir bin

# Compila i sorgenti (Su Windows usare ';' al posto di ':' come separatore nel classpath)
javac -cp "lib/gson-2.10.1.jar:." src/*.java -d bin/
```
### Creazione degli Eseguibili (.jar)
```bash
jar cvfm Server.jar server-manifest.txt -C bin/ .
jar cvfm Client.jar client-manifest.txt -C bin/ .
```
### Esecuzione
Avviare il server e il client in terminali separati
```bash
# Terminale 1: Avvio Server
java -jar Server.jar

# Terminale 2: Avvio Client
java -jar Client.jar
```
## 🕹️ Guida all'Uso
* Menù Iniziale: L'interfaccia a riga di comando (CLI) guida l'utente tra Registrazione, Login e Aggiornamento Credenziali.

* Gameplay: Durante un turno, il giocatore può inviare una proposta inserendo esattamente 4 parole in maiuscolo separate da spazio (es. CANE TOPO RATTO GATTO).

* Statistiche: È possibile interrogare il server per visualizzare le classifiche globali, le statistiche personali (con streak di vittorie e istogramma degli errori) e lo storico delle partite passate tramite Game ID.

* Termine del gioco: Il server invierà automaticamente una notifica UDP a tutti i client loggati allo scadere del timer, mostrando la classifica aggiornata del round.

* Terminazione del server: è possibile inoltre eseguire un graceful shutdown digitando "exit" nel terminale del server.
