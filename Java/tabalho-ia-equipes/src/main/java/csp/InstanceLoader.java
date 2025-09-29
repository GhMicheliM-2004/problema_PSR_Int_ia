package csp;

import constraints.*;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe utilitária para carregar uma instância de problema a partir de um arquivo JSON.
 * Versão final com criação inteligente de restrições binárias para C7.
 */
public class InstanceLoader {

    public static CSP loadInstanceFromFile(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        JSONObject json = new JSONObject(content);

        // 1. Carrega os dados base
        JSONObject jsonDomains = json.getJSONObject("jogadores");
        JSONObject jsonOverais = json.getJSONObject("overais");
        JSONObject jsonPosicoes = json.getJSONObject("posicoes");
        JSONObject jsonRestricoes = json.getJSONObject("restricoes");

        // 2. Cria as variáveis
        Map<String, Variable> variablesMap = new HashMap<>();
        for (String playerName : jsonDomains.keySet()) {
            List<Object> domain = jsonDomains.getJSONArray(playerName).toList();
            variablesMap.put(playerName, new Variable(playerName, domain));
        }
        List<Variable> variables = new ArrayList<>(variablesMap.values());
        List<Constraint> constraints = new ArrayList<>();

        // 3. Adiciona as restrições dinamicamente
        if (jsonRestricoes.has("C1") || jsonRestricoes.has("C4")) {
            constraints.add(new TeamBalanceConstraint(variables));
        }
        if (jsonRestricoes.has("C2")) {
            constraints.add(new NotSameTeamConstraint(variablesMap.get("J1"), variablesMap.get("J2")));
        }
        if (jsonRestricoes.has("C3")) {
            constraints.add(new SameTeamConstraint(variablesMap.get("J3"), variablesMap.get("J4")));
        }
        if (jsonRestricoes.has("C6")) {
            constraints.add(new ConditionalBalanceConstraint(variablesMap.get("J3"), variablesMap.get("J4"), variablesMap.get("J1")));
        }

        // Lógica para C7 que cria restrições binárias
        if (jsonRestricoes.has("C7")) {
            Map<String, String> posicoes = jsonToStringMap(jsonPosicoes);
            
            // Agrupa jogadores pela posição
            Map<String, List<String>> playersByPosition = new HashMap<>();
            for (Map.Entry<String, String> entry : posicoes.entrySet()) {
                playersByPosition.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
            }

            // Para cada posição que tem mais de um jogador, cria restrições binárias
            for (List<String> playersInSamePosition : playersByPosition.values()) {
                if (playersInSamePosition.size() > 1) {
                    for (int i = 0; i < playersInSamePosition.size(); i++) {
                        for (int j = i + 1; j < playersInSamePosition.size(); j++) {
                            Variable p1 = variablesMap.get(playersInSamePosition.get(i));
                            Variable p2 = variablesMap.get(playersInSamePosition.get(j));
                            constraints.add(new NotSameTeamConstraint(p1, p2));
                            // A linha de "INFO" foi removida daqui
                        }
                    }
                }
            }
        }
        
        if (jsonRestricoes.has("C8") && json.has("limite")) {
            double forcaMediaLimite = json.getJSONObject("limite").getDouble("numero");
            Map<String, Integer> overais = jsonToIntegerMap(jsonOverais);
            constraints.add(new AverageStrengthConstraint(variables, overais, forcaMediaLimite));
        }
        
        return new CSP(variables, constraints);
    }

    private static Map<String, String> jsonToStringMap(JSONObject jsonObj) {
        return jsonObj.keySet().stream()
                .collect(Collectors.toMap(key -> key, jsonObj::getString));
    }

    private static Map<String, Integer> jsonToIntegerMap(JSONObject jsonObj) {
        return jsonObj.keySet().stream()
                .collect(Collectors.toMap(key -> key, key -> Integer.parseInt(jsonObj.getString(key))));
    }
}