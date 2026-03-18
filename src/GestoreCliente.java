//La classe GestoreCliente è un runnable che fa da interfaccia con
//il client preleva le sue richieste le fa processare al 
//ControlloreGioco e rispedisce indietro al client l'esito

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress; // NUOVO IMPORT per prelevare l'IP
import java.lang.reflect.Type; 
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;



import java.util.List;

public class GestoreCliente implements Runnable {
    
    Socket socket; 
    ControlloreGioco controllore; 
    
    public GestoreCliente(Socket socket, ControlloreGioco controllore){
        this.socket = socket; 
        this.controllore = controllore; 
    }

    @Override
    public void run(){
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            boolean connesso = true; 

            while (connesso) {
                String message = in.readLine();

                if (message == null){
                    System.out.println("Il client si è disconnesso improvvisamente.");
                    connesso = false; 
                    break;          
                }

                try {
                    Gson gson = new Gson();
                    Type MessaggioType = new TypeToken<MessaggioClient>(){}.getType();
                    MessaggioClient ms = gson.fromJson(message, MessaggioType);

                    switch (ms.getOperation()) {
                        case "register": {
                            String utente = ms.getUsername(); 
                            String psw = ms.getPsw(); 
                            
                            //gestione di un messaggio malformato per la registrazione 
                            if (utente == null || psw == null || utente.isEmpty() || psw.isEmpty()) {
                                RispostaServer rs = new RispostaServer(false, "Errore: Username o password mancanti", 400); 
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson); 
                                break;
                            }

                            boolean esito = controllore.RegistraUtente(utente, psw);
                            
                            if (esito == true) {
                                RispostaServer rs = new RispostaServer(true, "Registrazione avvenuta correttamente.", 200); 
                                String rispostaJson = gson.toJson(rs); 
                                out.println(rispostaJson); 
                            } else {
                                RispostaServer rs = new RispostaServer(false, "Nome già in uso", 409); 
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            }
                            break; 
                        }

                        case "updateCredentials": {
                            int code = controllore.AggiornaCredenziali(ms.getOldUsername(), ms.getNewUsername(), ms.getOldPsw(), ms.getNewPsw());
                            
                            if (code == 200) {
                                RispostaServer rs = new RispostaServer(true, "Credenziali aggiornate con successo", 200);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            } 
                            else if (code == 401) {
                                RispostaServer rs = new RispostaServer(false, "Errore: Vecchie credenziali errate", 401);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            } 
                            else if (code == 400) {
                                RispostaServer rs = new RispostaServer(false, "Errore: Nessun dato nuovo fornito", 400);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            } 
                            else if (code == 409) {
                                RispostaServer rs = new RispostaServer(false, "Errore: Il nuovo nome utente è già occupato", 409);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            } 
                            else {
                                RispostaServer rs = new RispostaServer(false, "Errore critico del server", 500);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            }
                            break;
                        }

                        case "login": {
                            String utente = ms.getUsername(); 
                            String psw = ms.getPsw();
                            
                            if (utente == null || psw == null) {
                                RispostaServer rs = new RispostaServer(false, "Errore: Dati mancanti", 400);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                                break;
                            }
                            
                            
                            // 1. Estraiamo l'IP del client dalla connessione TCP
                            InetAddress ipClient = socket.getInetAddress();
                            
                            // 2. Estraiamo la porta UDP dal JSON inviato dal client
                          
                            int portaUdpClient = ms.getUdpPort();
                            
                            // 3. Passiamo i nuovi parametri al Controllore
                            int code = controllore.Login(utente, psw, ipClient, portaUdpClient);
                            // ----------------------------
                            
                            if (code == 200) {
                                RispostaServer rs = new RispostaServer(true, "Login effettuato con successo", 200);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            } 
                            else if (code == 401) {
                                RispostaServer rs = new RispostaServer(false, "Errore: Credenziali errate", 401);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            } 
                            else if (code == 409) {
                                RispostaServer rs = new RispostaServer(false, "Errore: Utente già online", 409);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            }
                            break;
                        }

                        case "logout": {
                            String utente = ms.getUsername();
                            int code = controllore.Logout(utente);
                            
                            if (code == 200) {
                                RispostaServer rs = new RispostaServer(true, "Logout effettuato con successo", 200);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            } else {
                                RispostaServer rs = new RispostaServer(false, "Errore: Non eri loggato", 400);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            }
                            connesso = false; 
                            break;
                        }

                        case "submitProposal": {
                            String utente = ms.getUsername();
                            List<String> parole = ms.getWords();
        
                            // Ora il controllore ci restituisce tutto il pacchetto EsitoMossa
                            EsitoMossa esito = controllore.Proposta(utente, parole);
        
                            if (esito.getStatusCode() == 200) {
                                
                                RispostaServer rs = new RispostaServer(true, 200, esito);
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            } 
                            else {
                                // Se c'è un errore (es. 400 o 401), mandiamo esito false e il messaggio
                                RispostaServer rs = new RispostaServer(false, esito.getMessaggio(), esito.getStatusCode());
                                String rispostaJson = gson.toJson(rs);
                                out.println(rispostaJson);
                            }
                            break;
                        }

                      case "requestGameInfo": {
                            String utente = ms.getUsername();
                            Integer gameId = ms.getGameId(); // Può essere null
                            
                            // SEGNALAZIONE: Se il gameId è nullo o uguale alla partita corrente, vuole quella in corso
                            if (gameId == null || gameId == controllore.getIdPartitaCorrente()) {
                                InfoPartita info = controllore.InfoGioco(utente);
                                RispostaServer rs = new RispostaServer(true, 200, info);
                                out.println(gson.toJson(rs));
                            } 
                            // ALTRIMENTI: Vuole una partita passata dallo storico
                            else {
                                InfoPartitaConclusa infoStorico = controllore.InfoGiocoPassato(utente, gameId);
                                
                                if (infoStorico != null) {
                                    RispostaServer rs = new RispostaServer(true, 200, infoStorico);
                                    out.println(gson.toJson(rs));
                                } else {
                                    // Se ha chiesto un ID che non abbiamo in memoria
                                    RispostaServer rs = new RispostaServer(false, "Errore: Partita non trovata in archivio", 404);
                                    out.println(gson.toJson(rs));
                                }
                            }
                            break;
                        }



                       case "requestLeaderboard": {
                            List<StatisticheGiocatore> classifica = controllore.Leader();
                            RispostaServer rs = new RispostaServer(true, 200, classifica);
                            out.println(gson.toJson(rs));
                            break;
                        }

                        case "requestPlayerStats": {
                            String utente = ms.getUsername();
                            ProfiloGiocatore profilo = controllore.StadiGiocatori(utente);
                            
                            if (profilo != null) {
                                RispostaServer rs = new RispostaServer(true, 200, profilo);
                                out.println(gson.toJson(rs));
                            } else {
                                RispostaServer rs = new RispostaServer(false, "Errore: Statistiche non trovate. Hai mai giocato?", 404);
                                out.println(gson.toJson(rs));
                            }
                            break;
                        }

                        case "requestGameStats": {
                            Integer gameId = ms.getGameId(); // Prende l'ID richiesto (può essere null)
                            StatisticheGlobaliPartita stat = controllore.StadiGioco(gameId);
                            
                            if (stat != null) {
                                RispostaServer rs = new RispostaServer(true, 200, stat);
                                out.println(gson.toJson(rs));
                            } else {
                                RispostaServer rs = new RispostaServer(false, "Errore: Partita non presente in archivio", 404);
                                out.println(gson.toJson(rs));
                            }
                            break;
                        }

                        default:
                            throw new AssertionError("Operazione dal client non riconosciuta: " + ms.getOperation());
                    }
                    
                } catch (JsonSyntaxException e) {
                    System.err.println("Il client ha inviato un JSON non valido: " + e.getMessage());
                } catch (Exception e){
                    System.err.println("Errore interno durante l'elaborazione: " + e.getMessage());
                }
            }
        } catch (IOException e){
            System.err.println("Errore di connessione col client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}