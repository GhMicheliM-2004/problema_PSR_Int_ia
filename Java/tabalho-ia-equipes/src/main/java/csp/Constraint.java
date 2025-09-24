package csp;

import java.util.List;
import java.util.Map;

/**
 * Interface que representa uma restrição em um Problema de Satisfação de
 * Restrições (PSR).
 * * Uma restrição define uma relação permitida entre um conjunto de variáveis,
 * sendo um dos componentes centrais de um modelo CSP.
 */
public interface Constraint {

    /**
     * Retorna a lista de variáveis envolvidas nesta restrição. Este conjunto
     * é também conhecido como o "escopo" da restrição.
     * * @return Uma lista de objetos Variable que a restrição abrange.
     */
    List<Variable> getScope();

    /**
     * Verifica se a restrição é satisfeita para uma dada atribuição de valores
     * às variáveis.
     * * Este método é o coração da lógica da restrição. Ele será chamado
     * repetidamente pelo solver de backtracking para verificar a consistência
     * de uma atribuição parcial ou completa.
     * * @param assignment Um mapa onde a chave é um objeto Variable e o valor é o
     * valor atualmente atribuído a ela (ex: "T1" ou "T2").
     * O mapa pode conter atribuições para variáveis que não
     * estão no escopo desta restrição, as quais devem ser ignoradas.
     * @return true se a atribuição atual não viola a restrição, false caso contrário.
     */
    boolean isSatisfied(Map<Variable, Object> assignment);
}