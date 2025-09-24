package csp;

import java.util.Map;

/**
 * Classe principal para executar o resolvedor de CSP.
 * Carrega uma instância, executa o solver com diferentes configurações
 * e imprime os resultados para comparação.
 */
public class Main {
    public static void main(String[] args) {
        try {
            // Altere o caminho para o seu arquivo de instância JSON.
            // Coloque-o na pasta src/main/resources do seu projeto Maven.
            String instancePath = "src/main/resources/dificil.json";
            
            System.out.println("Carregando instância de: " + instancePath);
            CSP baseCsp = InstanceLoader.loadInstanceFromFile(instancePath);
            System.out.println("Instância carregada com sucesso.");

            Solver solver = new Solver();
            
            System.out.println("\n--- Executando com Heurística PADRÃO (sem AC-3) ---");
            runAndMeasure(solver, baseCsp.clone(), "PADRAO", false);

            System.out.println("\n--- Executando com Heurística PADRÃO (com AC-3) ---");
            runAndMeasure(solver, baseCsp.clone(), "PADRAO", true);

            // Adicione aqui as chamadas para suas outras heurísticas (MRV, LCV, etc.)

        } catch (Exception e) {
            System.err.println("Ocorreu um erro ao executar o programa:");
            e.printStackTrace();
        }
    }

    /**
     * Executa o solver para um dado CSP e configuração, medindo e imprimindo os resultados.
     */
    private static void runAndMeasure(Solver solver, CSP csp, String heuristic, boolean useAC3) {
        long startTime = System.nanoTime();

        if (useAC3) {
            System.out.println("Aplicando pré-processamento AC-3...");
            solver.ac3(csp);
        }

        Map<Variable, Object> solution = solver.solve(csp, heuristic);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        System.out.println("Execução finalizada.");
        if (solution != null) {
            System.out.println("Solução encontrada: ");
            solution.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Variable::getName)))
                .forEach(entry -> System.out.println("  " + entry.getKey().getName() + " -> " + entry.getValue()));
        } else {
            System.out.println("Nenhuma solução foi encontrada.");
        }
        System.out.println("Tempo de execução: " + durationMs + " ms");
        System.out.println("Número de retrocessos (backtracks): " + solver.getBacktrackCount());
    }
}