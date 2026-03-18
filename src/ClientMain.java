
import java.io.FileInputStream;
import java.util.Properties;


public class ClientMain {
    public static void main(String[] args) {
        Properties config = new Properties();
        int portaTcp; 
        String indirizzoServer; 
        // Legge le impostazioni
        try (FileInputStream input = new FileInputStream("client.properties")) {
            config.load(input);
            portaTcp = Integer.parseInt(config.getProperty("porta_tcp"));
            indirizzoServer = config.getProperty("indirizzo_server"); 
        } catch (Exception e) {
            
            System.err.println("Errore CRITICO: Impossibile leggere il file 'server.properties' o parametro 'porta_tcp' mancante.");
            System.err.println("Chiusura forzata del client.");
            return; // Termina immediatamente l'esecuzione del main
        }

        try {
            GestoreReteNIO rete = new GestoreReteNIO();
            rete.connetti(indirizzoServer, portaTcp);
            
            MenuClient menu = new MenuClient(rete);
            menu.avvia();
            
        } catch (Exception e) {
            System.err.println("Impossibile avviare il client: " + e.getMessage());
        }
    }
}