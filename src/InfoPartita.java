import java.util.List;
import java.util.Set;

public class InfoPartita {
    private long tempoRimanenteSecondi;
    private List<String> paroleRimaste;
    private int erroriFatti;
    private int punteggioCorrente;
    private Set<String> temiIndovinati;

    public InfoPartita(long tempoRimanenteSecondi, List<String> paroleRimaste, int erroriFatti, int punteggioCorrente, Set<String> temiIndovinati) {
        this.tempoRimanenteSecondi = tempoRimanenteSecondi;
        this.paroleRimaste = paroleRimaste;
        this.erroriFatti = erroriFatti;
        this.punteggioCorrente = punteggioCorrente;
        this.temiIndovinati = temiIndovinati;
    }

    public long getTempoRimanenteSecondi() { return tempoRimanenteSecondi; }
    public List<String> getParoleRimaste() { return paroleRimaste; }
    public int getErroriFatti() { return erroriFatti; }
    public int getPunteggioCorrente() { return punteggioCorrente; }
    public Set<String> getTemiIndovinati() { return temiIndovinati; }
}