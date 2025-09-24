package csp;

import constraints.*; // Importa todas as suas classes de restrição
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe utilitária para carregar uma instância de problema a partir de um arquivo JSON.
 */
public class InstanceLoader {

    public static CSP loadInstanceFromFile(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        JSONObject json = new JSONObject(content);

        JSONObject jsonDomains = json.getJSONObject("jogadores");
        JSONObject jsonOverais = json.getJSONObject("overais");
        JSONObject jsonPosicoes = json.getJSONObject("posicoes");
        double forcaMediaLimite = json.getJSONObject("limite").getDouble("forca media");

        Map<String, Variable> variablesMap = new HashMap<>();
        for (String playerName : jsonDomains.keySet()) {
            List<Object> domain = jsonDomains.getJSONArray(playerName).toList();
            variablesMap.put(playerName, new Variable(playerName, domain));
        }

        // C5: J5 não pode ficar no Time 2 (pré-processamento do domínio)
        if (variablesMap.containsKey("J5")) {
            variablesMap.get("J5").setDomain(List.of("T1"));
        }

        List<Variable> variables = new ArrayList<>(variablesMap.values());
        List<Constraint> constraints = new ArrayList<>();

        // Adicionando restrições fixas
        constraints.add(new NotSameTeamConstraint(variablesMap.get("J1"), variablesMap.get("J2"))); // C2
        constraints.add(new SameTeamConstraint(variablesMap.get("J3"), variablesMap.get("J4")));    // C3
        constraints.add(new ConditionalBalanceConstraint(variablesMap.get("J3"), variablesMap.get("J4"), variablesMap.get("J1"))); // C6

        // Adicionando restrições globais e baseadas em dados
        constraints.add(new TeamBalanceConstraint(variables)); // C1 e C4

        Map<String, String> posicoes = jsonToStringMap(jsonPosicoes);
        constraints.add(new PositionConstraint(variables, posicoes)); // C7

        Map<String, Integer> overais = jsonToIntegerMap(jsonOverais);
        constraints.add(new AverageStrengthConstraint(variables, overais, forcaMediaLimite)); // C8
        
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