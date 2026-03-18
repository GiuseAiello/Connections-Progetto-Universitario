import com.google.gson.Gson;
import java.util.Scanner;
import java.util.List;
import java.util.Arrays;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MenuClient {
    private GestoreReteNIO rete;
    private Scanner scanner;
    private Gson gson;
    
    private boolean utenteLoggato = false;
    private String usernameAttuale = "";
    
    // Per gestire le notifiche asincrone di fine partita
    private DatagramSocket udpSocket;
    private Thread threadNotifiche;

    public MenuClient(GestoreReteNIO rete) {
        this.rete = rete;
        this.scanner = new Scanner(System.in);
        this.gson = new Gson();
    }

    public void avvia() {
        boolean esci = false;
        System.out.println("=== BENVENUTO NEL GIOCO DELLE PAROLE ===");

        while (!esci) {
            if (!utenteLoggato) {
                esci = mostraMenuPreLogin();
            } else {
                esci = mostraMenuGioco();
            }
        }
        System.out.println("Chiusura in corso...");
        scanner.close();
    }

    // --- MENU 1: NON LOGGATO ---
    private boolean mostraMenuPreLogin() {
        System.out.println("\n--- MENU PRINCIPALE ---");
        System.out.println("1. Registrati (register)");
        System.out.println("2. Effettua il Login (login)");
        System.out.println("3. Aggiorna Credenziali (updateCredentials)");
        System.out.println("4. Esci");
        System.out.print("Scelta: ");
        
        String scelta = scanner.nextLine();

        switch (scelta) {
            case "1": eseguiRegistrazione(); break;
            case "2": eseguiLogin(); break;
            case "3": eseguiAggiornaCredenziali(); break;
            case "4": return true; // Fa terminare il ciclo nel main
            default: System.out.println("Scelta non valida.");
        }
        return false;
    }

    // --- MENU 2: LOGGATO ---
    private boolean mostraMenuGioco() {
        System.out.println("\n--- BENTORNATO, " + usernameAttuale + " ---");
        System.out.println("1. Richiedi info partita (requestGameInfo)");
        System.out.println("2. Invia proposta parole (submitProposal)");
        System.out.println("3. Statistiche Partita (requestGameStats)");
        System.out.println("4. Classifica (requestLeaderboard)");
        System.out.println("5. Le mie Statistiche (requestPlayerStats)");
        System.out.println("6. Logout");
        System.out.print("Scelta: ");
        
        String scelta = scanner.nextLine();

        switch (scelta) {
            case "1": richiediInfoPartita(); break;
            case "2": inviaProposta(); break;
            case "3": richiediStatistichePartita(); break;
            case "4": richiediClassifica(); break;
            case "5": richiediMieStatistiche(); break;
            case "6": eseguiLogout(); break;
            default: System.out.println("Scelta non valida.");
        }
        return false;
    }

    // ==========================================
    // IMPLEMENTAZIONE METODI PRE-LOGIN
    // ==========================================

    private void eseguiRegistrazione() {
        System.out.print("Username: ");
        String user = scanner.nextLine(); 
        System.out.print("Password: ");  
        String psw = scanner.nextLine(); 
        
        MessaggioClient msg = new MessaggioClient();
        msg.setOperation("register");
        msg.setUsername(user); 
        msg.setPsw(psw); 

        try {
            String stringaJsonRisposta = rete.inviaEAttendiRisposta(gson.toJson(msg));
            RispostaServer rs = gson.fromJson(stringaJsonRisposta, RispostaServer.class);
            System.out.println("[Server]: " + rs.getRisposta());
        } catch (Exception e) {
            System.err.println("Errore di rete: " + e.getMessage());
        }
    }

    private void eseguiLogin() {
        System.out.print("Username: ");
        String user = scanner.nextLine();
        System.out.print("Password: ");
        String psw = scanner.nextLine();

        MessaggioClient msg = new MessaggioClient();
        msg.setOperation("login");
        msg.setUsername(user);
        msg.setPsw(psw);

        try {
            // Ci facciamo assegnare una porta libera dal Sistema Operativo
            this.udpSocket = new DatagramSocket(0); 
            //comunichiamo al server qual'è la porta che ci è stata assegnata
            msg.setUdpPort(this.udpSocket.getLocalPort());

            String stringaJsonRisposta = rete.inviaEAttendiRisposta(gson.toJson(msg));
            RispostaServer rs = gson.fromJson(stringaJsonRisposta, RispostaServer.class);
            System.out.println("[Server]: " + rs.getRisposta());
            
            
            if (rs.getEsito() == true) {
                this.utenteLoggato = true;
                this.usernameAttuale = user;
                
                // Facciamo partire l'ascoltatore UDP non appena il login ha successo
                avviaAscoltatoreUDP();   



            } else {
                this.udpSocket.close(); // Login fallito, chiudiamo la porta UDP creata
            }
        } catch (Exception e) {
            System.err.println("Errore di rete: " + e.getMessage());
        }
    }

    private void eseguiAggiornaCredenziali() {
        System.out.print("Vecchio Username: ");
        String vecchio_user = scanner.nextLine(); 
        System.out.print("Vecchia Password: "); 
        String vecchia_psw = scanner.nextLine(); 
        
        System.out.print("Nuovo Username (Premi INVIO per non cambiarlo): ");
        String new_user = scanner.nextLine(); 
        System.out.print("Nuova Password (Premi INVIO per non cambiarla): "); 
        String new_psw = scanner.nextLine(); 

        MessaggioClient msg = new MessaggioClient(); 
        msg.setOperation("updateCredentials"); 
        msg.setOldUsername(vecchio_user);
        msg.setOldPsw(vecchia_psw);
        
        if (!new_user.trim().isEmpty()) msg.setNewUsername(new_user);
        if (!new_psw.trim().isEmpty()) msg.setNewPsw(new_psw);

        try {
            String stringaJsonRisposta = rete.inviaEAttendiRisposta(gson.toJson(msg));
            RispostaServer rs = gson.fromJson(stringaJsonRisposta, RispostaServer.class);
            System.out.println("[Server]: " + rs.getRisposta());
        } catch (Exception e) {
            System.err.println("Errore di rete: " + e.getMessage());
        }
    }

    // ==========================================
    // IMPLEMENTAZIONE METODI POST-LOGIN
    // ==========================================

    private void eseguiLogout() {
        MessaggioClient msg = new MessaggioClient();
        msg.setOperation("logout");
        msg.setUsername(usernameAttuale);

        try {
            String stringaJsonRisposta = rete.inviaEAttendiRisposta(gson.toJson(msg));
            RispostaServer rs = gson.fromJson(stringaJsonRisposta, RispostaServer.class);
            System.out.println("[Server]: " + rs.getRisposta());
            
            if (rs.getEsito() == true) {
                this.utenteLoggato = false;
                this.usernameAttuale = "";
                
                if (udpSocket != null && !udpSocket.isClosed()) {
                    udpSocket.close(); 
                }
            }
        } catch (Exception e) {
            System.err.println("Errore di rete: " + e.getMessage());
        }
    }

    private void richiediInfoPartita() {
        System.out.print("Vuoi lo storico di una vecchia partita? Inserisci il GameID (o premi INVIO per la partita corrente): ");
        String input = scanner.nextLine();
        
        MessaggioClient msg = new MessaggioClient();
        msg.setOperation("requestGameInfo");
        msg.setUsername(usernameAttuale);
        
        if (!input.trim().isEmpty()) {
            try {
                msg.setGameId(Integer.parseInt(input));
            } catch (NumberFormatException e) {
                System.out.println("ID non valido. Richiedo la partita in corso.");
            }
        }

        try {
            String rispostaJson = rete.inviaEAttendiRisposta(gson.toJson(msg));
            System.out.println("\n[Server Info Partita]:\n" + rispostaJson);
        } catch (Exception e) {
            System.err.println("Errore di rete: " + e.getMessage());
        }
    }

    private void inviaProposta() {
        System.out.println("Inserisci 4 parole separate da uno spazio:");
        String input = scanner.nextLine();
        List<String> paroleGiocate = Arrays.asList(input.split("\\s+"));

        MessaggioClient msg = new MessaggioClient();
        msg.setOperation("submitProposal");
        msg.setUsername(usernameAttuale);
        msg.setWords(paroleGiocate);

        try {
            String rispostaJson = rete.inviaEAttendiRisposta(gson.toJson(msg));
            RispostaServer rs = gson.fromJson(rispostaJson, RispostaServer.class);
            
            if (rs.getDatiMossa() != null) {
                System.out.println("\n[Esito Mossa]: " + rs.getDatiMossa().getMessaggio());
                System.out.println("Punti: " + rs.getDatiMossa().getPuntiAttuali() + " | Errori: " + rs.getDatiMossa().getErroriFatti());
            } else {
                System.out.println("[Server]: " + rs.getRisposta()); // In caso di errore di formato
            }
        } catch (Exception e) {
            System.err.println("Errore di rete: " + e.getMessage());
        }
    }

    private void richiediStatistichePartita() {
        System.out.print("Inserisci il GameID della partita (o premi INVIO per le statistiche di quella attuale): ");
        String input = scanner.nextLine();
        
        MessaggioClient msg = new MessaggioClient();
        msg.setOperation("requestGameStats");
        
        if (!input.trim().isEmpty()) {
            try {
                msg.setGameId(Integer.parseInt(input));
            } catch (NumberFormatException e) {
                System.out.println("ID non valido. Richiedo la partita in corso.");
            }
        }

        try {
            String rispostaJson = rete.inviaEAttendiRisposta(gson.toJson(msg));
            System.out.println("\n[Statistiche Partita]:\n" + rispostaJson);
        } catch (Exception e) {
            System.err.println("Errore di rete: " + e.getMessage());
        }
    }

    private void richiediClassifica() {
        System.out.println("Richiesta classifica in corso...");
        
        MessaggioClient msg = new MessaggioClient();
        msg.setOperation("requestLeaderboard");

        try {
            String rispostaJson = rete.inviaEAttendiRisposta(gson.toJson(msg));
            System.out.println("\n[Classifica Generale]:\n" + rispostaJson);
        } catch (Exception e) {
            System.err.println("Errore di rete: " + e.getMessage());
        }
    }

    private void richiediMieStatistiche() {
        System.out.println("Recupero il tuo storico personale...");
        
        MessaggioClient msg = new MessaggioClient();
        msg.setOperation("requestPlayerStats");
        msg.setUsername(usernameAttuale);

        try {
            String rispostaJson = rete.inviaEAttendiRisposta(gson.toJson(msg));
            System.out.println("\n[Le Mie Statistiche]:\n" + rispostaJson);
        } catch (Exception e) {
            System.err.println("Errore di rete: " + e.getMessage());
        }
    }



    //Thread che fa da ascoltatore UDP
    // ==========================================
    // NOTIFICHE ASINCRONE (UDP)
    // ==========================================

    //questo thread rimane continuamente in ascolto di notifiche asincrone da quando faccio il login
    //a quando faccio il logout (udpSocket.close();)
    
    private void avviaAscoltatoreUDP() {
        this.threadNotifiche = new Thread(() -> {
            try {
                // 1. AUMENTIAMO IL BUFFER!
                // Un JSON con la classifica intera è pesante. Lo portiamo a 8192 byte per stare sicuri.
                byte[] buffer = new byte[8192];

                while (utenteLoggato && !udpSocket.isClosed()) {
                    
                    DatagramPacket pacchettoInArrivo = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(pacchettoInArrivo);
                    
                    String messaggioJson = new String(
                        pacchettoInArrivo.getData(), 
                        0, 
                        pacchettoInArrivo.getLength()
                    );

                    try {
                        // 2. INTERPRETIAMO IL NUOVO JSON
                        // Trasformiamo la stringa in una Mappa per capire cosa ci ha mandato il server
                        java.util.Map<String, Object> dati = gson.fromJson(messaggioJson, java.util.Map.class);
                        
                        // Controlliamo se è la "busta" speciale con le statistiche
                        if (dati != null && "FINE_PARTITA".equals(dati.get("tipo"))) {
                            System.out.println("\n\n=== PARTITA CONCLUSA! ECCO I RISULTATI ===");
                            System.out.println("[Statistiche del Turno]:\n" + gson.toJson(dati.get("statistichePartita")));
                            System.out.println("[Classifica Generale Aggiornata]:\n" + gson.toJson(dati.get("leaderboard")));
                            System.out.println("=============================================");
                            System.out.print("Scelta: ");
                        } else {
                            // Se fosse un messaggio normale, lo stampiamo classico
                            System.out.println("\n\n[ALLARME DAL SERVER]: " + messaggioJson);
                            System.out.print("Scelta: ");
                        }
                    } catch (Exception e) {
                        // Se non è un JSON formattato, lo stampiamo così com'è
                        System.out.println("\n\n[MESSAGGIO DAL SERVER]: " + messaggioJson);
                        System.out.print("Scelta: ");
                    }
                }
            } catch (Exception e) {
                if (utenteLoggato) {
                    System.err.println("Errore nell'ascolto delle notifiche UDP: " + e.getMessage());
                }
            }
        });

        this.threadNotifiche.setDaemon(true); 
        this.threadNotifiche.start(); 
    }
}