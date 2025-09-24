package constraints;

import csp.Constraint;
import csp.Variable;
import java.util.List;
import java.util.Map;

/**
 * Restrição C3: Garante que duas variáveis (jogadores) sejam atribuídas
 * ao mesmo valor (time).
 */
public class SameTeamConstraint implements Constraint {
    private final Variable var1;
    private final Variable var2;

    public SameTeamConstraint(Variable var1, Variable var2) {
        this.var1 = var1;
        this.var2 = var2;
    }

    @Override
    public List<Variable> getScope() {
        return List.of(var1, var2);
    }

    @Override
    public boolean isSatisfied(Map<Variable, Object> assignment) {
        Object value1 = assignment.get(var1);
        Object value2 = assignment.get(var2);

        // Se uma das variáveis não foi atribuída, a restrição não foi violada.
        if (value1 == null || value2 == null) {
            return true;
        }

        // A restrição é satisfeita se os valores (times) forem iguais.
        return value1.equals(value2);
    }
}