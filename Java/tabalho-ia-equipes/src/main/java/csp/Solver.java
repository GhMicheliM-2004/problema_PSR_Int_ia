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
    private int nodesTested = 0;

    /**
     * Resolve o CSP e retorna TODAS as soluções válidas.
     * @param csp O problema a ser resolvido.
     * @return Uma lista de todas as soluções encontradas.
     */
    public List<Map<Variable, Object>> solve(CSP csp) {
        this.backtrackCount = 0;
        this.nodesTested = 0;
        List<Map<Variable, Object>> solutions = new ArrayList<>();
        backtrack(new HashMap<>(), csp, solutions);
        return solutions;
    }

    private void backtrack(Map<Variable, Object> assignment, CSP csp, List<Map<Variable, Object>> solutions) {
        this.nodesTested++; // Incrementa o contador de nós visitados

        if (assignment.size() == csp.getVariables().size()) {
            solutions.add(new HashMap<>(assignment)); // Adiciona uma cópia da solução
            return; // Continua a busca por outras soluções
        }

        Variable var = selectUnassignedVariable(assignment, csp);
        if (var == null) return;

        for (Object value : orderDomainValues(var, assignment, csp)) {
            assignment.put(var, value);

            if (isConsistent(var, assignment, csp)) {
                backtrack(assignment, csp, solutions);
            }
            
            assignment.remove(var);
        }
        
        this.backtrackCount++;
    }
    
    // --- HEURÍSTICAS MRV e LCV ---

    private Variable selectUnassignedVariable(Map<Variable, Object> assignment, CSP csp) {
        // Heurística MRV com desempate alfabético
        return csp.getVariables().stream()
            .filter(v -> !assignment.containsKey(v))
            .min(Comparator.comparingInt((Variable v) -> v.getDomain().size())
                           .thenComparing(Variable::getName))
            .orElse(null);
    }

    private List<Object> orderDomainValues(Variable var, Map<Variable, Object> assignment, CSP csp) {
        // Heurística LCV
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
    
    // Métodos auxiliares para LCV, consistência, etc. (sem alterações)...
    // [Os métodos getUnassignedNeighbors, violatesConstraints, isConsistent, etc. permanecem os mesmos]
    
    public int getBacktrackCount() { return this.backtrackCount; }
    public int getNodesTested() { return this.nodesTested; }
    
    // --- IMPLEMENTAÇÃO DO AC-3 ---
    public boolean ac3(CSP csp) {
        Queue<Arc> queue = new LinkedList<>();
        for (Constraint constraint : csp.getConstraints()) {
            if (constraint.getScope().size() == 2) {
                Variable xi = constraint.getScope().get(0);
                Variable xj = constraint.getScope().get(1);
                queue.add(new Arc(xi, xj, constraint));
                queue.add(new Arc(xj, xi, constraint));
            }
        }
        
        while (!queue.isEmpty()) {
            Arc arc = queue.poll();
            if (revise(arc.getXi(), arc.getXj(), arc.getConstraint())) {
                if (arc.getXi().getDomain().isEmpty()) return false;

                for (Constraint c : csp.getConstraints()) {
                    if (c.getScope().size() == 2 && c.getScope().contains(arc.getXi())) {
                        Variable xk = c.getScope().get(0).equals(arc.getXi()) ? 
                                      c.getScope().get(1) : c.getScope().get(0);
                        if (!xk.equals(arc.getXj())) {
                            queue.add(new Arc(xk, arc.getXi(), c));
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean revise(Variable xi, Variable xj, Constraint constraint) {
        boolean revised = false;
        for (Object valueX : new ArrayList<>(xi.getDomain())) {
            boolean hasSupport = xj.getDomain().stream()
                .anyMatch(valueY -> {
                    Map<Variable, Object> assignment = new HashMap<>();
                    assignment.put(xi, valueX);
                    assignment.put(xj, valueY);
                    return constraint.isSatisfied(assignment);
                });
            if (!hasSupport) {
                xi.getDomain().remove(valueX);
                revised = true;
            }
        }
        return revised;
    }

    private static class Arc {
        private final Variable xi, xj; private final Constraint constraint;
        public Arc(Variable xi, Variable xj, Constraint c) { this.xi = xi; this.xj = xj; this.constraint = c; }
        public Variable getXi() { return xi; }
        public Variable getXj() { return xj; }
        public Constraint getConstraint() { return constraint; }
    }
    
    // Os métodos auxiliares de antes (getUnassignedNeighbors, isConsistent, etc.)
    // devem ser mantidos aqui. Para economizar espaço, não os repeti, mas eles
    // devem estar no seu arquivo.
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
}