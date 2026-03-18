import java.util.List;

public class RispostaServer {
    private boolean esito; 
    private String risposta; 
    private Integer codice; 
    
    // I vari "pacchetti" di dati che il server può inviare
    private EsitoMossa datiMossa;
    private InfoPartita infoPartita;
    private InfoPartitaConclusa infoPartitaConclusa;
    private ProfiloGiocatore profiloGiocatore;
    private List<StatisticheGiocatore> classifica; 
    private StatisticheGlobaliPartita statPartita;

    // --- COSTRUTTORI ---

    // 1. Per risposte testuali semplici (Registrazione, Login, Logout, Errori vari)
    public RispostaServer(boolean esito, String risposta, Integer codice) {
        this.esito = esito; 
        this.risposta = risposta; 
        this.codice = codice;
    }

    // 2. Per l'esito di una mossa (submitProposal)
    public RispostaServer(boolean esito, Integer codice, EsitoMossa datiMossa) {
        this.esito = esito; 
        this.codice = codice;
        this.datiMossa = datiMossa;
    }

    // 3. Per le informazioni della partita IN CORSO (requestGameInfo)
    public RispostaServer(boolean esito, Integer codice, InfoPartita infoPartita) {
        this.esito = esito;
        this.codice = codice;
        this.infoPartita = infoPartita;
    }

    // 4. Per le informazioni di una partita CONCLUSA (requestGameInfo con gameId)
    public RispostaServer(boolean esito, Integer codice, InfoPartitaConclusa infoPartitaConclusa) {
        this.esito = esito;
        this.codice = codice;
        this.infoPartitaConclusa = infoPartitaConclusa;
    }

    // 5. Per le statistiche personali (requestPlayerStats)
    public RispostaServer(boolean esito, Integer codice, ProfiloGiocatore profiloGiocatore) {
        this.esito = esito;
        this.codice = codice;
        this.profiloGiocatore = profiloGiocatore;
    }

    // 6. Per la classifica generale (requestLeaderboard)
    public RispostaServer(boolean esito, Integer codice, List<StatisticheGiocatore> classifica) {
        this.esito = esito;
        this.codice = codice;
        this.classifica = classifica;
    }

    // 7. Per le statistiche globali di una singola partita (requestGameStats)
    public RispostaServer(boolean esito, Integer codice, StatisticheGlobaliPartita statPartita) {
        this.esito = esito;
        this.codice = codice;
        this.statPartita = statPartita;
    }

    // --- GETTER ---

    public boolean getEsito(){ return this.esito; }
    public String getRisposta(){ return this.risposta; }
    public Integer getCodice(){ return this.codice; }
    
    public EsitoMossa getDatiMossa() { return this.datiMossa; }
    public InfoPartita getInfoPartita() { return this.infoPartita; }
    public InfoPartitaConclusa getInfoPartitaConclusa(){ return this.infoPartitaConclusa; }
    public ProfiloGiocatore getProfiloGiocatore() { return this.profiloGiocatore; }
    public List<StatisticheGiocatore> getClassifica(){ return this.classifica; }
    public StatisticheGlobaliPartita getStatPartita() { return this.statPartita; }
}