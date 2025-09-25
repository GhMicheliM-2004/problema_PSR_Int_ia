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
            // Altere aqui para testar: "facil.json", "medio.json", "dificil.json", etc.
            String instancePath = "src/main/resources/dificil.json";
            
            System.out.println("=========================================================");
            System.out.println("Carregando instância de: " + instancePath);
            System.out.println("=========================================================");
            
            CSP baseCsp = InstanceLoader.loadInstanceFromFile(instancePath);
            Solver solver = new Solver();
            
            // --- BLOCO 1: HEURÍSTICA PADRÃO ---
            System.out.println("\n--- 1. Heurística PADRÃO (Sem AC-3) ---");
            runAndMeasure(solver, baseCsp.clone(), "PADRAO", false);

            System.out.println("\n--- 2. Heurística PADRÃO (Com AC-3) ---");
            runAndMeasure(solver, baseCsp.clone(), "PADRAO", true);
            
            // --- BLOCO 2: HEURÍSTICA MRV+LCV ---
            System.out.println("\n--- 3. Heurística MRV+LCV (Sem AC-3) ---");
            runAndMeasure(solver, baseCsp.clone(), "MRV+LCV", false);
            
            System.out.println("\n--- 4. Heurística MRV+LCV (Com AC-3) ---");
            runAndMeasure(solver, baseCsp.clone(), "MRV+LCV", true);

        } catch (Exception e) {
            System.err.println("\nOcorreu um erro ao executar o programa:");
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
            boolean consistent = solver.ac3(csp);
            if (!consistent) {
                System.out.println("Inconsistência detectada pelo AC-3. Não há solução.");
                long endTime = System.nanoTime();
                long durationMs = (endTime - startTime) / 1_000_000;
                System.out.println("Tempo de execução: " + durationMs + " ms");
                System.out.println("Número de retrocessos (backtracks): 0");
                return;
            }
             System.out.println("AC-3 concluído. Domínios reduzidos.");
        }

        Map<Variable, Object> solution = solver.solve(csp, heuristic);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        System.out.println("Busca finalizada.");
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