# solver_sem_ac3_pre.py
# Sem AC-3 como pré-processamento.

import json
import time
import sys
from collections import defaultdict

# Construir restrições binárias (C2, C3, C7) - mesma assinatura do seu antigo
def construir_restricoes_binarias(vars_list, posicoes):
    restricoes = {}
    vizinhos = {v: set() for v in vars_list}
    def add(x, y, fn):
        restricoes[(x, y)] = fn
        vizinhos[x].add(y)

    # C2: J1 != J2 (se existirem)
    if "J1" in vars_list and "J2" in vars_list:
        add("J1", "J2", lambda a, b: a != b)
        add("J2", "J1", lambda a, b: a != b)

    # C3: J3 == J4
    if "J3" in vars_list and "J4" in vars_list:
        add("J3", "J4", lambda a, b: a == b)
        add("J4", "J3", lambda a, b: a == b)

    # C7: mesma posição -> não no mesmo time (par-a-par)
    for i in range(len(vars_list)):
        for j in range(i+1, len(vars_list)):
            vi = vars_list[i]; vj = vars_list[j]
            if posicoes.get(vi) == posicoes.get(vj):
                add(vi, vj, lambda a, b: a != b)
                add(vj, vi, lambda a, b: a != b)
    return restricoes, vizinhos


# MRV: selecionar variável que possui o menor número de valores possíveis em seu domínio.
def selecionar_mrv(arquivo, dominios):
    nao_atr = [v for v in dominios if v not in arquivo]
    ordenados = sorted(nao_atr, key=lambda v: len(dominios[v]))
    return ordenados[0]

# LCV: serve para ordenar valores pelo quanto deixam opções nos vizinhos
def ordenar_lcv(var, dominios, vizinhos, restricoes, arquivo):
    valores = list(dominios[var])
    pont = []
    for val in valores:
        total = 0
        for nb in vizinhos[var]:
            if nb in arquivo:
                continue
            if (nb, var) in restricoes:
                cfn = restricoes[(nb, var)]
                comp = sum(1 for vnb in dominios[nb] if cfn(vnb, val))
            else:
                comp = len(dominios[nb])
            total += comp
        pont.append((val, total))
    pont.sort(key=lambda x: -x[1])
    return [v for v, _ in pont]

# -------------------------
# Forward checking simples
# -------------------------
def forward_checking(dominios, var, valor, restricoes):
    novos = {v: list(dominios[v])[:] for v in dominios}
    novos[var] = [valor]
    for nb in novos:
        if nb == var:
            continue
        if (nb, var) in restricoes:
            cfn = restricoes[(nb, var)]
            allowed = [vnb for vnb in novos[nb] if cfn(vnb, valor)]
            if not allowed:
                return None
            novos[nb] = allowed
    return novos

# Verificação final (C1..C8) - mantida igual à sua versão
def verificacao_final (arquivo, dados):
    jogadores = list(dados["jogadores"].keys()) # Transforma em lista para melhor manipulaçõa

    t1 = [j for j, t in arquivo.items() if t == "T1"] # Todos os jogadores do T1
    t2 = [j for j, t in arquivo.items() if t == "T2"] # Todos os jogadores do T2

    contar_jogadores = {"T1": len(t1), "T2": len(t2)} # Conta quantos jogadores tem em cada time

    # C1: Balanceamento: |{j | Vj=T1}| = |{j | Vj=T2}| ± 1.
    if abs(contar_jogadores["T1"] - contar_jogadores["T2"]) > 1:
        return False, "C1 violada"

    # C2: J1 e J2 não podem ficar no mesmo time (V1 ≠ V2).
    if arquivo['J1'] == arquivo['J2']:
        return False, "C2 violada"

    # C3: J3 e J4 preferem juntos (V3 = V4).
    if arquivo['J3'] != arquivo['J4']:
        return False, "C3 violada"

    # C4: No mínimo 2 jogadores em cada time
    if contar_jogadores["T1"] < 2 or contar_jogadores["T2"] < 2:
        return False, "C4 violada"

    # C5: J5 não pode ficar no T2 (V5 ≠ T2).
    if arquivo['J5'] == "T2":
        return False, "C5 violada"

    # C6: Se V3 = V4 = T1 então V1 = T2 (equilíbrio condicional).
    if arquivo['J3'] == "T1" and arquivo['J4'] == "T1":
        if arquivo['J1'] != "T2":
            return False, "C6 violada"

    # C7: Jogadores com mesma posição não podem concentrar-se no mesmo time (encode via instância).
    if "C7" in dados['restricoes']:
        c7 = dados['posicoes']
        for i in range(len(jogadores)):
            for j in range(i + 1, len(jogadores)):
                a = jogadores[i]; b = jogadores[j]
                if c7[a] == c7[b] and arquivo[a] == arquivo[b]:
                    return False, f"C7 violada ({a},{b})"

    # C8: Limite de força média por time (encode como soma de ratings não ultrapassar limiar).
    if "limite" in dados :
        limite = float(dados["limite"])
        soma = {"T1": 0, "T2": 0}
        contt = {"T1": 0, "T2": 0}
        for j in jogadores:
            t = arquivo[j]
            soma[t] += int(dados["overais"][j])
            contt[t] += 1
        for t in ("T1", "T2"):
            if contt[t] > 0 and (soma[t] / contt[t]) > limite:
                return False, f"C8 violada em {t}"

    return True, "Restrições aceitas!"

# -------------------------
# Backtracking (sem AC-3 pré). Mede tempo total e tempo de busca.
# -------------------------
def backtracking_solver_sem_ac3(dados, parar_na_primeira=False):
    vars_list = sorted(list(dados["jogadores"].keys()))
    dominios_init = {v: list(dados["jogadores"][v])[:] for v in vars_list}
    restricoes, vizinhos = construir_restricoes_binarias(vars_list, dados.get("posicoes"))

    nodes = 0
    backtracks = 0
    solucoes = []
    total_vars = len(vars_list)

    inicio_total = time.perf_counter()
    inicio_busca = time.perf_counter()

    def backtrack(arquivo, dominios_correntes):
        nonlocal nodes, backtracks, solucoes
        if len(arquivo) == total_vars:
            valido, motivo = verificacao_final(arquivo, dados)
            if valido:
                solucoes.append(dict(arquivo))
                if parar_na_primeira:
                    return True
            else:
                backtracks += 1
            return False

        var = selecionar_mrv(arquivo, dominios_correntes)
        valores = ordenar_lcv(var, dominios_correntes, vizinhos, restricoes, arquivo)
        for val in valores:
            nodes += 1
            pruned = forward_checking(dominios_correntes, var, val, restricoes)
            if pruned is None:
                backtracks += 1
                continue

            arquivo[var] = val
            stop = backtrack(arquivo, pruned)
            if stop is True and parar_na_primeira:
                return True
            del arquivo[var]

        return False

    backtrack({}, dominios_init)
    tempo_busca = time.perf_counter() - inicio_busca
    tempo_total = time.perf_counter() - inicio_total

    stats = {"time": tempo_total, "time_search": tempo_busca,
             "nodes": nodes, "backtracks": backtracks, "solutions": len(solucoes)}
    return solucoes, stats

def main(caminho):
    with open(caminho, "r") as arquivo:
        dados = json.load(arquivo)

    print(f"\n_________ Verificando o JSON: {arquivo.name} __________\n\nJSON:\n")
    print("-- Overais (ratings) --")
    for jogador, overall in dados["overais"].items():
        print(f"  {jogador}: {overall}")
    print("\n-- Domínios (times permitidos) --")
    for jogador, times in dados["jogadores"].items():
        print(f"  {jogador}: {times}")
    print("\n-- Posições --")
    for jogador, posicao in dados["posicoes"].items():
        print(f"  {jogador}: {posicao}")
    print("\n-- Restrições --")
    for restricao, numero in dados["restricoes"].items():
        print(f"  {restricao}: {numero}")
    if "limite" in dados:
        print(f"\n-- Limite de força (C8): {dados['limite']}")
    print("")  # linha em branco

    print("\n-- Verificando restrições --\n")

    print("Rodando solver SEM AC-3 (pré), MRV e LCV...\n")
    solucoes, stats = backtracking_solver_sem_ac3(dados, parar_na_primeira=False)

    print("=== Resultados (SEM AC-3 pré) ===")
    print(f"Tempo total: {stats['time']:.6f} s | Tempo busca: {stats['time_search']:.6f} s")
    print(f"Nós testados: {stats['nodes']} | Retrocessos: {stats['backtracks']}")
    print(f"Soluções encontradas: {stats['solutions']}\n")

    for i, s in enumerate(solucoes, start=1):
        print(f"--- Solução {i} ---")
        for j in sorted(s.keys()):
            print(f"  {j}: {s[j]}")
        print("")

    if not solucoes:
        print("Nenhuma solução válida encontrada.")

main("facil.json")