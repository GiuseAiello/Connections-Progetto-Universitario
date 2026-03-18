//questa classe contiene tutte le informazione su una partita passata

import java.util.List;

public class InfoPartitaConclusa {
    private List<Gruppo> soluzione; 
    private int proposteCorrette;
    private int erroriFatti;
    private int punteggioOttenuto;

    public InfoPartitaConclusa(List<Gruppo> soluzione, int proposteCorrette, int erroriFatti, int punteggioOttenuto) {
        this.soluzione = soluzione;
        this.proposteCorrette = proposteCorrette;
        this.erroriFatti = erroriFatti;
        this.punteggioOttenuto = punteggioOttenuto;
    }

    public List<Gruppo> getSoluzione() { return soluzione; }
    public int getProposteCorrette() { return proposteCorrette; }
    public int getErroriFatti() { return erroriFatti; }
    public int getPunteggioOttenuto() { return punteggioOttenuto; }
}