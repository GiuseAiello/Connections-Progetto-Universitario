import java.util.List;

public class MessaggioClient {
    // 1. L'operation che ci consente (in GestoreCliente) di capire che tipo di operazione 
    //vuole svolgere il client e quindi di rispondergli di conseguenza
    private String operation; 
    
    private String username; 
    private String psw; 
    
    private String oldUsername; 
    private String newUsername;
    private String oldPsw; 
    private String newPsw; 
    
    private List<String> words;
    
    // 2. Usiamo Integer invece di int, così se mancano diventano "null" e non "0"
    //
    private Integer gameId;
    private String playerName; 
    private Integer topPlayers; 

    private Integer UdpPort; 

    // Costruttore vuoto per Gson
    public MessaggioClient() {}

    // I vari getter che GestoreClient utilizzerà
    
    
    public String getOperation() { return operation; }
    
    public String getUsername() { return username; }
    public String getPsw() { return psw; }
    
    public String getOldUsername() { return oldUsername; }
    public String getNewUsername() { return newUsername; }
    public String getOldPsw() { return oldPsw; }
    public String getNewPsw() { return newPsw; }
    
    public List<String> getWords() { return words; }
    
    public Integer getGameId() { return gameId; }
    public String getPlayerName() { return playerName; }
    public Integer getTopPlayers() { return topPlayers; }
    public Integer getUdpPort(){ return UdpPort; }

    // --- SETTER ---
    public void setOperation(String operation) { this.operation = operation; }
    
    public void setUsername(String username) { this.username = username; }
    public void setPsw(String psw) { this.psw = psw; }
    
    public void setOldUsername(String oldUsername) { this.oldUsername = oldUsername; }
    public void setNewUsername(String newUsername) { this.newUsername = newUsername; }
    public void setOldPsw(String oldPsw) { this.oldPsw = oldPsw; }
    public void setNewPsw(String newPsw) { this.newPsw = newPsw; }
    
    public void setWords(List<String> words) { this.words = words; }
    
    public void setGameId(Integer gameId) { this.gameId = gameId; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public void setTopPlayers(Integer topPlayers) { this.topPlayers = topPlayers; }
    public void setUdpPort(Integer UdpPort) { this.UdpPort = UdpPort; }
}
