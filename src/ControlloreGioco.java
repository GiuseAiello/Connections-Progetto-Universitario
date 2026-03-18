
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;



import java.io.*;
import java.net.*;


public class ControlloreGioco {


    //classe che mi servirà per avviare la connessione asincrona con il client
    //in InviaNotificaUnicast
    //avere questa classe è molto comodo in quanto quando devo instaurare una connessione UDP con 
    //il client ho tutto quello che mi serve qui dentro
    public class InfoClient {
        public InetAddress ip; 
        public int portaUdp;

        public InfoClient(InetAddress ip, int portaUdp) {
            this.ip = ip;
            this.portaUdp = portaUdp;
        }
    }

    public static class RecordPartita {
        public Partita partita;
        public Map<String, StatoPartitaGiocatore> risultati;

        public RecordPartita(Partita p, Map<String, StatoPartitaGiocatore> r) {
            this.partita = p;
            // Creiamo una copia della mappa, altrimenti quando la svuotiamo si cancella anche qui
            this.risultati = new ConcurrentHashMap<>(r); 
        }
    }





    // Database degli utenti registrati <username, password>
    static Map<String, String> InfoGiocatori = new ConcurrentHashMap<>();

    // Database dei profili statistici globali <username, Profilo>
    static Map<String, ProfiloGiocatore> ProfiliGlobali = new ConcurrentHashMap<>();
    
    //Database degli utenti online
    static Map<String, InfoClient> UtentiOnline = new ConcurrentHashMap<>();

    // --- VARIABILI PER LO STREAMING JSON ---
    //JsonReader è ciò che ci consente di trattare il file json delle partite come se fosse un file molto più grande
    //aka di prenderne il contenuto poco alla volta 
    private JsonReader jsonReader; // Il mezzo verso il file
    private Gson gson;             // Il traduttore
    private Partita partitaCorrente; // solo una partita in memoria alla volta!

    // Una mappa che ha come chiave lo username di un giocatore e come valore 
    // una classe contenente info rilevanti di quel player (punti, errori, temiIndovinati)
    static Map<String, StatoPartitaGiocatore> PartiteInCorso = new ConcurrentHashMap<>();

    // L'archivio delle partite passate <gameId, Record>
    static Map<Integer, RecordPartita> ArchivioPartite = new ConcurrentHashMap<>();

    // variabili che hanno a che fare con la durata di una partita
    private int indicePartitaCorrente = 0; // Contatore per comodità
    private long tempoFinePartitaAttuale;  // Per ricordarci quando scadrà il tempo
    private int durataPartitaMinuti;       // Quanto dura una partita? (letta da config)
    private int porta_udp; //porta per le comunicazioni asincrone al client
    
    // Il Timer
    private ScheduledExecutorService timerPartita = Executors.newSingleThreadScheduledExecutor();

    public ControlloreGioco(String FileDizionario, int durataPartitaMinuti, int porta_udp) {

        this.durataPartitaMinuti = durataPartitaMinuti; 
        this.porta_udp = porta_udp; // Salvo la porta UDP passata dal Main
        this.gson = new Gson(); // Inizializziamo Gson una volta sola
        
        try {
            // 1. Apriamo il flusso dati VERSO il file (senza caricarlo tutto)
            this.jsonReader = new JsonReader(new FileReader(FileDizionario));
            
            // 2. Leggiamo la primissima parentesi quadra '[' per "entrare" nell'array
            this.jsonReader.beginArray();
            
            // --- MANDIAMO AVANTI IL NASTRO DEL JSON ---
            //questo ci consente ogni volta che riavviamo il server di ricordarci a che partita 
            //eravamo e partire da lì
            int partiteDaSaltare = CaricaIndicePartita();
            //questo for serve per far sì che il lettore jsonReader si trovi alla prima partita ancora non letta
            //ed abbia scartato tutte le partite già svolte
            for (int i = 0; i < partiteDaSaltare; i++) {
                if (jsonReader.hasNext()) {
                    // Legge l'oggetto JSON e lo butta via senza salvarlo in memoria
                    gson.fromJson(jsonReader, Partita.class); 
                    this.indicePartitaCorrente++;
                }
            }
           
            
            
        } catch (IOException e) {
            System.err.println("Errore CRITICO: Impossibile trovare o aprire il file " + FileDizionario);
            e.printStackTrace();

        }
        CaricaDatiSalvati(); 
        

        // Avviamo la prima partita, che pescherà il primo elemento dal flusso
        AvviaNuovaPartita();
    }

    public void AvviaNuovaPartita() {
        // 1. Pulizia: Svuotiamo i "taccuini" della partita precedente
        PartiteInCorso.clear();
        
        // 2. Peschiamo la partita dal flusso (L'Approccio Ibrido GSON)
        try {
            if (jsonReader != null && jsonReader.hasNext()) {
                // Chiediamo a Gson di leggere SOLAMENTE il prossimo oggetto {...} e fermarsi
                //questo è il modo in cui prelevo le partite dal file json
                //in modo da poterlo trattare come fosse un file molto più grande 
                this.partitaCorrente = gson.fromJson(jsonReader, Partita.class);
                indicePartitaCorrente++;
            } else {
                System.out.println("ATTENZIONE: Il file JSON è terminato! Non ci sono più partite.");
        
                return; 
            }
        } catch (IOException e) {
            System.err.println("Errore durante la lettura in streaming della partita.");
        }

        // 3. Calcoliamo l'orario in cui scadrà
        long tempoAttuale = System.currentTimeMillis();
        long durataInMillisecondi = (long) durataPartitaMinuti * 60 * 1000;
        this.tempoFinePartitaAttuale = tempoAttuale + durataInMillisecondi;
        
        System.out.println("--- NUOVA PARTITA AVVIATA ---");
        System.out.println("Partita numero (file): " + indicePartitaCorrente);
        System.out.println("Game ID effettivo: " + partitaCorrente.getGameId());
        System.out.println("Durata: " + durataPartitaMinuti + " minuti.");

        // diamo un compito (GestisciFinePartita) al thread Timer che dovrà eseguire fra X minuti
        timerPartita.schedule(() -> {
            GestisciFinePartita();
        }, durataPartitaMinuti, TimeUnit.MINUTES);
    }




    private void GestisciFinePartita() {
        System.out.println("--- TEMPO SCADUTO! PARTITA CONCLUSA ---");
        
        // 1. Assicuriamo uno StatoPartita (anche a 0) a tutti i loggati
        //scorriamo un set con tutte le chiavi (Username)
        for (String utenteLoggato : UtentiOnline.keySet()) {
            PartiteInCorso.putIfAbsent(utenteLoggato, new StatoPartitaGiocatore());
        }
        
        // 2. AGGIORNIAMO LE STATISTICHE GLOBALI DI TUTTI I PARTECIPANTI
        for (Map.Entry<String, StatoPartitaGiocatore> entry : PartiteInCorso.entrySet()) {
            String username = entry.getKey();
            StatoPartitaGiocatore stato = entry.getValue();
            
            // Ha vinto se ha indovinato 3 temi
            boolean haVinto = (stato.getTemiIndovinati().size() == 3);
            
           
            //Cerca se esiste un valore già associato alla chiave username
            //se esiste già non fare nulla -> restituisci il vecchio profilo che hai trovato
            //se non esiste crea un nuovo profilo
            //nettamente più efficiente di putIfAbsent()
            ProfiloGiocatore profilo = ProfiliGlobali.computeIfAbsent(username, k -> new ProfiloGiocatore());
            
            // Travasiamo i dati della singola partita nel profilo della carriera!
            profilo.aggiornaStatistiche(stato.getPunti(), stato.getErrori(), haVinto);
        }
        
        // 3. Salviamo nello storico della singola partita
        ArchivioPartite.put(this.partitaCorrente.getGameId(), new RecordPartita(this.partitaCorrente, PartiteInCorso));
        
        // 4. In questo modo ogni volta che un turno scade salviamo le informazioni relative ai punteggi
        // e gestiamo la persistenza 
        SalvaDati(); 
        
        // --- Inviamo le statistiche a tutti i giocatori DOPO aver aggiornato i dati ---
       
        InviaStatisticheFinePartita();
        
        // 5. Facciamo ripartire il ciclo infinito
        AvviaNuovaPartita();
    }


    public EsitoMossa Proposta(String username, List<String> paroleGiocate) {
        
        if (!UtentiOnline.containsKey(username)) 
            return new EsitoMossa(401, "Errore: Devi fare il login per giocare", 0, 0, false);
            
        if (paroleGiocate == null || paroleGiocate.size() != 4) 
            return new EsitoMossa(400, "Errore: Devi inviare esattamente 4 parole", 0, 0, false);

        StatoPartitaGiocatore stato = PartiteInCorso.computeIfAbsent(username, k -> new StatoPartitaGiocatore());

        // Controllo se la partita era già finita nei turni precedenti
        if (stato.getErrori() >= 4 || stato.getTemiIndovinati().size() == 3) {
            return new EsitoMossa(403, "La tua partita è già terminata!", stato.getPunti(), stato.getErrori(), true);
        }

        boolean propostaCorretta = false;
        String temaIndovinato = null;

        // USA LA VARIABILE GLOBALE AGGIORNATA DALLO STREAMING
        for (Gruppo gruppo : this.partitaCorrente.getGroups()) {
            if (gruppo.getWords().containsAll(paroleGiocate)) {
                propostaCorretta = true;
                temaIndovinato = gruppo.getTheme();
                break; 
            }
        }

        if (propostaCorretta) {
            if (stato.getTemiIndovinati().contains(temaIndovinato)) {
                return new EsitoMossa(400, "Hai già indovinato questo gruppo!", stato.getPunti(), stato.getErrori(), false);
            }

            stato.aggiungiPunti();
            stato.aggiungiTemaIndovinato(temaIndovinato);
            
            if (stato.getTemiIndovinati().size() == 3) {
                // --- Inviamo le statistiche SOLO a chi ha vinto in anticipo ---
                InviaStatisticheFinePartita();
                return new EsitoMossa(200, "HAI VINTO! Gruppo indovinato: " + temaIndovinato, stato.getPunti(), stato.getErrori(), true);
            }
            
            return new EsitoMossa(200, "Risposta Esatta! Gruppo: " + temaIndovinato, stato.getPunti(), stato.getErrori(), false);

        } else {
            stato.aggiungiErrore(); 
            
            if (stato.getErrori() == 4) {
                // --- Inviamo le statistiche SOLO a chi ha perso in anticipo ---
                InviaStatisticheFinePartita();
                return new EsitoMossa(200, "GAME OVER! Hai raggiunto 4 errori.", stato.getPunti(), stato.getErrori(), true);
            }

            return new EsitoMossa(200, "Risposta Errata! Ti restano " + (4 - stato.getErrori()) + " tentativi.", stato.getPunti(), stato.getErrori(), false);
        }
    }

   public boolean RegistraUtente(String username, String password) {
        Object risultato = InfoGiocatori.putIfAbsent(username, password);
        if (risultato == null) {
            // Se la registrazione va a buon fine, gli creiamo il profilo statistico!
            ProfiliGlobali.putIfAbsent(username, new ProfiloGiocatore());
            return true;
        }
        return false;
    }

    public int AggiornaCredenziali(String oldUsername, String newUsername, String oldPsw, String newPsw) {
        String passwordSalvata = InfoGiocatori.get(oldUsername);

        if (passwordSalvata == null || !passwordSalvata.equals(oldPsw)) return 401;

        // le seguenti variabili che sfruttano il metodo trim()
        // mi consentono di avere più casistiche (es. l'utente vuole cambiare solo la psw o solo il nome)
        boolean cambiaNome = (newUsername != null && !newUsername.trim().isEmpty());
        boolean cambiaPsw = (newPsw != null && !newPsw.trim().isEmpty());

        if (!cambiaNome && !cambiaPsw) return 400;

        if (!cambiaNome && cambiaPsw) {
            boolean successo = InfoGiocatori.replace(oldUsername, oldPsw, newPsw);
            return successo ? 200 : 401;
        }

        if (cambiaNome) {
            String passwordDaSalvare = cambiaPsw ? newPsw : oldPsw;
            if (InfoGiocatori.putIfAbsent(newUsername, passwordDaSalvare) != null) return 409;

            if (!InfoGiocatori.remove(oldUsername, oldPsw)) {
                InfoGiocatori.remove(newUsername);
                return 500;
            }

            // Se ha cambiato nome, lo scolleghiamo per sicurezza (dovrà rifare il login col nuovo nome)
            UtentiOnline.remove(oldUsername);
            return 200;
        }
        return 500;
    }

    public int Login(String username, String password, InetAddress ipClient, int portaUdpClient) {
        String pswSalvata = InfoGiocatori.get(username);
        
        if (pswSalvata == null || !pswSalvata.equals(password)) return 401;
        
        // Se putIfAbsent restituisce null, significa che l'utente non c'era ed è stato inserito
        if (UtentiOnline.putIfAbsent(username, new InfoClient(ipClient, portaUdpClient)) != null) {
            return 409; // Già loggato!
        }
        
        return 200; 
    }

    public int Logout(String username) {
        // CORREZIONE: Ora rimuove fisicamente l'utente dalla mappa
        if (UtentiOnline.remove(username) != null) return 200;
        return 404; // Non era loggato
    }




   // Metodo getter per permettere al GestoreCliente di sapere l'ID della partita in corso
    public int getIdPartitaCorrente() {
        if (this.partitaCorrente != null) {
            return this.partitaCorrente.getGameId();
        }
        return -1; // Ritorna -1 come sicurezza nel caso estremo in cui non ci sia una partita
    }

    

    // Restituisce il pacchetto con i dati reali per l'utente specifico
    public InfoPartita InfoGioco(String username) {
        
        // 1. Calcolo del tempo rimanente (in secondi)
        long tempoAttuale = System.currentTimeMillis();
        long rimanenteMillis = tempoFinePartitaAttuale - tempoAttuale;
        long rimanenteSecondi = Math.max(0, rimanenteMillis / 1000); 

        // 2. Recuperiamo il taccuino del giocatore
        StatoPartitaGiocatore stato = PartiteInCorso.computeIfAbsent(username, k -> new StatoPartitaGiocatore());

        // 3. Estraiamo le parole della partita corrente, escludendo quelle già indovinate!
        List<String> paroleRimaste = new ArrayList<>();
        
        for (Gruppo gruppo : this.partitaCorrente.getGroups()) {
            if (!stato.getTemiIndovinati().contains(gruppo.getTheme())) {
                paroleRimaste.addAll(gruppo.getWords());
            }
        }

        // 4. MISCHIAMO LE PAROLE 
        java.util.Collections.shuffle(paroleRimaste);

        // 5. Restituiamo il pacchetto completo
        return new InfoPartita(rimanenteSecondi, paroleRimaste, stato.getErrori(), stato.getPunti(), stato.getTemiIndovinati());
    }



    public InfoPartitaConclusa InfoGiocoPassato(String username, int gameIdCercato) {
        RecordPartita record = ArchivioPartite.get(gameIdCercato);
        
        if (record == null) {
            return null; // La partita non esiste in archivio
        }
        
        // Se il giocatore non aveva partecipato a quella partita, gli diamo un taccuino vuoto con 0 punti
        StatoPartitaGiocatore stato = record.risultati.getOrDefault(username, new StatoPartitaGiocatore());
        
        int proposteCorrette = stato.getTemiIndovinati().size();
        
        return new InfoPartitaConclusa(
            record.partita.getGroups(), 
            proposteCorrette, 
            stato.getErrori(), 
            stato.getPunti()
        );
    }




    // 1. CLASSIFICA: Ordina tutti i giocatori per punteggio
    //ritorna un array classifica ordinato in base al punteggio di goni giocatore
    public List<StatisticheGiocatore> Leader() {
        List<StatisticheGiocatore> classifica = new ArrayList<>();
        
        for (Map.Entry<String, ProfiloGiocatore> entry : ProfiliGlobali.entrySet()) {
            classifica.add(new StatisticheGiocatore(entry.getKey(), entry.getValue().getPunteggioTotale()));
        }
        
        // Ordiniamo la lista dal punteggio più alto al più basso
        classifica.sort((a, b) -> Integer.compare(b.getPunteggioTotale(), a.getPunteggioTotale()));
        return classifica;
    }

    // 2. STATISTICHE PERSONALI: Ritorna tutto il profilo (partite vinte, streak, istogramma)
    public ProfiloGiocatore StadiGiocatori(String username) {
        return ProfiliGlobali.get(username);
    }

    // 3. STATISTICHE PARTITA GLOBALI: Quanti hanno giocato e vinto a quella specifica partita?
    public StatisticheGlobaliPartita StadiGioco(Integer gameId) {
        Map<String, StatoPartitaGiocatore> taccuiniDaAnalizzare;
        
        // Capiamo se vuole analizzare la partita in corso o una vecchia
        if (gameId == null || gameId == getIdPartitaCorrente()) {
            taccuiniDaAnalizzare = PartiteInCorso;
        } else {
            RecordPartita record = ArchivioPartite.get(gameId);
            if (record == null) return null; // Partita non trovata!
            taccuiniDaAnalizzare = record.risultati;
        }

        int partecipanti = taccuiniDaAnalizzare.size();
        int vittorie = 0;
        int sconfitte = 0;

        // Analizziamo tutti i taccuini di quel turno
        for (StatoPartitaGiocatore stato : taccuiniDaAnalizzare.values()) {
            if (stato.getTemiIndovinati().size() == 3) vittorie++;
            else if (stato.getErrori() == 4) sconfitte++;
        }

        return new StatisticheGlobaliPartita(partecipanti, vittorie, sconfitte);
    }


//Il seguente metodo si occupa di inviare notifiche asincrone ai giocatori
//quando perdono, quando vincono e quando scatta il timer
    private void InviaNotificheUnicast (String messaggio) {
        // Creiamo il socket UDP
        try (DatagramSocket udpSocket = new DatagramSocket(porta_udp)) {
            
            byte[] buffer = messaggio.getBytes();
            
            // Cicliamo su tutti gli utenti attualmente loggati
            for (Map.Entry<String, InfoClient> entry : UtentiOnline.entrySet()) {
                InfoClient destinatario = entry.getValue();
                
                // Creiamo un pacchetto personalizzato per questo specifico client
                DatagramPacket pacchetto = new DatagramPacket(
                    buffer, 
                    buffer.length, 
                    destinatario.ip, 
                    destinatario.portaUdp
                );
                
                // Lo spediamo!
                udpSocket.send(pacchetto);
            }
            System.out.println("Notifiche UDP Unicast inviate a " + UtentiOnline.size() + " giocatori.");
            
        } catch (Exception e) {
            System.err.println("Errore durante l'invio delle notifiche UDP: " + e.getMessage());
        }
    }




//questo metodo si occupa di costruire di prelevare le statistiche e le classifiche 
//di ogni partita da inviare poi ai giocatori in maniera asincrona
//in sostanza costruisce l'input per il metodo (InviaNotificheUnicast)
    private void InviaStatisticheFinePartita() {
        int gameId = getIdPartitaCorrente();
        
        // 1. Statistiche della partita (Vittorie/Sconfitte totali del turno)
        StatisticheGlobaliPartita statPartita = StadiGioco(gameId);
        
        // 2. Calcoliamo la classifica solo di questa partita!
        List<StatisticheGiocatore> classificaDiPartita = new ArrayList<>();
        for (Map.Entry<String, StatoPartitaGiocatore> entry : PartiteInCorso.entrySet()) {
            // Prendiamo il nome e i punti fatti in questo taccuino
            classificaDiPartita.add(new StatisticheGiocatore(entry.getKey(), entry.getValue().getPunti()));
        }
        // Ordiniamo dal punteggio più alto al più basso
        classificaDiPartita.sort((a, b) -> Integer.compare(b.getPunteggioTotale(), a.getPunteggioTotale()));

        // 3. Creiamo la HashMap per Gson
        java.util.Map<String, Object> datiFinePartita = new java.util.HashMap<>();
        datiFinePartita.put("tipo", "FINE_PARTITA");
        datiFinePartita.put("statistichePartita", statPartita);
        datiFinePartita.put("leaderboard", classificaDiPartita); 

        String messaggioJson = gson.toJson(datiFinePartita);

        
        InviaNotificheUnicast(messaggioJson); 
    }
    


    //Gestione della persistenza 
    private void SalvaDati() {
        Gson gson = new Gson();

        // Usiamo il try-with-resources per aprire e poi chiudere automaticamente i file
        try (FileWriter writerUtenti = new FileWriter("utenti.json");
             FileWriter writerStatistiche = new FileWriter("statistiche.json");
             // --- Salviamo l'indice della partita ---
             FileWriter writerIndice = new FileWriter("indice_partita.txt");
             //salviamo l'archivio delle partite passate
             FileWriter writerArchivio = new FileWriter("archivio.json")) {
            
            // 1. Salviamo le password nel primo file
            gson.toJson(InfoGiocatori, writerUtenti); 
            
            // 2. Salviamo i profili storici nel secondo file 
            gson.toJson(ProfiliGlobali, writerStatistiche); 

            // 3. Salviamo l'indice a cui siamo arrivati
            //prendo il valore di indicePartitaCorrente che avevo incrementato
            //all'avvio di una nuova partita (in AvviaNuovaPartita )e strasformo in stringa per scriverlo 
            //nel file di testo
            writerIndice.write(String.valueOf(this.indicePartitaCorrente));
            
            //salviamo l'intero archivio delle partite vecchie in modo tale che 
            // quando riavviamo il server saremo comunque in grado di ricavare le 
            //info relative alle partite pre riavvio
            gson.toJson(ArchivioPartite, writerArchivio);

            

        } catch (IOException e) {
            System.err.println("Errore critico durante il salvataggio dei file: " + e.getMessage());
        }
    }

    // --- METODO PER CARICARE I DATI (Da chiamare all'avvio del server) ---
    private void CaricaDatiSalvati() {
        // 1. CARICAMENTO UTENTI
        try (FileReader readerUtenti = new FileReader("utenti.json")) {
            Type tipoUtenti = new TypeToken<ConcurrentHashMap<String, String>>(){}.getType();
            Map<String, String> utentiSalvati = gson.fromJson(readerUtenti, tipoUtenti);
            
            if (utentiSalvati != null) {
                InfoGiocatori.putAll(utentiSalvati);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File utenti.json non trovato (Primo avvio).");
        } catch (IOException e) {
            System.err.println("Errore nella lettura di utenti.json: " + e.getMessage());
        }

        // 2. CARICAMENTO STATISTICHE GLOBALI
        try (FileReader readerStatistiche = new FileReader("statistiche.json")) {
            Type tipoProfili = new TypeToken<ConcurrentHashMap<String, ProfiloGiocatore>>(){}.getType();
            Map<String, ProfiloGiocatore> statisticheSalvate = gson.fromJson(readerStatistiche, tipoProfili);
            
            if (statisticheSalvate != null) {
                ProfiliGlobali.putAll(statisticheSalvate);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File statistiche.json non trovato (Primo avvio).");
        } catch (IOException e) {
            System.err.println("Errore nella lettura di statistiche.json: " + e.getMessage());
        }
        
        


        // 3. CARICAMENTO ARCHIVIO PARTITE PASSATE
        try (FileReader readerArchivio = new FileReader("archivio.json")) {
            // Diciamo a Gson che tipo di struttura stiamo leggendo
            Type tipoArchivio = new TypeToken<ConcurrentHashMap<Integer, RecordPartita>>(){}.getType();
            Map<Integer, RecordPartita> archivioSalvato = gson.fromJson(readerArchivio, tipoArchivio);
            
            if (archivioSalvato != null) {
                ArchivioPartite.putAll(archivioSalvato);
            }
            
        } catch (FileNotFoundException e) {
            System.out.println("File archivio.json non trovato (Primo avvio o archivio vuoto).");
        } catch (IOException e) {
            System.err.println("Errore nella lettura di archivio.json: " + e.getMessage());
        }
        
        
    }

    // --- Metodo per leggere l'indice all'avvio ---
    //legge il contenuto del file che contiene l'indice della partita a cui eravamo arrivati e ne estrae l'unico int che trova (senza spazi)
    private int CaricaIndicePartita() {
        try (BufferedReader br = new BufferedReader(new FileReader("indice_partita.txt"))) {
            return Integer.parseInt(br.readLine().trim());
        } catch (Exception e) {
            return 0; // Se è il primo avvio o il file non esiste, riparte da 0
        }
    }


// --- METODO PER LO SHUTDOWN ---
    public void spegniServer() {

        // 1. Salviamo tutti i dati (Utenti, Storico, Indice)
        SalvaDati();

        // 2. Fermiamo il Timer
        if (timerPartita != null && !timerPartita.isShutdown()) {
            timerPartita.shutdownNow();
            
        }

        // 3. Chiudiamo il file JSON
        try {
            if (jsonReader != null) {
                jsonReader.close();
                
            }
        } catch (IOException e) {
            System.err.println("[SHUTDOWN] Errore durante la chiusura del file JSON: " + e.getMessage());
        }

        System.out.println("Arrivederci!");
    }




}