package csp;

import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Queue;

/**
 * O motor de resolução do CSP. Implementa o algoritmo de backtracking
 * e o pré-processamento AC-3.
 */
public class Solver {
    private int backtrackCount = 0;

    public Map<Variable, Object> solve(CSP csp, String heuristic) {
        this.backtrackCount = 0;
        return backtrack(new HashMap<>(), csp, heuristic);
    }

    private Map<Variable, Object> backtrack(Map<Variable, Object> assignment, CSP csp, String heuristic) {
        if (assignment.size() == csp.getVariables().size()) {
            return assignment;
        }

        Variable var = selectUnassignedVariable(assignment, csp, heuristic);
        if (var == null) return null; // Caso não haja mais variáveis

        for (Object value : orderDomainValues(var, assignment, csp, heuristic)) {
            assignment.put(var, value);

            if (isConsistent(var, assignment, csp)) {
                Map<Variable, Object> result = backtrack(assignment, csp, heuristic);
                if (result != null) {
                    return result;
                }
            }
            
            assignment.remove(var);
        }
        
        this.backtrackCount++;
        return null;
    }
    
    // --- IMPLEMENTAÇÃO DO AC-3 ---

    /**
     * Algoritmo de consistência de arco (AC-3). Reduz os domínios das variáveis
     * removendo valores que não podem fazer parte de uma solução.
     * @param csp O problema a ser pré-processado.
     * @return false se uma inconsistência for encontrada (domínio vazio), true caso contrário.
     */
    public boolean ac3(CSP csp) {
        Queue<Arc> queue = new LinkedList<>();

        // 1. Inicializa a fila com todos os arcos do problema
        for (Constraint constraint : csp.getConstraints()) {
            // AC-3 é mais simples com restrições binárias, mas pode ser adaptado.
            // Aqui, vamos focar nas restrições que envolvem 2 variáveis.
            if (constraint.getScope().size() == 2) {
                Variable xi = constraint.getScope().get(0);
                Variable xj = constraint.getScope().get(1);
                queue.add(new Arc(xi, xj, constraint));
                queue.add(new Arc(xj, xi, constraint));
            }
        }
        
        // 2. Processa a fila
        while (!queue.isEmpty()) {
            Arc arc = queue.poll();
            Variable xi = arc.getXi();
            Variable xj = arc.getXj();

            // 3. Revisa o domínio de Xi
            if (revise(xi, xj, arc.getConstraint())) {
                // 4. Se o domínio de Xi ficou vazio, não há solução
                if (xi.getDomain().isEmpty()) {
                    return false;
                }

                // 5. Adiciona os arcos vizinhos de Xi de volta à fila
                for (Constraint constraint : csp.getConstraints()) {
                    if (constraint.getScope().size() == 2 && constraint.getScope().contains(xi)) {
                        Variable xk = constraint.getScope().get(0).equals(xi) ? 
                                      constraint.getScope().get(1) : 
                                      constraint.getScope().get(0);
                        if (!xk.equals(xj)) {
                            queue.add(new Arc(xk, xi, constraint));
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Função auxiliar do AC-3. Remove valores do domínio de Xi para os quais
     * não há um valor correspondente em Xj que satisfaça a restrição.
     * @return true se o domínio de Xi foi modificado, false caso contrário.
     */
    private boolean revise(Variable xi, Variable xj, Constraint constraint) {
        boolean revised = false;
        List<Object> xiDomainCopy = new ArrayList<>(xi.getDomain());

        for (Object valueX : xiDomainCopy) {
            boolean hasSupport = false;
            for (Object valueY : xj.getDomain()) {
                Map<Variable, Object> assignment = new HashMap<>();
                assignment.put(xi, valueX);
                assignment.put(xj, valueY);
                if (constraint.isSatisfied(assignment)) {
                    hasSupport = true;
                    break;
                }
            }
            if (!hasSupport) {
                xi.getDomain().remove(valueX);
                revised = true;
            }
        }
        return revised;
    }

    // Classe interna para representar um arco
    private static class Arc {
        private final Variable xi, xj;
        private final Constraint constraint;
        public Arc(Variable xi, Variable xj, Constraint c) { this.xi = xi; this.xj = xj; this.constraint = c; }
        public Variable getXi() { return xi; }
        public Variable getXj() { return xj; }
        public Constraint getConstraint() { return constraint; }
    }
    
    // --- HEURÍSTICAS MRV e LCV ---

    private Variable selectUnassignedVariable(Map<Variable, Object> assignment, CSP csp, String heuristic) {
        List<Variable> unassigned = csp.getVariables().stream()
            .filter(v -> !assignment.containsKey(v))
            .toList();

        if (heuristic.equalsIgnoreCase("MRV") || heuristic.equalsIgnoreCase("MRV+LCV")) {
            return unassigned.stream()
                .min(Comparator.comparingInt(v -> v.getDomain().size()))
                .orElse(null);
        }
        
        return unassigned.isEmpty() ? null : unassigned.get(0);
    }

    private List<Object> orderDomainValues(Variable var, Map<Variable, Object> assignment, CSP csp, String heuristic) {
        if (!heuristic.equalsIgnoreCase("MRV+LCV")) {
            return var.getDomain();
        }

        Map<Object, Integer> valueCosts = new HashMap<>();
        for (Object value : var.getDomain()) {
            int eliminatedChoices = 0;
            List<Variable> neighbors = getUnassignedNeighbors(var, assignment, csp);
            
            for (Variable neighbor : neighbors) {
                for (Object neighborValue : neighbor.getDomain()) {
                    if (violatesConstraints(var, value, neighbor, neighborValue, csp, assignment)) {
                        eliminatedChoices++;
                    }
                }
            }
            valueCosts.put(value, eliminatedChoices);
        }

        List<Object> sortedDomain = new ArrayList<>(var.getDomain());
        sortedDomain.sort(Comparator.comparingInt(valueCosts::get));
        return sortedDomain;
    }
    
    // --- MÉTODOS AUXILIARES E DE CONSISTÊNCIA ---
    
    private List<Variable> getUnassignedNeighbors(Variable var, Map<Variable, Object> assignment, CSP csp) {
        List<Variable> neighbors = new ArrayList<>();
        for (Constraint constraint : csp.getConstraints()) {
            if (constraint.getScope().contains(var)) {
                for (Variable scopeVar : constraint.getScope()) {
                    if (!scopeVar.equals(var) && !assignment.containsKey(scopeVar) && !neighbors.contains(scopeVar)) {
                        neighbors.add(scopeVar);
                    }
                }
            }
        }
        return neighbors;
    }

    private boolean violatesConstraints(Variable v1, Object val1, Variable v2, Object val2, CSP csp, Map<Variable, Object> baseAssignment) {
        Map<Variable, Object> temp = new HashMap<>(baseAssignment);
        temp.put(v1, val1);
        temp.put(v2, val2);

        for (Constraint c : csp.getConstraints()) {
            if (c.getScope().contains(v1) && c.getScope().contains(v2)) {
                if (!c.isSatisfied(temp)) return true;
            }
        }
        return false;
    }

    private boolean isConsistent(Variable var, Map<Variable, Object> assignment, CSP csp) {
        for (Constraint constraint : csp.getConstraints()) {
            if (constraint.getScope().contains(var)) {
                if (!constraint.isSatisfied(assignment)) return false;
            }
        }
        return true;
    }
    
    public int getBacktrackCount() {
        return this.backtrackCount;
    }
}