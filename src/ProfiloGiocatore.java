//questa classe ci consente di mantenere lo storico del giocatore

import java.util.HashMap;
import java.util.Map;

public class ProfiloGiocatore {
    
    private int partiteGiocate;
    private int partiteVinte;
    private int punteggioTotale;
    private int streakAttuale;
    private int streakMassima;
    
    // Mappa per l'istogramma: Chiave = numero di errori (0, 1, 2, 3), Valore = quante volte ha vinto con quegli errori
    private Map<Integer, Integer> distribuzioneErrori;

    public ProfiloGiocatore() {
        this.partiteGiocate = 0;
        this.partiteVinte = 0;
        this.punteggioTotale = 0;
        this.streakAttuale = 0;
        this.streakMassima = 0;
        this.distribuzioneErrori = new HashMap<>();
        // Inizializziamo l'istogramma a zero per tutti i possibili errori vincenti
        for (int i = 0; i < 4; i++) {
            this.distribuzioneErrori.put(i, 0);
        }
    }

    // Il metodo che chiamiamo a fine partita 
    public synchronized void aggiornaStatistiche(int puntiOttenuti, int erroriFatti, boolean vinta) {
        this.partiteGiocate++;
        this.punteggioTotale += puntiOttenuti;

        if (vinta) {
            this.partiteVinte++;
            this.streakAttuale++;
            if (this.streakAttuale > this.streakMassima) {
                this.streakMassima = this.streakAttuale;
            }
            //l'istogramma viene aggiornato solo se vinciamo in quanto 
            //risulta così <numero errori, numero partite vinte con quegli errori> 
            // Aggiorniamo l'istogramma degli errori (ha vinto facendo X errori)
            int count = this.distribuzioneErrori.getOrDefault(erroriFatti, 0);
            //esempio : se è la prima volta che vinco una partita facendo 2 errori
            //nella mappa avrò (2,1), in quanto il metodo sopra restituisce 0 di default
            //se non trova la chiave  
            this.distribuzioneErrori.put(erroriFatti, count + 1);
        } else {
            // Se perde, la serie di vittorie consecutive si azzera
            this.streakAttuale = 0;
        }
    }

    // Getter utili per quando dovremo inviare i dati al client
    public int getPartiteGiocate() { return partiteGiocate; }
    public int getPartiteVinte() { return partiteVinte; }
    public int getPunteggioTotale() { return punteggioTotale; }
    public int getStreakAttuale() { return streakAttuale; }
    public int getStreakMassima() { return streakMassima; }
    public Map<Integer, Integer> getDistribuzioneErrori() { return distribuzioneErrori; }
    
    // Metodo extra per calcolare la media punti al volo
    public double getMediaPunti() {
        if (partiteGiocate == 0) return 0.0;
        return (double) punteggioTotale / partiteGiocate;
    }
}
