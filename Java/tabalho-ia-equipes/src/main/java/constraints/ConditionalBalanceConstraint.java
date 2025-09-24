package constraints;

import csp.Constraint;
import csp.Variable;
import java.util.List;
import java.util.Map;

/**
 * Restrição C6: Se J3 e J4 estão no T1, então J1 deve estar no T2.
 */
public class ConditionalBalanceConstraint implements Constraint {
    private final Variable j3, j4, j1;

    public ConditionalBalanceConstraint(Variable j3, Variable j4, Variable j1) {
        this.j3 = j3;
        this.j4 = j4;
        this.j1 = j1;
    }

    @Override
    public List<Variable> getScope() {
        return List.of(j3, j4, j1);
    }

    @Override
    public boolean isSatisfied(Map<Variable, Object> assignment) {
        Object valJ3 = assignment.get(j3);
        Object valJ4 = assignment.get(j4);
        Object valJ1 = assignment.get(j1);

        // Se a condição (J3 e J4 em T1) não é totalmente conhecida ou não ocorre,
        // a restrição não é violada.
        if (valJ3 == null || valJ4 == null || !valJ3.equals("T1") || !valJ4.equals("T1")) {
            return true;
        }

        // Se a condição ocorre, J1 deve estar atribuído e ser T2.
        return valJ1 != null && valJ1.equals("T2");
    }
}