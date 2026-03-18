//Grazie a questa classe riesco a ricostruire la struttura del file JSON (Connections_Data)
//-> struttura : Partita[Gruppo]
//che devo appunto trasformare in una classe Java
import java.util.List;

public class Partita {
    private Integer gameId;
    private List<Gruppo> groups;

    public Partita() {}

    public Integer getGameId() { return gameId; }
    public List<Gruppo> getGroups() { return groups; }
}



//porzione del file Connections_Data che mi dà un'idea più chiara della struttura

// [                        //List<Partita>
//   {                                        Partita   (inizio)
//     "gameId": 0,
//     "groups": [
//       {
//         "theme": "WET WEATHER",
//         "words": [
//           "SNOW",
//           "HAIL",
//           "RAIN",
//           "SLEET"
//         ]
//       },
//       {
//         "theme": "NBA TEAMS",
//         "words": [
//           "HEAT",
//           "BUCKS",
//           "JAZZ",
//           "NETS"
//         ]
//       },
//       {
//         "theme": "KEYBOARD KEYS",
//         "words": [
//           "SHIFT",
//           "TAB",
//           "RETURN",
//           "OPTION"
//         ]
//       },
//       {
//         "theme": "PALINDROMES",
//         "words": [
//           "LEVEL",
//           "KAYAK",
//           "RACECAR",
//           "MOM"
//         ]
//       }
//     ]
//   },                     Partita (fine)