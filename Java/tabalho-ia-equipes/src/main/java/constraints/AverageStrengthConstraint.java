package constraints;

import csp.Constraint;
import csp.Variable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Restrição C8: A força média de cada time (soma dos overais / N de jogadores)
 * não deve ultrapassar um limite.
 */
public class AverageStrengthConstraint implements Constraint {
    private final List<Variable> scope;
    private final Map<String, Integer> overais;
    private final double limit;

    public AverageStrengthConstraint(List<Variable> scope, Map<String, Integer> overais, double limit) {
        this.scope = new ArrayList<>(scope);
        this.overais = overais;
        this.limit = limit;
    }

    @Override
    public List<Variable> getScope() {
        return scope;
    }

    @Override
    public boolean isSatisfied(Map<Variable, Object> assignment) {
        // Esta restrição só pode ser totalmente validada quando a atribuição está completa.
        // Uma verificação parcial poderia ser feita, mas aumenta a complexidade.
        // Para o backtracking padrão, é mais simples verificar no final.
        if (assignment.size() != scope.size()) {
            return true;
        }

        Map<Object, List<Integer>> teamOverais = new HashMap<>();
        teamOverais.put("T1", new ArrayList<>());
        teamOverais.put("T2", new ArrayList<>());

        for (Variable var : scope) {
            Object team = assignment.get(var);
            int overall = overais.get(var.getName());
            teamOverais.get(team).add(overall);
        }

        for (List<Integer> teamStats : teamOverais.values()) {
            if (teamStats.isEmpty()) {
                continue;
            }

            double sum = teamStats.stream().mapToInt(Integer::intValue).sum();
            double average = sum / teamStats.size();

            if (average > limit) {
                return false;
            }
        }
        return true;
    }
}