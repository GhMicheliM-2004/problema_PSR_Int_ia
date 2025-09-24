"""
Solver para o Tópico 7 (5 jogadores) usando:
- MRV (Minimum Remaining Values) para selecionar variável
- LCV (Least Constraining Value) para ordenar valores
- AC-3 (pré-processamento), forward checking e checagens parciais
Entrada: dicionário 'arquivo' no formato JSON mostrado pelo usuário.
"""

import json
import time
# import copy 
# from collections import defaultdict

with open('facil.json', 'r') as arquivo:
    dados = json.load(arquivo)
    print(f"\n_________ Verificando o JSON: {arquivo.name} __________\n\nJSON:\n")
    for item, overall in dados["overais"].items():
      print(f"Jogador: {item} | Overall: {overall}")
    for item, time in dados["jogadores"].items():
      print(f"Jogador: {item} | Time: {time}")
    for item, posicao in dados["posicoes"].items():
      print(f"Jogador: {item} | Posição: {posicao}")

#inicio = time.time()

# nos_explorados = 0
# retrocessos = 0

def verifica_restricoes(arquivo):
    t1 = [j for j, t in arquivo["jogadores"].items() if t == 'T1'] # Todos os jogadores do T1
    t2 = [j for j, t in arquivo["jogadores"].items() if t == 'T2'] # Todos os jogadores do T2
   
    # C4: Restrição de no máximo 5 jogadores e no mínimo 2
    if len(arquivo["jogadores"]) < 5 and len(arquivo["jogadores"]) >= 2:
        return True

    # C2: J1 ≠ J2
    if arquivo['J1'].items() != arquivo['J2'].items():
      return True

    # C4: Mínimo 2 jogadores por time
    if len(t1) <= 2 and len(t2) <= 2:
      return True

#     # C1: Balanceamento ±1
#     if abs(len(t1) - len(t2)) > 1:
#         return False

#     # C3: J3 = J4
#     if 'J3' in assignment and 'J4' in assignment:
#         if assignment['J3'] != assignment['J4']:
#             return False



#     # C5: J5 ≠ T2
#     if 'J5' in assignment and assignment['J5'] == 'T2':
#         return False

#     # C6: Se J3 = J4 = T1 → J1 = T2
#     if all(j in assignment for j in ['J1', 'J3', 'J4']):
#         if assignment['J3'] == assignment['J4'] == 'T1' and assignment['J1'] != 'T2':
#             return False

#     # C7: Mesma posição não pode concentrar-se no mesmo time (>2)
#     for time in ['T1', 'T2']:
#         pos_count = defaultdict(int)
#         for j, t in assignment.items():
#             if t == time:
#                 pos_count[dados["posicoes"][j]] += 1
#         if any(v > 2 for v in pos_count.values()):
#             return False

#     # C8: Média de força ≤ 8.5 por time
#     for time in ['T1', 'T2']:
#         jogadores = [j for j, t in assignment.items() if t == time]
#         media = sum(dados["overais"][j] for j in jogadores) / len(jogadores)
#         if media > 8.5:
#             return False

#     return True

# def ac3(dom):
#     queue = [('J1', 'J2'), ('J3', 'J4')]
#     while queue:
#         xi, xj = queue.pop(0)
#         revised = False
#         for x in dom[xi][:]:
#             if not any(verifica({xi: x, xj: y}) for y in dom[xj]):
#                 dom[xi].remove(x)
#                 revised = True
#         if revised:
#             for xk in dom:
#                 if xk != xi:
#                     queue.append((xk, xi))
#     return dom

# # Algoritmo de Backtracking com métricas
# def backtrack(dom, assignment={}):
#     global nos_explorados, retrocessos
#     if len(assignment) == len(dom):
#         return assignment
#     var = mrv(dom)[0][0]  # Escolhe a variável com menor domínio
#     for val in lcv(var, dom, assignment):
#         nos_explorados += 1
#         assignment[var] = val
#         if verifica(assignment):
#             result = backtrack(dom, assignment)
#             if result:
#                 return result
#         del assignment[var]
#         retrocessos += 1
#     return None

# # Heurística MRV
# def mrv(dom):
#     return sorted(dom.items(), key=lambda item: len(item[1]))

# # Heurística LCV
# def lcv(var, dom, assignment):
#     def impacto(valor):
#         conflitos = 0
#         for outro in dom:
#             if outro != var and outro not in assignment:
#                 for v_outro in dom[outro]:
#                     simulado = assignment.copy()
#                     simulado[var] = valor
#                     simulado[outro] = v_outro
#                     # Só verifica se já temos pelo menos 2 jogadores simulados
#                     if len(simulado) >= 2 and not verifica(simulado):
#                         conflitos += 1
#         return conflitos
#     return sorted(dom[var], key=impacto)

# # Execução principal
# start = time.time()
# dom_reduzido = ac3(copy.deepcopy(dados["jogadores"]))
# solucao = backtrack(dom_reduzido)
# end = time.time()

# # Saída formatada com status
# print("\n===== RESULTADO =====")
# if solucao:
#     print("Solução encontrada:")
#     for jogador, time in solucao.items():
#         print(f"{jogador} → {time}")
#     print("\nStatus: ✅ Solução válida encontrada")
# else:
#     print("Nenhuma solução possível.")
#     print("\nStatus: ❌ Instância inconsistente — nenhuma atribuição satisfaz todas as restrições")

# print(f"\nTempo de execução: {round(end - start, 4)} segundos")
# print(f"Nós explorados: {nos_explorados}")
# print(f"Retrocessos: {retrocessos}")
# print("======================\n")