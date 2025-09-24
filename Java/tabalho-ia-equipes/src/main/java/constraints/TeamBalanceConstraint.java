package constraints;

import csp.Constraint;
import csp.Variable;
import java.util.List;
import java.util.Map;

/**
 * Restrição C1 e C4: Garante o balanceamento dos times (3x2 para 5 jogadores)
 * e o mínimo de 2 jogadores por time.
 */
public class TeamBalanceConstraint implements Constraint {
    private final List<Variable> scope;

    public TeamBalanceConstraint(List<Variable> scope) {
        this.scope = scope;
    }

    @Override
    public List<Variable> getScope() {
        return scope;
    }

    @Override
    public boolean isSatisfied(Map<Variable, Object> assignment) {
        long countT1 = assignment.values().stream().filter(v -> v.equals("T1")).count();
        long countT2 = assignment.values().stream().filter(v -> v.equals("T2")).count();

        // Se a atribuição ainda não está completa, a restrição é violada
        // apenas se um time já excedeu o limite de 3 jogadores.
        if (countT1 > 3 || countT2 > 3) {
            return false;
        }

        // Se a atribuição está completa, verifica a regra final (3x2 e 2x3).
        if (assignment.size() == scope.size()) {
            boolean isBalanced = (countT1 == 3 && countT2 == 2) || (countT1 == 2 && countT2 == 3);
            return isBalanced;
        }

        // Se não estiver completa e não estourou os limites, continua válido.
        return true;
    }
}