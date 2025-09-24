package csp;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * O motor de resolução do CSP. Implementa o algoritmo de backtracking
 * para encontrar uma solução.
 */
public class Solver {
    private int backtrackCount = 0;

    public Map<Variable, Object> solve(CSP csp, String heuristic) {
        this.backtrackCount = 0;
        // Inicia a busca recursiva com uma atribuição vazia.
        return backtrack(new HashMap<>(), csp, heuristic);
    }

    private Map<Variable, Object> backtrack(Map<Variable, Object> assignment, CSP csp, String heuristic) {
        // Se a atribuição está completa (todas as variáveis têm valor), encontramos uma solução.
        if (assignment.size() == csp.getVariables().size()) {
            return assignment;
        }

        // Seleciona a próxima variável a ser atribuída.
        Variable var = selectUnassignedVariable(assignment, csp, heuristic);

        // Itera sobre os valores do domínio da variável selecionada.
        for (Object value : orderDomainValues(var, assignment, csp, heuristic)) {
            // Tenta atribuir o valor à variável.
            assignment.put(var, value);

            // Verifica se a atribuição atual é consistente com as restrições.
            if (isConsistent(var, assignment, csp)) {
                Map<Variable, Object> result = backtrack(assignment, csp, heuristic);
                // Se a chamada recursiva encontrou uma solução, propaga o resultado.
                if (result != null) {
                    return result;
                }
            }
            
            // Se chegou aqui, a atribuição não levou a uma solução. Desfaz (backtrack).
            assignment.remove(var);
        }
        
        this.backtrackCount++; // Incrementa o contador de retrocessos.
        return null; // Nenhuma solução encontrada a partir deste ponto.
    }

    /**
     * Seleciona a próxima variável não atribuída para a busca.
     */
    private Variable selectUnassignedVariable(Map<Variable, Object> assignment, CSP csp, String heuristic) {
        List<Variable> unassigned = new ArrayList<>();
        for (Variable v : csp.getVariables()) {
            if (!assignment.containsKey(v)) {
                unassigned.add(v);
            }
        }

        if (heuristic.equalsIgnoreCase("MRV")) {
            // TODO: Implementar a heurística MRV (Minimum Remaining Values)
            // Encontrar a variável em 'unassigned' com o menor tamanho de domínio.
            // Exemplo:
            // unassigned.sort(Comparator.comparingInt(v -> v.getDomain().size()));
            // return unassigned.get(0);
        }
        
        // Estratégia padrão: retorna a primeira da lista.
        return unassigned.get(0);
    }

    /**
     * Ordena os valores do domínio da variável a ser tentada.
     */
    private List<Object> orderDomainValues(Variable var, Map<Variable, Object> assignment, CSP csp, String heuristic) {
        if (heuristic.equalsIgnoreCase("LCV")) {
            // TODO: Implementar a heurística LCV (Least Constraining Value)
            // Para cada valor no domínio de 'var', conte quantas escolhas ele elimina
            // para as variáveis vizinhas. Retorne os valores ordenados do menos
            // restritivo para o mais restritivo.
        }

        // Ordem padrão: a ordem original do domínio.
        return var.getDomain();
    }

    /**
     * Verifica se a atribuição de um valor a uma variável é consistente com todas as
     * restrições.
     */
    private boolean isConsistent(Variable var, Map<Variable, Object> assignment, CSP csp) {
        for (Constraint constraint : csp.getConstraints()) {
            // Só verifica restrições que incluem a variável recém-atribuída.
            if (constraint.getScope().contains(var)) {
                if (!constraint.isSatisfied(assignment)) {
                    return false; // Violação encontrada.
                }
            }
        }
        return true; // Nenhuma violação encontrada.
    }
    
    public int getBacktrackCount() {
        return this.backtrackCount;
    }

    // TODO: Implementar o algoritmo AC-3 aqui.
    public boolean ac3(CSP csp) {
        System.out.println("Aviso: Algoritmo AC-3 não implementado.");
        return true; // Placeholder
    }
}