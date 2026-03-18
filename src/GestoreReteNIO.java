import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class GestoreReteNIO {
    private SocketChannel clientChannel;
    private Selector selector;


//1° metodo 
    public void connetti(String indirizzo, int porta) throws IOException {
        // 1. CREAZIONE DEL CANALE E DEL SELECTOR
        this.clientChannel = SocketChannel.open();
        this.clientChannel.configureBlocking(false); 
        this.selector = Selector.open();

        // 2. AVVIO LA CONNESSIONE
        //in maniera non bloccante lanciamo la richiesta al SO e andiamo avanti
        clientChannel.connect(new InetSocketAddress(indirizzo, porta));
        //chiedo al selector di avvisarmi non appena la connessione viene stabilita
        clientChannel.register(selector, SelectionKey.OP_CONNECT);

        // Aspettiamo che la connessione si stabilisca
        while (true) {
            selector.select(); 
            //iteratore che ci servirà per scorrere "le chiavi dei canali pronti"
            Iterator<SelectionKey> iteratore = selector.selectedKeys().iterator();

            while (iteratore.hasNext()) {
                SelectionKey chiave = iteratore.next();
                //per evitare di incorrere in un errore dato dal comportamento cumulativo
                //del selector, dobbiamo rimuovere la chiave 
                //aka evitare di gestire chiavi già gestite in precedenza 
                iteratore.remove(); 

                if (chiave.isConnectable()) {
                    if (clientChannel.isConnectionPending()) {
                        //completiamo il 3 hand shaking di tcp
                        clientChannel.finishConnect();
                    }
                    
                    // Rimuoviamo l'interesse, ora siamo connessi
                    //il client non viene svegliato dal selector per nessun motivo
                    clientChannel.register(selector, 0); 
                    return; // Usciamo dal metodo, siamo pronti a giocare
                }
            }
        }
    }

//2° metodo
    // Metodo che il Menu chiamerà per mandare il JSON e ricevere la risposta
    public String inviaEAttendiRisposta(String jsonMessaggio) throws IOException {
        // 1. Mettiamoci in attesa di poter scrivere
        clientChannel.register(selector, SelectionKey.OP_WRITE);
        
        // Aggiungiamo \n perché il server usa readLine()
        String payload = jsonMessaggio + "\n"; 
        //wrap è simile ad allocate 
        //con la differenza che non alloco dello spazio vuoto all'inizio
        //in quanto possiedo già un array di byte payload.getBytes()
        //in genere si utilizza allocate per la lettura
        //mentre wrap per la scrittura 
        ByteBuffer bufferScrittura = ByteBuffer.wrap(payload.getBytes());
        String rispostaServer = null;

        while (true) {
            selector.select();
            Iterator<SelectionKey> iteratore = selector.selectedKeys().iterator();

            while (iteratore.hasNext()) {
                //una volta che prendo la chiave la rimuovo
                SelectionKey chiave = iteratore.next();
                iteratore.remove();

                if (!chiave.isValid()) continue;

                // FASE A: SCRIVIAMO IL MESSAGGIO
                //se possiamo inviare ci viene dato il via libera dal selector 
                //allora a quel punto il canale scarica il bytebuffer sulla rete
                if (chiave.isWritable()) {
                    //leggo dal buffer per scrivere nel canale
                    //(questo non ci garantisce che spediremo tutti i byte in un una volta)
                    clientChannel.write(bufferScrittura);
                    //se ho finito di inviare tutti i byte del mio messaggio
                    //cambio interesse -> .OP_READ, altrimenti ciclo
                    if (!bufferScrittura.hasRemaining()) {
                        // Abbiamo finito di scrivere, ora vogliamo LEGGERE la risposta
                        clientChannel.register(selector, SelectionKey.OP_READ);
                    }
                }
                
                
                // FASE B: LEGGIAMO LA RISPOSTA
                else if (chiave.isReadable()) {
                    ByteBuffer bufferLettura = ByteBuffer.allocate(2048);
                    //leggo dal canale per scrivere sul buffer
                    int byteLetti = clientChannel.read(bufferLettura);
                    
                    if (byteLetti > 0) {
                        bufferLettura.flip();
                        rispostaServer = new String(bufferLettura.array(), 0, bufferLettura.limit());
                        
                        // Ripuliamo gli interessi del canale in attesa del prossimo comando
                        clientChannel.register(selector, 0);
                        return rispostaServer; // Restituiamo il JSON al Menu!
                    //se non ricevo nulla
                    } else if (byteLetti == -1) {
                        throw new IOException("Il server ha chiuso la connessione.");
                    }
                }
            }
        }
    }
}

