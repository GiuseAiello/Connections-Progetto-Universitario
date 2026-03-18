public class StatisticheGiocatore {
    private String username;
    private int punteggioTotale;

    public StatisticheGiocatore(String username, int punteggioTotale) {
        this.username = username;
        this.punteggioTotale = punteggioTotale;
    }

    public String getUsername() { return username; }
    public int getPunteggioTotale() { return punteggioTotale; }
}