package constraints;

import csp.Constraint;
import csp.Variable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Restrição C7: Jogadores com mesma posição não podem se concentrar no mesmo
 * time. Esta versão é genérica e impede que qualquer posição que se repita
 * na instância seja alocada no mesmo time.
 */
public class PositionConstraint implements Constraint {
    private final List<Variable> scope;
    private final Map<String, String> positions;
    private final List<String> positionsToEnforce;

    public PositionConstraint(List<Variable> scope, Map<String, String> positions) {
        this.scope = new ArrayList<>(scope);
        this.positions = positions;
        
        // Descobre automaticamente quais posições aparecem mais de uma vez
        // para aplicar a regra apenas a elas.
        this.positionsToEnforce = positions.values().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<Variable> getScope() {
        return scope;
    }

    @Override
    public boolean isSatisfied(Map<Variable, Object> assignment) {
        Map<Object, List<String>> teamPositions = new HashMap<>();
        teamPositions.put("T1", new ArrayList<>());
        teamPositions.put("T2", new ArrayList<>());

        for (Variable var : scope) {
            if (assignment.containsKey(var)) {
                Object team = assignment.get(var);
                String position = positions.get(var.getName());
                teamPositions.get(team).add(position);
            }
        }

        for (List<String> assignedPositions : teamPositions.values()) {
            Map<String, Long> counts = assignedPositions.stream()
                    .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
            
            // Regra genérica: para qualquer posição que deva ser fiscalizada (aparece > 1 vez),
            // a contagem no time não pode ser maior que 1.
            for (String position : positionsToEnforce) {
                if (counts.getOrDefault(position, 0L) > 1) {
                    return false;
                }
            }
        }
        return true;
    }
}