public class StatisticheGlobaliPartita {
    private int giocatoriPartecipanti;
    private int giocatoriVincitori;
    private int giocatoriSconfitti; // Quelli che hanno raggiunto i 4 errori

    public StatisticheGlobaliPartita(int giocatoriPartecipanti, int giocatoriVincitori, int giocatoriSconfitti) {
        this.giocatoriPartecipanti = giocatoriPartecipanti;
        this.giocatoriVincitori = giocatoriVincitori;
        this.giocatoriSconfitti = giocatoriSconfitti;
    }

    public int getGiocatoriPartecipanti() { return giocatoriPartecipanti; }
    public int getGiocatoriVincitori() { return giocatoriVincitori; }
    public int getGiocatoriSconfitti() { return giocatoriSconfitti; }
}