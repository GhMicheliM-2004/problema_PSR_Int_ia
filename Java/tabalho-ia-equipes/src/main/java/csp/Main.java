package csp;

import java.util.List;
import java.util.Map;

/**
 * Classe principal para executar e comparar as abordagens de resolução do CSP.
 */
public class Main {
    public static void main(String[] args) {
        try {
            // Escolha a instância para testar
            String instancePath = "src/main/resources/dificil.json"; // Use a sua instância média aqui
            
            System.out.println("Rodando solver com heurísticas MRV e LCV...");
            
            // --- Execução SEM AC-3 (AGORA VEM PRIMEIRO) ---
            System.out.println("\n=== Resultados SEM AC-3 (pré) ===");
            CSP cspSemAc3 = InstanceLoader.loadInstanceFromFile(instancePath);
            runAndPrint(new Solver(), cspSemAc3, false);
            
            // --- Execução COM AC-3 (AGORA VEM DEPOIS) ---
            System.out.println("\n=== Resultados COM AC-3 (pré) ===");
            CSP cspComAc3 = InstanceLoader.loadInstanceFromFile(instancePath);
            runAndPrint(new Solver(), cspComAc3, true);

        } catch (Exception e) {
            System.err.println("\nOcorreu um erro ao executar o programa:");
            e.printStackTrace();
        }
    }

    /**
     * Executa o solver e imprime os resultados no formato especificado.
     */
    private static void runAndPrint(Solver solver, CSP csp, boolean useAC3) {
        long totalStartTime = System.nanoTime();
        long preProcTime = 0;
        
        if (useAC3) {
            double domainSizeBefore = calculateAverageDomainSize(csp);
            
            long preProcStartTime = System.nanoTime();
            boolean consistent = solver.ac3(csp);
            preProcTime = System.nanoTime() - preProcStartTime;

            double domainSizeAfter = calculateAverageDomainSize(csp);
            
            System.out.printf("Impacto do AC-3 nos domínios: Tamanho médio de %.2f -> %.2f\n",
                    domainSizeBefore, domainSizeAfter);
            
            if (!consistent) {
                long totalTime = System.nanoTime() - totalStartTime;
                System.out.printf("Tempo total: %.8f s | Pré-processamento: %.8f s\n", totalTime / 1e9, preProcTime / 1e9);
                System.out.println("Inconsistência detectada pelo AC-3. Nenhuma solução possível.");
                return;
            }
        }
        
        long searchStartTime = System.nanoTime();
        List<Map<Variable, Object>> solutions = solver.solve(csp);
        long searchTime = System.nanoTime() - searchStartTime;
        long totalTime = System.nanoTime() - totalStartTime;
        
        if (useAC3) {
            System.out.printf("Tempo total: %.8f s | Pré-processamento: %.8f s | Busca: %.8f s\n",
                    totalTime / 1e9, preProcTime / 1e9, searchTime / 1e9);
        } else {
            System.out.printf("Tempo total: %.8f s | Tempo busca: %.8f s\n",
                    totalTime / 1e9, searchTime / 1e9);
        }
        
        System.out.printf("Nós testados: %d | Retrocessos: %d\n",
                solver.getNodesTested(), solver.getBacktrackCount());
        System.out.printf("Soluções encontradas: %d\n", solutions.size());

        for (int i = 0; i < solutions.size(); i++) {
            String solutionHeader = useAC3 ? "--- Solução " + (i + 1) + " (COM AC-3) ---" : "--- Solução " + (i + 1) + " ---";
            System.out.println("\n" + solutionHeader);
            solutions.get(i).entrySet().stream()
                .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Variable::getName)))
                .forEach(entry -> System.out.println("  " + entry.getKey().getName() + ": " + entry.getValue()));
        }
    }

    private static double calculateAverageDomainSize(CSP csp) {
        return csp.getVariables().stream()
            .mapToInt(v -> v.getDomain().size())
            .average()
            .orElse(0.0);
    }
}