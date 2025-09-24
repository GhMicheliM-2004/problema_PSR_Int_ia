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
 * time. A regra específica (ex: não mais de 1 'meia' ou 'volante') é
 * implementada aqui.
 */
public class PositionConstraint implements Constraint {
    private final List<Variable> scope;
    private final Map<String, String> positions;

    public PositionConstraint(List<Variable> scope, Map<String, String> positions) {
        this.scope = new ArrayList<>(scope);
        this.positions = positions;
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

            // Regra da instância 'dificil.json':
            // não pode ter 2+ volantes OU 2+ meias no mesmo time.
            if (counts.getOrDefault("volante", 0L) > 1 || counts.getOrDefault("meia", 0L) > 1) {
                return false;
            }
        }
        return true;
    }
}