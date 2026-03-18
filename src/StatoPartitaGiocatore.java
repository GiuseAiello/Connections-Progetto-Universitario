import java.util.HashSet;
import java.util.Set;

public class StatoPartitaGiocatore {
    
    private int punti; 
    private int errori; 
    private Set<String> temiIndovinati;

    public StatoPartitaGiocatore() {
        this.punti = 0; 
        this.errori = 0; 
        this.temiIndovinati = new HashSet<>();
    }

    public int getPunti() {
        return this.punti; 
    }

    public int getErrori() {
        return this.errori; 
    }

    public Set<String> getTemiIndovinati() {
        return this.temiIndovinati;
    }
    
    //  Passiamo i punti esatti da aggiungere (+6, +12 o +18)
    public void aggiungiPunti() {
        this.punti += 6; 
    }

    // Aggiunge l'errore e scala in automatico i 4 punti di penalità
    public void aggiungiErrore() {
        this.errori++; 
        this.punti -= 4; // -4 per ogni errore 
    }

    // Metodo per salvare il gruppo indovinato
    public void aggiungiTemaIndovinato(String tema) {
        this.temiIndovinati.add(tema);
    }
}