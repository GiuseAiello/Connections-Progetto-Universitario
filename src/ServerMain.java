import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerMain {
    
    // Variabile globale per dire al server quando fermarsi
    private static volatile boolean inEsecuzione = true;

    // Lista per tenere traccia di tutti i socket aperti
    private static List<Socket> clientConnessi = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        Properties config = new Properties();

        try (FileInputStream input = new FileInputStream("server.properties")) {
            config.load(input);

            int durata = Integer.parseInt(config.getProperty("durata_partita"));
            int porta = Integer.parseInt(config.getProperty("porta_tcp"));
            int portaUDP = Integer.parseInt(config.getProperty("porta_udp"));
            String nomeFileDizionario = config.getProperty("file_dizionario");

            ControlloreGioco controllore = new ControlloreGioco(nomeFileDizionario, durata, portaUDP);
            ExecutorService operatori = Executors.newCachedThreadPool();

            System.out.println("Avvio del server sulla porta: " + porta);

            // Apriamo la ServerSocket fuori dal Thread del terminale per potervi accedere
            try (ServerSocket servSock = new ServerSocket(porta)) {
                
                // --- THREAD DEL TERMINALE (Ascolta la tastiera) ---
                Thread consoleThread = new Thread(() -> {
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Digita 'exit' per spegnere il server.");
                    while (inEsecuzione) {
                        String comando = scanner.nextLine();
                        if (comando.equalsIgnoreCase("exit")) {
                            System.out.println("Comando 'exit' ricevuto. Avvio spegnimento...");
                            inEsecuzione = false; // Fermiamo il ciclo while principale
                            
                            // 1. Facciamo le pulizie (salviamo i dati)
                            controllore.spegniServer();

                            //chiudiamo forzatamente tutti i socket del client connessi
                            //Dato che vi sono dei thread fermi sulle readLine() nel GestoreCliente
                            //in questo modo noi sblocchiamo questi thread dicendo loro di non aspettarsi
                            //più una risposta dal client
                            for (Socket clientSocket : clientConnessi) {
                                //forzando la chiusura dei socket connessi facciamo
                                //scattare un'eccezione nei thread in ascolto
                                //che altrimenti si bloccherebero finche tutti i client non hanno deciso
                                //di uscire 
                                try {
                                    if (!clientSocket.isClosed()) {
                                        clientSocket.close();
                                    }
                                } catch (IOException e) {
                                    System.err.println("Errore chiusura client: " + e.getMessage());
                                }
                            }


                            operatori.shutdown(); // Fermiamo il thread pool
                            
                            // 2. Chiudiamo la ServerSocket per sbloccare l'accept
                            //a quel punto l'accept() l'ancerà una SocketException
                            //il main cattura l'eccezione nel catch ed esce esce dal programma
                            try {
                                servSock.close(); 
                            } catch (IOException e) {
                                System.err.println("Errore chiusura socket: " + e.getMessage());
                            }
                            break; // Usciamo dal ciclo scanner
                        }
                    }
                });
                consoleThread.start();
                // --- FINE THREAD TERMINALE ---

                // --- CICLO PRINCIPALE DEL SERVER ---
                while (inEsecuzione) { 
                    try {
                        Socket clientSocket = servSock.accept(); 
                        //aggiungiamo il socket appena connesso alla nostra lista 
                        clientConnessi.add(clientSocket);

                        operatori.submit(new GestoreCliente(clientSocket, controllore));
                    } catch (SocketException e) {
                        // Quando il thread del terminale fa "servSock.close()", 
                        // l'accept() si sblocca e lancia questa eccezione.
                        if (!inEsecuzione) {
                            //usciamo dal programma 
                        } else {
                            System.err.println("Errore di rete improvviso: " + e.getMessage());
                        }
                    }
                }
                
            } catch (IOException e) {
                System.err.println("Errore nell'apertura della porta: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Errore: File 'server.properties' non trovato!");
        }
    }
}