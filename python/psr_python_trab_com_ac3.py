import json
import time
from collections import deque

# Construir restrições binárias (C2, C3, C7)
def construir_restricoes_binarias(lista_ordenada, posicoes):
    restricoes = {}
    vizinhos = {v: set() for v in lista_ordenada} # Dicionário que mapeia cada variável para o conjunto de outras variáveis que ela possui restrições.
    def add(x, y, fn): # Auxiliar para adicionar restrições
        restricoes[(x, y)] = fn
        vizinhos[x].add(y)

    # C2: J1 e J2 não podem estar no mesmo time.
    if "J1" in lista_ordenada and "J2" in lista_ordenada:
        add("J1", "J2", lambda a, b: a != b)
        add("J2", "J1", lambda a, b: a != b)

    # C3: J3 e J4 devem estar no mesmo time
    if "J3" in lista_ordenada and "J4" in lista_ordenada:
        add("J3", "J4", lambda a, b: a == b)
        add("J4", "J3", lambda a, b: a == b)

    # C7: Jogadores na mesma posição não podem estar no mesmo time
    def restricao_c7(a, b): # Função genérica para a restrição de posição
        return a != b

    for i in range(len(lista_ordenada)):
        for j in range(i+1, len(lista_ordenada)):
            vi = lista_ordenada[i]; vj = lista_ordenada[j]
            if posicoes.get(vi) == posicoes.get(vj):
                add(vi, vj, restricao_c7)
                add(vj, vi, restricao_c7)
    return restricoes, vizinhos # Retorna o dicionário de restrições e o dicionário de vizinhos

# AC-3: consistência de arco (pré-processamento)
# dominios: dicionário {var: [valores possíveis]}
# restricoes: dicionário {(var1, var2): função que retorna True se valores são compatíveis} (ou seja, se existe restrição entre var1 e var2)
def revisao(dominios, xi, xj, restricoes): #
    if (xi, xj) not in restricoes:
        return False
    cfn = restricoes[(xi, xj)] 
    removido = False 
    novo = [] 
    for vi in dominios[xi]:
        # Existe vj em dominios[xj] que satisfaça cfn(vi, vj)?
        if any(cfn(vi, vj) for vj in dominios[xj]): # Se existe, mantém vi
            novo.append(vi)  
        else:
            removido = True
    if removido:
        dominios[xi] = novo
    return removido

# AC-3 principal
def ac3(dominios, restricoes): 
    # Deque (fila) de arcos (xi, xj)
    # Inicia com todos os arcos
    # Deque é mais eficiente que lista para pop(0)
    fila = deque(restricoes.keys()) 
    while fila:
        xi, xj = fila.popleft() # Popleft é uma função de deque que remove e retorna o primeiro elemento
        # Se domínio de xi foi reduzido, re-adiciona arcos (xk, xi)
        if revisao(dominios, xi, xj, restricoes):
            if not dominios[xi]:
                return False
            # Re-adiciona arcos (xk, xi)
            for (xk, _xi) in list(restricoes.keys()):
                if _xi == xi and xk != xj:
                    fila.append((xk, xi)) # Adiciona arco (xk, xi) para re-verificação
    return True

# MRV: selecionar variável que possui o menor número de valores possíveis em seu domínio.
# Arquivo: dict de atribuições já feitas, dominios: dict atual, podado ou inicial
def selecionar_mrv(arquivo, dominios):
    vars_nao_atr = [v for v in dominios if v not in arquivo]
    ordenados = sorted(vars_nao_atr, key=lambda v: len(dominios[v])) # Ordena vars_nao_atr por tamanho do domínio em ordem crescente
    return ordenados[0] # Primeira variável, de menor domínio

# LCV: serve para ordenar valores pelo quanto "atrapalham" os vizinhos
def ordenar_lcv(var, dominios, vizinhos, restricoes, arquivo):
    valores = list(dominios[var])  # Copia valores possíveis da variável var
    pontuacao = [] # Lista que irá as possibilidades de todos os vizinhos se usar este valor para var
    for val in valores:
        total = 0 # Quantos valores são possíveis
        for viz in vizinhos[var]:
            if viz in arquivo: # Para todo vizinho de var, verifica se já está no arquivo
                continue # Pula ele
            if (viz, var) in restricoes:
                cfn = restricoes[(viz, var)]
                comp = sum(1 for vnb in dominios[viz] if cfn(vnb, val)) # Conta quantos valores do domínio do vizinho viz ainda são compatíveis com val
            else:
                comp = len(dominios[viz])
            total += comp # Soma o número de valores possíveis no vizinho ao total
        pontuacao.append((val, total)) # Salva (valor, total) para esse val
    pontuacao.sort(key=lambda x: -x[1]) # Ordena a lista pontuacao pelo maior total primeiro
    return [v for v, _ in pontuacao]

# Forward checking simples -> podar os domínios dos vizinhos após a atribuição de valor para variável no backstrack
# dominios: dicionario com os dominios atuais antes da atribuição, var: variável que desejamos atribuir agora, valor: valor testado e restricoes: dicionario de restrições binárias que foram montadas na função construir_restricoes_binarias
def forward_checking(dominios, var, valor, restricoes):
    novos = {v: list(dominios[v])[:] for v in dominios} # Copia os domínios em um novo dicionário
    novos[var] = [valor]
    for n in list(novos):
        if n == var:
            continue
        if (n, var) in restricoes: # Se existe função que diz que para que n seja válido dado var
            r = restricoes[(n, var)]
            permitidos = [vnb for vnb in novos[n] if r(vnb, valor)] # nova lista de valores permitidos para o vizinho n
            if not permitidos:
                return None # Falha
            novos[n] = permitidos
    return novos # Domínios já podados, que devem ser usados na recursão.

# Verificação final (Todas as restrições C1...C8)
def verificacao_final (arquivo, dados):
    jogadores = list(dados["jogadores"].keys()) # Transforma em lista para melhor manipulaçõa

    t1 = [j for j, t in arquivo.items() if t == "T1"] # Todos os jogadores do T1
    t2 = [j for j, t in arquivo.items() if t == "T2"] # Todos os jogadores do T2

    contar_jogadores = {"T1": len(t1), "T2": len(t2)} # Conta quantos jogadores tem em cada time

    # C1: Balanceamento: |{j | Vj=T1}| = |{j | Vj=T2}| ± 1.
    if abs(contar_jogadores["T1"] - contar_jogadores["T2"]) > 1:
        return False

    # C2: J1 e J2 não podem ficar no mesmo time (V1 ≠ V2).
    if arquivo['J1'] == arquivo['J2']:
        return False

    # C3: J3 e J4 preferem juntos (V3 = V4).
    if arquivo['J3'] != arquivo['J4']:
        return False

    # C4: No mínimo 2 jogadores em cada time
    if contar_jogadores["T1"] < 2 or contar_jogadores["T2"] < 2:
        return False

    # C5: J5 não pode ficar no T2 (V5 ≠ T2).
    if arquivo['J5'] == "T2":
        return False

    # C6: Se V3 = V4 = T1 então V1 = T2 (equilíbrio condicional).
    if arquivo['J3'] == "T1" and arquivo['J4'] == "T1":
        if arquivo['J1'] != "T2":
            return False

    # C7: Jogadores com mesma posição não podem concentrar-se no mesmo time (encode via instância).
    if "C7" in dados['restricoes']:
        c7 = dados['posicoes']
        for i in range(len(jogadores)):
            for j in range(i+1, len(jogadores)):
                a, b = jogadores[i], jogadores[j]
                if c7[a] == c7[b] and arquivo[a] == arquivo[b]:
                    return False

    # C8: Limite de força média por time (encode como soma de ratings não ultrapassar limiar).
    if "limite" in dados :
        # Ajuste para formato que você usa no JSON (ex.: dados['limite']['numero'])
        # Se o JSON tem apenas dados['limite'] como número, mude para float(dados['limite'])
        limite = None
        if isinstance(dados['limite'], dict) and 'numero' in dados['limite']:
            limite = float(dados['limite']['numero'])
        else:
            try:
                limite = float(dados['limite'])
            except Exception:
                limite = None
        if limite is not None:
            soma = {"T1": 0, "T2": 0}
            conta_times = {"T1": 0, "T2": 0}
            for j in jogadores:
                t = arquivo[j]
                soma[t] += int(dados["overais"][j])
                conta_times[t] += 1 # Conta quantos jogadores tem em cada time
            for t in ("T1", "T2"):
                if conta_times[t] > 0 and (soma[t] / conta_times[t]) > limite: # Se o time tem jogadores e a média ultrapassa o limite
                    return False

    return True

# Solver com AC-3 (pré-processamento)
# Usa MRV, LCV, AC-3 (pré), forward checking e checagens parciais
# Entrada: dicionário 'arquivo' no formato JSON mostrado pelo usuário.
# Retorna: lista de soluções e dicionário de estatísticas
def backtracking_solver_com_ac3(dados):
    lista_ordenada = sorted(list(dados["jogadores"].keys()))
    dominios_iniciais = {v: list(dados["jogadores"][v])[:] for v in lista_ordenada}
    restricoes, vizinhos = construir_restricoes_binarias(lista_ordenada, dados["posicoes"])

    # Aplica AC-3 nos domínios iniciais (pré-processamento)
    dominios = {v: list(dominios_iniciais[v])[:] for v in dominios_iniciais}
    inicio_pre = time.time()
    ac_ok = ac3(dominios, restricoes) # ac_ok será False se detectar inconsistência
    fim_pre = time.time()
    tempo_pre = fim_pre - inicio_pre

    # se AC-3 detectar inconsistência, retornamos estatísticas vazias (sem buscar)
    if not ac_ok:
        return [], {"time": tempo_pre, "time_search": 0.0, "nodes": 0, "retrocessos": 0, "solutions": 0}

    nos_encontrados = 0 # Nós testados
    retrocessos = 0 # Retrocessos
    solucoes = [] # Soluções completas encontradas

    inicio_busca = time.time()

    # Busca recursiva. Se as variáveis já tem valor, então chama verificacao_final, se passa vai como solução, senão vai como retrocesso. Se ainda faltam variáveis chama a próxima com MRV e ordena os valores possíveis com LCV
    # Aqui podemos dizer que todas as variáveis foram atribuídas
    def busca(arquivo, dominios_correntes): # dominios_correntes contém os domínios atuais (após forward checking)
        nonlocal nos_encontrados, retrocessos, solucoes
        if len(arquivo) == len(lista_ordenada): 
            if verificacao_final(arquivo, dados) == True: # Já verificado pelas restrições
                solucoes.append(dict(arquivo)) # Salva um cópia dict de arquivo em solucoes
            else: # Verificação final deu False (atribuição inválida)
                retrocessos += 1
            return False

        var = selecionar_mrv(arquivo, dominios_correntes) # Seleciona a próxima variável livre usando a heurística MRV (menor domínio primeiro). Retorna a variável a atribuir
        valores = ordenar_lcv(var, dominios_correntes, vizinhos, restricoes, arquivo) # Ordena os valores possíveis para var usando LCV — valores que menos restringem os vizinhos vêm primeiro
        for v in valores:
            nos_encontrados += 1
            pruned = forward_checking(dominios_correntes, var, v, restricoes) # Conj. de domínios atualizados, depois da poda
            if pruned is None:
                retrocessos += 1
                continue

            arquivo[var] = v
            busca(arquivo, pruned)
            # Desfaz backstrack
            del arquivo[var]

        return False

    busca({}, dominios)
    fim_busca = time.time()
    tempo_busca = fim_busca - inicio_busca
    tempo_total = time.time() - inicio_pre + tempo_busca  # inclui pré-processamento na conta total

    # Dicionário de resposta para apresentação das estatísticas
    stats = {"time": tempo_total, 
             "time_pre": tempo_pre, 
             "time_search": tempo_busca,
             "nodes": nos_encontrados, 
             "retrocessos": retrocessos,
             "solutions": len(solucoes)
             }
    
    return solucoes, stats

def main(caminho):
    with open(caminho, "r") as arquivo:
        # JSON carregado com jogadores, posições, etc
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
    for restricao, numero in dados["restricoes"].items(): # C1, C4, C5, C6, C7
        print(f"  {restricao}: {numero}")
    if "limite" in dados:
        # mantive o formato que você espera no print
        if isinstance(dados['limite'], dict) and 'numero' in dados['limite']:
            limprint = dados['limite']['numero']
        else:
            limprint = dados['limite']
        print(f"\n-- Limite de força (C8): {limprint} --")
    print("")  # linha em branco

    print("\n-- Verificando restrições --\n")

    # Primeiro: rodar COM AC-3 (pré) para comparação
    print("Rodando solver COM AC-3 (pré-processamento), MRV e LCV...\n")
    solucoes_ac3, stats_ac3 = backtracking_solver_com_ac3(dados)
    print("=== Resultados COM AC-3 (pré) ===")
    if "time_pre" in stats_ac3:
        tp = stats_ac3["time_pre"]
    else:
        tp = 0.0
    print(f"Tempo total: {stats_ac3.get('time', 0.0):.8f} s | Pré-processamento: {tp:.8f} s | Busca: {stats_ac3.get('time_search', 0.0):.8f} s")
    print(f"Nós testados: {stats_ac3.get('nodes', 0)} | Retrocessos: {stats_ac3.get('retrocessos', 0)}")
    print(f"Soluções encontradas: {stats_ac3.get('solutions', 0)}\n")
    for i, s in enumerate(solucoes_ac3, start=1):
        print(f"--- Solução {i} (COM AC-3) ---")
        for j in sorted(s.keys()):
            print(f"  {j}: {s[j]}")
        print("")

main("medio.json")