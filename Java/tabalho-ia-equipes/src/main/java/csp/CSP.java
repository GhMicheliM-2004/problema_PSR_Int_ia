package csp;

import java.util.List;

/**
 * Representa um Problema de Satisfação de Restrições (PSR) completo.
 * Contém o conjunto de variáveis e o conjunto de restrições que definem o problema.
 */
public class CSP {
    private final List<Variable> variables;
    private final List<Constraint> constraints;

    public CSP(List<Variable> variables, List<Constraint> constraints) {
        this.variables = variables;
        this.constraints = constraints;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }
}