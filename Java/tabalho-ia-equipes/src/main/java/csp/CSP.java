package csp;

import java.util.List;
import java.util.ArrayList;

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

    /**
     * Cria um clone profundo do CSP. Essencial para executar o solver várias
     * vezes (com e sem AC-3) sem que uma execução afete a outra.
     */
    @Override
    public CSP clone() {
        // A clonagem aqui é superficial, mas para o nosso caso é suficiente
        // porque não modificamos as restrições, apenas os domínios das variáveis.
        // Uma implementação mais robusta faria um clone profundo.
        List<Variable> clonedVariables = new ArrayList<>();
        for (Variable v : this.variables) {
            clonedVariables.add(new Variable(v.getName(), new ArrayList<>(v.getDomain())));
        }
        
        // As restrições podem ser reutilizadas, pois são imutáveis.
        List<Constraint> clonedConstraints = new ArrayList<>(this.constraints);
        
        return new CSP(clonedVariables, clonedConstraints);
    }
}