package csp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa uma variável em um Problema de Satisfação de Restrições.
 * Cada variável tem um nome, um domínio de possíveis valores e, eventualmente,
 * um valor atribuído durante a busca por uma solução.
 */
public class Variable {
    private final String name;
    private List<Object> domain;
    private Object assignedValue;

    public Variable(String name, List<Object> domain) {
        this.name = name;
        this.domain = new ArrayList<>(domain); // Cria uma cópia para poder modificar
        this.assignedValue = null;
    }

    public String getName() {
        return name;
    }

    public List<Object> getDomain() {
        return domain;
    }

    public void setDomain(List<Object> domain) {
        this.domain = new ArrayList<>(domain);
    }
    
    public Object getAssignedValue() {
        return assignedValue;
    }

    public void setAssignedValue(Object value) {
        this.assignedValue = value;
    }
    
    public boolean isAssigned() {
        return this.assignedValue != null;
    }

    @Override
    public String toString() {
        return name; // Facilita a impressão de soluções
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return name.equals(variable.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}