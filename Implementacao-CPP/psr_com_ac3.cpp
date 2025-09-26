// psr_com_ac3.cpp
// Versao com AC-3 + MRV + LCV + Forward Checking
// Compilar: g++ -std=c++11 psr_com_ac3.cpp -o psr_com_ac3

#include <bits/stdc++.h>
#include "json.hpp" // Biblioteca para manipulacao de JSON
using namespace std;
using json = nlohmann::json;

// Tipo para a funcao de restricao binaria. Recebe dois valores (string) e retorna true se a restricao for satisfeita.
typedef function<bool(const string&, const string&)> ConstraintFn;

// Estrutura para representar um jogador (nao utilizada diretamente no solver, mas pode ser util)
struct Jogador {
    string nome;
    vector<string> dominio;
    int overall;
    string posicao;
};

/**
 * @brief Constroi o mapa de restricoes binarias (pares (Xi, Xj) e a funcao de restricao)
 * e o mapa de vizinhos.
 * @param lista Lista de todas as variaveis (jogadores).
 * @param posicoes Mapa que associa cada jogador a sua posicao.
 * @param restricoes Mapa de restricoes binarias.
 * @param vizinhos Mapa de adjacencia para as variaveis.
 */
void construir_restricoes_binarias(const vector<string>& lista, const map<string,string>& posicoes,
    map<pair<string,string>, ConstraintFn>& restricoes,
    map<string, set<string> >& vizinhos) {

    // Funcao lambda auxiliar para adicionar restricoes bidirecionais
    auto add = [&](const string &x, const string &y, ConstraintFn fn) {
        restricoes[make_pair(x,y)] = fn;
        vizinhos[x].insert(y);
    };

    // Inicializa a lista de vizinhos para todas as variaveis
    for (size_t i=0;i<lista.size();++i) vizinhos[lista[i]] = set<string>();

    // C2: J1 e J2 devem estar em times diferentes (a != b)
    if (find(lista.begin(), lista.end(), "J1")!=lista.end() &&
        find(lista.begin(), lista.end(), "J2")!=lista.end()) {
        add("J1","J2", [](const string& a, const string& b){ return a != b; });
        add("J2","J1", [](const string& a, const string& b){ return a != b; });
    }
    // C3: J3 e J4 devem estar no mesmo time (a == b)
    if (find(lista.begin(), lista.end(), "J3")!=lista.end() &&
        find(lista.begin(), lista.end(), "J4")!=lista.end()) {
        add("J3","J4", [](const string& a, const string& b){ return a == b; });
        add("J4","J3", [](const string& a, const string& b){ return a == b; });
    }
    // C7: Jogadores de mesma posicao nao podem estar no mesmo time
    auto restr_c7 = [](const string& a, const string& b){ return a != b; };
    for (size_t i=0;i<lista.size();++i) {
        for (size_t j=i+1;j<lista.size();++j) {
            string vi = lista[i], vj = lista[j];
            auto itvi = posicoes.find(vi);
            auto itvj = posicoes.find(vj);
            if (itvi!=posicoes.end() && itvj!=posicoes.end()) {
                // Se tem a mesma posicao, aplica a restricao (devem ser times diferentes)
                if (itvi->second == itvj->second) {
                    add(vi, vj, restr_c7);
                    add(vj, vi, restr_c7);
                }
            }
        }
    }
}

/**
 * @brief Funcao de Revisao (Arc Consistency) para o arco (xi, xj).
 * Verifica se cada valor 'vi' no dominio de 'xi' e consistente com pelo menos um
 * valor 'vj' no dominio de 'xj'. Se nao, 'vi' e removido.
 * @param dominios Mapa de dominios das variaveis.
 * @param xi Variavel de origem.
 * @param xj Variavel de destino.
 * @param restricoes Mapa de restricoes binarias.
 * @return true se algum valor foi removido do dominio de xi, false caso contrario.
 */
bool revisao(map<string, vector<string> >& dominios, const string &xi, const string &xj,
             const map<pair<string,string>, ConstraintFn>& restricoes) {
    auto key = make_pair(xi, xj);
    if (restricoes.find(key) == restricoes.end()) return false; // Nao ha restricao direta
    ConstraintFn cfn = restricoes.at(key);
    bool removido = false;
    vector<string> novo;
    for (size_t i=0;i<dominios[xi].size();++i) {
        string vi = dominios[xi][i];
        bool ok = false;
        for (size_t j=0;j<dominios[xj].size();++j) {
            string vj = dominios[xj][j];
            // Verifica se existe um valor vj em Dom(xj) compativel com vi
            if (cfn(vi, vj)) { ok = true; break; }
        }
        if (ok) novo.push_back(vi); // vi e consistente
        else removido = true; // vi nao e consistente, sera removido
    }
    if (removido) dominios[xi] = novo;
    return removido;
}

/**
 * @brief Algoritmo AC-3 (Arc Consistency 3) para propagacao de restricoes.
 * Aplica a funcao de revisao em todos os arcos e os re-adiciona a fila
 * se o dominio da variavel de origem for reduzido.
 * @param dominios Mapa de dominios das variaveis (e modificado).
 * @param restricoes Mapa de restricoes binarias.
 * @return true se o CSP apos AC-3 for consistente (nenhum dominio vazio), false caso contrario.
 */
bool ac3(map<string, vector<string> >& dominios, const map<pair<string,string>, ConstraintFn>& restricoes) {
    deque< pair<string,string> > fila;
    // Inicializa a fila com todos os arcos
    for (auto it = restricoes.begin(); it != restricoes.end(); ++it) fila.push_back(it->first);
    while (!fila.empty()) {
        auto arc = fila.front(); fila.pop_front();
        string xi = arc.first, xj = arc.second;
        if (revisao(dominios, xi, xj, restricoes)) {
            if (dominios[xi].empty()) return false; // Dominio vazio -> inconsistencia
            // Re-adiciona todos os arcos (xk, xi) onde xk e vizinho de xi
            for (auto it = restricoes.begin(); it != restricoes.end(); ++it) {
                string xk = it->first.first;
                string _xi = it->first.second;
                // Verifica se o arco e da forma (xk, xi) e xk nao e igual a xj (evita redundancia)
                if (_xi == xi && xk != xj) fila.push_back(make_pair(xk, xi));
            }
        }
    }
    return true;
}

/**
 * @brief Heuristica MRV (Minimum Remaining Values).
 * Seleciona a variavel nao atribuida com o menor dominio.
 * @param asign Atribuicoes atuais (variaveis ja com valor).
 * @param dominios Mapa de dominios atuais.
 * @return O nome da variavel selecionada.
 */
string selecionar_mrv(const map<string,string>& asign, const map<string, vector<string> >& dominios) {
    string best = "";
    size_t best_size = SIZE_MAX; // Maior valor possivel para size_t
    for (auto it = dominios.begin(); it != dominios.end(); ++it) {
        const string &v = it->first;
        if (asign.count(v)) continue; // Ignora variaveis ja atribuidas
        size_t s = it->second.size();
        if (s < best_size) { best_size = s; best = v; }
    }
    return best;
}

/**
 * @brief Heuristica LCV (Least Constraining Value).
 * Ordena os valores (times) no dominio de 'var' priorizando aqueles
 * que causam o menor numero de remocoes nos dominios das variaveis vizinhas nao atribuidas.
 * @param var Variavel a ser atribuida.
 * @param dominios Mapa de dominios atuais.
 * @param vizinhos Mapa de vizinhos das variaveis.
 * @param restricoes Mapa de restricoes binarias.
 * @param asign Atribuicoes atuais.
 * @return Um vetor de valores do dominio de 'var' ordenado por LCV (melhores primeiro).
 */
vector<string> ordenar_lcv(const string &var, const map<string, vector<string> >& dominios,
                           const map<string, set<string> >& vizinhos,
                           const map<pair<string,string>, ConstraintFn>& restricoes,
                           const map<string,string>& asign) {
    vector<string> valores = dominios.at(var);
    vector<pair<string,int> > scores;

    for (size_t i=0;i<valores.size();++i) {
        string val = valores[i];
        int total = 0; // Contagem total de compativeis (menor remocao = maior contagem de compativeis)

        auto itv = vizinhos.find(var);
        if (itv != vizinhos.end()) {
            // Itera sobre os vizinhos da variavel 'var'
            for (auto vn = itv->second.begin(); vn != itv->second.end(); ++vn) {
                const string &viz = *vn;
                if (asign.count(viz)) continue; // So considera vizinhos nao atribuidos

                // A restricao deve ser (Vizinho, Var), pois checamos o impacto no dominio do vizinho
                auto itcons = restricoes.find(make_pair(viz, var));
                if (itcons != restricoes.end()) {
                    auto fn = itcons->second;
                    int cnt = 0; // Conta quantos valores no Dom(viz) sao compativeis com 'val'
                    for (size_t k=0;k<dominios.at(viz).size();++k) {
                        // Se o valor do vizinho for compativel com 'val', ele nao seria removido
                        if (fn(dominios.at(viz)[k], val)) ++cnt;
                    }
                    // Adiciona a contagem de compativeis (quanto maior, melhor)
                    total += cnt;
                } else {
                    // Nao ha restricao direta (Vizinho, Var)
                    total += (int)dominios.at(viz).size();
                }
            }
        }
        scores.push_back(make_pair(val, total));
    }

    // Ordena pelo score (total de compativeis) em ordem decrescente (LCV: mais compativeis primeiro)
    sort(scores.begin(), scores.end(), [](const pair<string,int>& a, const pair<string,int>& b){
        return a.second > b.second;
    });

    vector<string> out;
    for (size_t i=0;i<scores.size();++i) out.push_back(scores[i].first);
    return out;
}

/**
 * @brief Forward Checking.
 * Aplica a atribuicao (var=valor) e remove valores inconsistentes dos dominios
 * dos vizinhos nao atribuidos.
 * @param dominios Dominios atuais.
 * @param var Variavel a ser atribuida.
 * @param valor Valor a ser atribuido.
 * @param restricoes Mapa de restricoes binarias.
 * @param out_novos Mapa de dominios resultantes (passado por referencia para ser preenchido).
 * @return true se a atribuicao e consistente (nenhum dominio de vizinho ficou vazio), false caso contrario.
 */
bool forward_checking(const map<string, vector<string> >& dominios,
                      const string &var, const string &valor,
                      const map<pair<string,string>, ConstraintFn>& restricoes,
                      map<string, vector<string> >& out_novos) {
    out_novos = dominios; // Copia os dominios
    // Define o dominio da variavel atribuida como o valor unico
    out_novos[var].clear(); out_novos[var].push_back(valor);

    // Itera sobre todos os dominios (para checar os vizinhos)
    for (auto it = out_novos.begin(); it != out_novos.end(); ++it) {
        const string &n = it->first;
        if (n == var) continue; // Nao checa a variavel que acabou de ser atribuida

        // Checa a restricao (n, var) - arco de n para var
        auto rIt = restricoes.find(make_pair(n, var));
        if (rIt != restricoes.end()) {
            ConstraintFn r = rIt->second;
            vector<string> permitidos;
            // Itera sobre os valores do dominio de n (antes da atribuicao)
            for (size_t i=0;i<dominios.at(n).size();++i) {
                // Se o valor de n e consistente com o 'valor' de 'var', mantem
                if (r(dominios.at(n)[i], valor)) permitidos.push_back(dominios.at(n)[i]);
            }
            if (permitidos.empty()) return false; // Inconsistencia: dominio do vizinho ficou vazio
            out_novos[n] = permitidos; // Atualiza o dominio de n
        }
    }
    return true;
}

/**
 * @brief Verifica as restricoes globais C1 a C8 apos a atribuicao completa.
 * @param asign Atribuicao completa.
 * @param dados Dados do problema (lidos do JSON).
 * @return true se a atribuicao satisfaz todas as restricoes, false caso contrario.
 */
bool verificacao_final(const map<string,string>& asign, const json& dados) {
    vector<string> jogadores;
    // Pega a lista de todos os jogadores
    for (auto it = dados["jogadores"].begin(); it != dados["jogadores"].end(); ++it) jogadores.push_back(it.key());
    
    // Separa os jogadores nos times T1 e T2
    vector<string> t1, t2;
    for (auto it = asign.begin(); it != asign.end(); ++it) {
        if (it->second == "T1") t1.push_back(it->first);
        else t2.push_back(it->first);
    }
    map<string,int> contar;
    contar["T1"] = (int)t1.size();
    contar["T2"] = (int)t2.size();

    // C1: Equipes com diferenca maxima de 1 no numero de jogadores
    if (abs(contar["T1"] - contar["T2"]) > 1) return false;
    // C2: J1 e J2 devem estar em times diferentes (ja em restricoes binarias)
    if (asign.count("J1") && asign.count("J2") && asign.at("J1") == asign.at("J2")) return false;
    // C3: J3 e J4 devem estar no mesmo time (ja em restricoes binarias)
    if (asign.count("J3") && asign.count("J4") && asign.at("J3") != asign.at("J4")) return false;
    // C4: Cada time deve ter pelo menos 2 jogadores (esta verificacao so faz sentido no final)
    if (contar["T1"] < 2 || contar["T2"] < 2) return false;
    // C5: J5 deve estar no T1 (restricao unaria)
    if (asign.count("J5") && asign.at("J5") == "T2") return false;
    // C6: Se J3 e J4 estao no T1, entao J1 deve estar no T2
    if (asign.count("J1") && asign.count("J3") && asign.count("J4") && 
        asign.at("J3") == "T1" && asign.at("J4") == "T1") {
        if (asign.at("J1") != "T2") return false;
    }
    
    // C7: Jogadores de mesma posicao nao podem estar no mesmo time (verificacao extra)
    if (dados.find("restricoes") != dados.end() && dados["restricoes"].find("C7") != dados["restricoes"].end()) {
        auto posicoes = dados["posicoes"];
        for (size_t i=0;i<jogadores.size();++i) {
            for (size_t j=i+1;j<jogadores.size();++j) {
                string a = jogadores[i], b = jogadores[j];
                // Verifica se as posicoes sao iguais E se estao no mesmo time
                if (posicoes.count(a) && posicoes.count(b) && asign.count(a) && asign.count(b) &&
                    posicoes[a] == posicoes[b] && asign.at(a) == asign.at(b)) return false;
            }
        }
    }
    
    // C8: Limite de Overall Medio por time
    if (dados.find("limite") != dados.end()) {
        double limite = -1;
        // Tenta obter o limite a partir do JSON
        if (dados["limite"].is_object() && dados["limite"].find("numero") != dados["limite"].end())
            limite = dados["limite"]["numero"].get<double>();
        else if (dados["limite"].is_string() || dados["limite"].is_number())
            limite = stod(dados["limite"].get<string>());
        
        if (limite > 0) {
            map<string,int> soma; soma["T1"]=0; soma["T2"]=0;
            map<string,int> cnt; cnt["T1"]=0; cnt["T2"]=0;
            
            for (size_t i=0;i<jogadores.size();++i) {
                string j = jogadores[i];
                string t = asign.at(j);
                // Assume que os overais estao como string no JSON e converte para int
                int ov = stoi(dados["overais"][j].get<string>());
                soma[t] += ov; cnt[t] += 1;
            }
            
            // Verifica se a media de overall excede o limite em cada time
            for (auto tt : {"T1","T2"}) {
                if (cnt[tt] > 0) {
                    double media = (double)soma[tt] / cnt[tt];
                    if (media > limite) return false;
                }
            }
        }
    }
    return true; // Passou em todas as restricoes
}

/**
 * @brief Implementacao do Backtracking Search com AC-3 (pre-processamento), MRV, LCV e Forward Checking.
 * @param dados Dados do problema (JSON).
 * @return Um par: vetor de solucoes encontradas e um mapa de estatisticas.
 */
pair<vector< map<string,string> >, map<string,double> > backtracking_solver_com_ac3(const json& dados) {
    // 1. Inicializacao das variaveis e dominios
    vector<string> lista;
    for (auto it = dados["jogadores"].begin(); it != dados["jogadores"].end(); ++it) lista.push_back(it.key());
    sort(lista.begin(), lista.end()); // Garante ordem consistente
    
    map<string, vector<string> > dominios;
    double soma_tam_dominios_inicial = 0.0;
    for (size_t i=0;i<lista.size();++i) {
        string v = lista[i];
        vector<string> dom;
        // Preenche o dominio inicial (times permitidos)
        for (auto it = dados["jogadores"][v].begin(); it != dados["jogadores"][v].end(); ++it) dom.push_back(it->get<string>());
        dominios[v] = dom;
        soma_tam_dominios_inicial += dom.size();
    }
    double tam_medio_dominios_inicial = lista.empty() ? 0.0 : soma_tam_dominios_inicial / lista.size();

    // 2. Construcao de restricoes
    map<string,string> posmap;
    for (auto it = dados["posicoes"].begin(); it != dados["posicoes"].end(); ++it) posmap[it.key()] = it.value().get<string>();

    map<pair<string,string>, ConstraintFn> restricoes;
    map<string, set<string> > vizinhos;
    construir_restricoes_binarias(lista, posmap, restricoes, vizinhos);

    // 3. AC-3 pré-processamento
    auto t_pre_start = chrono::high_resolution_clock::now();
    bool ac_ok = ac3(dominios, restricoes);
    auto t_pre_end = chrono::high_resolution_clock::now();
    double time_pre = chrono::duration<double>(t_pre_end - t_pre_start).count();

    // 4. Analise pos-AC-3
    double soma_tam_dominios_final = 0.0;
    // Corrigido para sintaxe C++11 (evitando Structured Bindings que exigem C++17)
    for (auto const& item : dominios) {
        soma_tam_dominios_final += item.second.size();
    }
    double tam_medio_dominios_final = lista.empty() ? 0.0 : soma_tam_dominios_final / lista.size();

    if (!ac_ok) {
        // CSP e inconsistente apos AC-3
        map<string,double> stats;
        stats["time"] = time_pre;
        stats["time_pre"] = time_pre;
        stats["time_search"] = 0.0;
        stats["nodes"] = 0;
        stats["backtracks"] = 0;
        stats["solutions"] = 0;
        stats["tam_medio_inicial"] = tam_medio_dominios_inicial;
        stats["tam_medio_final"] = tam_medio_dominios_final;
        return make_pair(vector< map<string,string> >(), stats);
    }

    // 5. Backtracking Search
    long nodes = 0, backtracks = 0;
    vector< map<string,string> > solucoes;

    auto t_search_start = chrono::high_resolution_clock::now();

    // Funcao recursiva para a busca em profundidade
    function<void(map<string,string>&, map<string, vector<string> >&)> busca;
    busca = [&](map<string,string>& asign, map<string, vector<string> >& doms) {
        // Condicao de parada: todas as variaveis foram atribuidas
        if ((int)asign.size() == (int)lista.size()) {
            // Verifica as restricoes globais (C1, C4-C8)
            if (verificacao_final(asign, dados)) solucoes.push_back(asign);
            else backtracks++; // Falhou na verificacao final
            return;
        }
        
        // 1. Selecao de Variavel (MRV)
        string var = selecionar_mrv(asign, doms);
        
        // 2. Ordenacao de Valores (LCV)
        vector<string> valores = ordenar_lcv(var, doms, vizinhos, restricoes, asign);
        
        // 3. Iteracao sobre os valores
        for (size_t i=0;i<valores.size();++i) {
            nodes++;
            string v = valores[i];
            
            // 4. Propagacao (Forward Checking)
            map<string, vector<string> > novos;
            if (!forward_checking(doms, var, v, restricoes, novos)) { 
                backtracks++; // Falha no Forward Checking
                continue; 
            }
            
            // 5. Atribuicao e Chamada Recursiva
            asign[var] = v;
            busca(asign, novos);
            
            // 6. Backtrack (desfaz a atribuicao)
            asign.erase(var);
        }
    };

    map<string,string> inicial;
    busca(inicial, dominios);

    auto t_search_end = chrono::high_resolution_clock::now();
    
    double time_search = chrono::duration<double>(t_search_end - t_search_start).count();
    double time_total = chrono::duration<double>(t_search_end - t_pre_start).count();

    // 6. Coleta de Estatisticas
    map<string,double> stats;
    stats["time"] = time_total;
    stats["time_pre"] = time_pre;
    stats["time_search"] = time_search;
    stats["nodes"] = (double)nodes; // Convertendo para double para armazenar no mapa de stats
    stats["backtracks"] = (double)backtracks;
    stats["solutions"] = (double)solucoes.size();
    stats["tam_medio_inicial"] = tam_medio_dominios_inicial;
    stats["tam_medio_final"] = tam_medio_dominios_final;

    return make_pair(solucoes, stats);
}

int main(int argc, char* argv[]) {
    // Define o arquivo JSON padrao e tenta ler o argumento da linha de comando
    string arquivo = "medio.json";
    if (argc >= 2) arquivo = argv[1];

    // Abre o arquivo JSON
    ifstream f(arquivo.c_str());
    if (!f.is_open()) {
        cerr << "Erro ao abrir o arquivo JSON: " << arquivo << endl;
        return 1;
    }
    json dados; f >> dados; f.close();

    // Impressao dos dados lidos (sem caracteres especiais)
    cout << "\n_________ Verificando o JSON: " << arquivo << " __________\n\nJSON:\n\n";
    
    cout << "-- Overais (ratings) --\n";
    for (auto it = dados["overais"].begin(); it != dados["overais"].end(); ++it)
        cout << "  " << it.key() << ": " << it.value().get<string>() << "\n";

    cout << "\n-- Dominios (times permitidos) --\n";
    for (auto it = dados["jogadores"].begin(); it != dados["jogadores"].end(); ++it) {
        cout << "  " << it.key() << ": ";
        for (auto t = it.value().begin(); t != it.value().end(); ++t) cout << t->get<string>() << " ";
        cout << "\n";
    }

    cout << "\n-- Posicoes --\n";
    for (auto it = dados["posicoes"].begin(); it != dados["posicoes"].end(); ++it)
        cout << "  " << it.key() << ": " << it.value().get<string>() << "\n";

    cout << "\n-- Restricoes --\n";
    if (dados.find("restricoes") != dados.end()) {
        for (auto it = dados["restricoes"].begin(); it != dados["restricoes"].end(); ++it)
            cout << "  " << it.key() << ": " << it.value() << "\n";
    } else {
        cout << "  (nenhuma listada no JSON)\n";
    }
    if (dados.find("limite") != dados.end()) {
        if (dados["limite"].is_object() && dados["limite"].find("numero") != dados["limite"].end())
            cout << "\n-- Limite de forca (C8): " << dados["limite"]["numero"] << " --\n";
        else
            cout << "\n-- Limite de forca (C8): " << dados["limite"] << " --\n";
    }

    cout << "\nRodando solver COM AC-3 (pre), MRV e LCV...\n\n";
    
    // Roda o solver
    pair<vector< map<string,string> >, map<string,double> > res = backtracking_solver_com_ac3(dados);
    vector< map<string,string> > solucoes = res.first;
    map<string,double> stats = res.second;

    // Impressao dos resultados no formato solicitado (sem caracteres especiais)
    cout << "=== Resultados COM AC-3 (pre) ===\n";
    
    // Impacto do AC-3 nos dominios
    cout << "Impacto do AC-3 nos dominios: Tamanho medio de "
         << fixed << setprecision(2) << stats["tam_medio_inicial"] << " -> "
         << fixed << setprecision(2) << stats["tam_medio_final"] << "\n";

    // Tempos
    double tp = 0.0;
    if (stats.count("time_pre")) tp = stats["time_pre"];
    cout << "Tempo total: " << fixed << setprecision(8) << stats["time"]
         << " s | Pre-processamento: " << fixed << setprecision(8) << tp
         << " s | Busca: " << fixed << setprecision(8) << stats["time_search"] << " s\n";
         
    // Contadores
    cout << "Nos testados: " << (long)stats["nodes"]
         << " | Retrocessos: " << (long)stats["backtracks"] << "\n";
    cout << "Solucoes encontradas: " << (long)stats["solutions"] << "\n\n";

    // Lista de solucoes
    for (size_t i=0;i<solucoes.size();++i) {
        cout << "--- Solucao " << (i+1) << " (COM AC-3) ---\n";
        auto s = solucoes[i];
        vector<string> keys;
        for (auto it = s.begin(); it != s.end(); ++it) keys.push_back(it->first);
        sort(keys.begin(), keys.end()); // Garante ordem alfabetica
        for (size_t k=0;k<keys.size();++k) cout << "  " << keys[k] << ": " << s[keys[k]] << "\n";
        cout << "\n";
    }
    if (solucoes.empty()) cout << "Nenhuma solucao valida encontrada.\n";

    return 0;
}