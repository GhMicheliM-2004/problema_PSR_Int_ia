# solver_mrv_lcv_ac3.py
# Solver simples com AC-3 (pré), MRV (variável) e LCV (valores).
# Mantém a leitura/print do início do seu arquivo e produz saída organizada.

import json
import time
from collections import deque, defaultdict
import sys

# -------------------------
# Construir restrições binárias simples (C2, C3, C7)
# cada entrada é uma função fn(a,b)->bool
# -------------------------
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

# -------------------------
# AC-3: consistência de arco (pré-processamento)
# -------------------------
def revise(dominios, xi, xj, restricoes):
    if (xi, xj) not in restricoes:
        return False
    cfn = restricoes[(xi, xj)]
    removido = False
    novo = []
    for vi in dominios[xi]:
        # existe vj em dominios[xj] que satisfaça cfn(vi, vj)?
        if any(cfn(vi, vj) for vj in dominios[xj]):
            novo.append(vi)
        else:
            removido = True
    if removido:
        dominios[xi] = novo
    return removido

def ac3(dominios, restricoes):
    fila = deque(restricoes.keys())
    while fila:
        xi, xj = fila.popleft()
        if revise(dominios, xi, xj, restricoes):
            if not dominios[xi]:
                return False
            # re-adiciona arcos (xk, xi)
            for (xk, _xi) in list(restricoes.keys()):
                if _xi == xi and xk != xj:
                    fila.append((xk, xi))
    return True

# -------------------------
# Seleção MRV (variável com menor domínio)
# -------------------------
def selecionar_mrv(assignment, dominios):
    nao_atr = [v for v in dominios if v not in assignment]
    # menor domínio
    tam_min = min(len(dominios[v]) for v in nao_atr)
    candidatos = [v for v in nao_atr if len(dominios[v]) == tam_min]
    # desempate: retornar primeiro (estável)
    return candidatos[0]

# -------------------------
# LCV: ordenar valores pelo quanto deixam opções nos vizinhos
# -------------------------
def ordenar_lcv(var, dominios, vizinhos, restricoes, assignment):
    valores = list(dominios[var])
    pont = []
    for val in valores:
        total = 0
        for nb in vizinhos[var]:
            if nb in assignment:
                continue
            if (nb, var) in restricoes:
                cfn = restricoes[(nb, var)]
                comp = sum(1 for vnb in dominios[nb] if cfn(vnb, val))
            else:
                comp = len(dominios[nb])
            total += comp
        pont.append((val, total))
    # ordenar decrescente por 'total' (maior total -> menos restritivo)
    pont.sort(key=lambda x: -x[1])
    return [v for v, _ in pont]

# -------------------------
# Forward checking simples: fixa var=valor e filtra vizinhos por restricoes (nb,var)
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

# -------------------------
# Verificação final (todas as restrições C1..C8)
# -------------------------
def verificacao_final(assignment, dados):
    jogadores = list(dados["jogadores"].keys())

    # C4/C1: contagem por time
    cont = {"T1": 0, "T2": 0}
    for j in jogadores:
        t = assignment.get(j)
        # se valor não estiver no domínio original, falha
        if t not in dados["jogadores"][j]:
            return False, f"Valor {t} inválido para {j}"
        cont[t] += 1

    # C1: balanceamento
    if abs(cont["T1"] - cont["T2"]) > 1:
        return False, "C1 violada"

    # C4: mínimo 2 por time
    if cont["T1"] < 2 or cont["T2"] < 2:
        return False, "C4 violada"

    # C2
    if assignment.get("J1") == assignment.get("J2"):
        return False, "C2 violada"

    # C3
    if assignment.get("J3") != assignment.get("J4"):
        return False, "C3 violada"

    # C5
    if assignment.get("J5") == "T2":
        return False, "C5 violada"

    # C6: condicional
    if assignment.get("J3") == "T1" and assignment.get("J4") == "T1":
        if assignment.get("J1") != "T2":
            return False, "C6 violada"

    # C7: mesma posição não no mesmo time
    pos = dados.get("posicoes", {})
    vars_list = jogadores
    for i in range(len(vars_list)):
        for j in range(i+1, len(vars_list)):
            a = vars_list[i]; b = vars_list[j]
            if pos.get(a) == pos.get(b) and assignment[a] == assignment[b]:
                return False, f"C7 violada ({a},{b})"

    # C8 opcional
    if "limite_forca" in dados and dados["limite_forca"] is not None:
        limite = float(dados["limite_forca"])
        soma = {"T1": 0, "T2": 0}
        contt = {"T1": 0, "T2": 0}
        for j in jogadores:
            t = assignment[j]
            soma[t] += int(dados["overais"][j])
            contt[t] += 1
        for t in ("T1", "T2"):
            if contt[t] > 0 and (soma[t] / contt[t]) > limite:
                return False, f"C8 violada em {t}"

    return True, ""

# -------------------------
# Backtracking com MRV + LCV + AC3 pré
# -------------------------
def backtracking_solver(dados, usar_ac3_na_busca=False, parar_na_primeira=False):
    vars_list = sorted(list(dados["jogadores"].keys()))  # ordem estável
    dominios_init = {v: list(dados["jogadores"][v])[:] for v in vars_list}
    restricoes, vizinhos = construir_restricoes_binarias(vars_list, dados["posicoes"])

    # AC-3 pré-processamento
    dominios = {v: list(dominios_init[v])[:] for v in dominios_init}
    ac_ok = ac3(dominios, restricoes)
    if not ac_ok:
        print("Inconsistente após AC-3 (pré-processamento).")
        return [], {"success": False, "reason": "AC-3 pré falhou"}

    nodes = 0
    backtracks = 0
    solucoes = []
    total_vars = len(vars_list)
    inicio = time.perf_counter()

    def backtrack(assignment, dominios_correntes):
        nonlocal nodes, backtracks, solucoes
        # se completo, verificar tudo e guardar solução
        if len(assignment) == total_vars:
            valido, motivo = verificacao_final(assignment, dados)
            if valido:
                solucoes.append(dict(assignment))
                if parar_na_primeira:
                    return True  # sinaliza para parar tudo
            else:
                backtracks += 1
            return False

        # escolher variável por MRV
        var = selecionar_mrv(assignment, dominios_correntes)
        # ordenar valores por LCV
        valores = ordenar_lcv(var, dominios_correntes, vizinhos, restricoes, assignment)
        for val in valores:
            nodes += 1
            pruned = forward_checking(dominios_correntes, var, val, restricoes)
            if pruned is None:
                backtracks += 1
                continue
            # opcional: AC-3 durante busca (aumenta custo mas poda mais)
            if usar_ac3_na_busca:
                copia = {v: list(pruned[v])[:] for v in pruned}
                if not ac3(copia, restricoes):
                    backtracks += 1
                    continue
                pruned = copia

            # atribui e recursa
            assignment[var] = val
            stop = backtrack(assignment, pruned)
            if stop is True and parar_na_primeira:
                return True
            # desfaz
            del assignment[var]

        return False

    backtrack({}, dominios)
    tempo = time.perf_counter() - inicio
    stats = {"time": tempo, "nodes": nodes, "backtracks": backtracks, "solutions": len(solucoes)}
    return solucoes, stats

# -------------------------
# Main: leitura + execução + impressão organizada
# -------------------------
def main(caminho="dificil.json"):
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
    if "limite" in dados:
        print(f"\n-- Limite de força (C8): {dados["limite"]}")
    print("")  # linha em branco

    print("Rodando solver com AC-3 (pré), MRV (variável) e LCV (valores)...\n")
    solucoes, stats = backtracking_solver(dados, usar_ac3_na_busca=False, parar_na_primeira=False)
    print("=== Resultados ===")
    print(f"Tempo: {stats['time']:.6f} s | Nós testados: {stats['nodes']} | Retrocessos: {stats['backtracks']}")
    print(f"Soluções encontradas: {stats['solutions']}\n")
    for i, s in enumerate(solucoes, start=1):
        print(f"--- Solução {i} ---")
        for j in sorted(s.keys()):
            print(f"  {j}: {s[j]}")
        print("")
    if not solucoes:
        print("Nenhuma solução válida encontrada.")

if __name__ == "__main__":
    caminho = sys.argv[1] if len(sys.argv) > 1 else "dificil.json"
    main(caminho)
