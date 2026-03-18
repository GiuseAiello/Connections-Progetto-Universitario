public class EsitoMossa {
    private int statusCode;      // 200 (ok), 400 (errore client), 401 (non loggato)
    private String messaggio;    // Es. "Hai indovinato!", "Hai sbagliato!", "HAI VINTO!"
    private int puntiAttuali;    // I punti aggiornati del giocatore
    private int erroriFatti;     // Quanti errori ha commesso finora (max 4)
    private boolean partitaFinita; // true se ha vinto o perso, altrimenti false

    public EsitoMossa(int statusCode, String messaggio, int puntiAttuali, int erroriFatti, boolean partitaFinita) {
        this.statusCode = statusCode;
        this.messaggio = messaggio;
        this.puntiAttuali = puntiAttuali;
        this.erroriFatti = erroriFatti;
        this.partitaFinita = partitaFinita;
    }

    // Getter
    public int getStatusCode() { return statusCode; }
    public String getMessaggio() { return messaggio; }
    public int getPuntiAttuali() { return puntiAttuali; }
    public int getErroriFatti() { return erroriFatti; }
    public boolean isPartitaFinita() { return partitaFinita; }

    
}