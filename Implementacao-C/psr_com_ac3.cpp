// psr_com_ac3.cpp
// Versão com AC-3 + MRV + LCV + Forward Checking
// Compilar: g++ -std=c++11 psr_com_ac3.cpp -o psr_com_ac3

#include <bits/stdc++.h>
#include "json.hpp"
using namespace std;
using json = nlohmann::json;

typedef function<bool(const string&, const string&)> ConstraintFn;

struct Jogador {
    string nome;
    vector<string> dominio;
    int overall;
    string posicao;
};

void construir_restricoes_binarias(const vector<string>& lista, const map<string,string>& posicoes,
    map<pair<string,string>, ConstraintFn>& restricoes,
    map<string, set<string> >& vizinhos) {

    auto add = [&](const string &x, const string &y, ConstraintFn fn) {
        restricoes[make_pair(x,y)] = fn;
        vizinhos[x].insert(y);
    };

    for (size_t i=0;i<lista.size();++i) vizinhos[lista[i]] = set<string>();

    // C2
    if (find(lista.begin(), lista.end(), "J1")!=lista.end() &&
        find(lista.begin(), lista.end(), "J2")!=lista.end()) {
        add("J1","J2", [](const string& a, const string& b){ return a != b; });
        add("J2","J1", [](const string& a, const string& b){ return a != b; });
    }
    // C3
    if (find(lista.begin(), lista.end(), "J3")!=lista.end() &&
        find(lista.begin(), lista.end(), "J4")!=lista.end()) {
        add("J3","J4", [](const string& a, const string& b){ return a == b; });
        add("J4","J3", [](const string& a, const string& b){ return a == b; });
    }
    // C7
    auto restr_c7 = [](const string& a, const string& b){ return a != b; };
    for (size_t i=0;i<lista.size();++i) {
        for (size_t j=i+1;j<lista.size();++j) {
            string vi = lista[i], vj = lista[j];
            auto itvi = posicoes.find(vi);
            auto itvj = posicoes.find(vj);
            if (itvi!=posicoes.end() && itvj!=posicoes.end()) {
                if (itvi->second == itvj->second) {
                    add(vi, vj, restr_c7);
                    add(vj, vi, restr_c7);
                }
            }
        }
    }
}

// Revisão xi,xj: se não existe vj compatível, remove vi; retorna true se removeu algo
bool revisao(map<string, vector<string> >& dominios, const string &xi, const string &xj,
             const map<pair<string,string>, ConstraintFn>& restricoes) {
    auto key = make_pair(xi, xj);
    if (restricoes.find(key) == restricoes.end()) return false;
    ConstraintFn cfn = restricoes.at(key);
    bool removido = false;
    vector<string> novo;
    for (size_t i=0;i<dominios[xi].size();++i) {
        string vi = dominios[xi][i];
        bool ok = false;
        for (size_t j=0;j<dominios[xj].size();++j) {
            string vj = dominios[xj][j];
            if (cfn(vi, vj)) { ok = true; break; }
        }
        if (ok) novo.push_back(vi);
        else removido = true;
    }
    if (removido) dominios[xi] = novo;
    return removido;
}

// AC-3
bool ac3(map<string, vector<string> >& dominios, const map<pair<string,string>, ConstraintFn>& restricoes) {
    deque< pair<string,string> > fila;
    for (auto it = restricoes.begin(); it != restricoes.end(); ++it) fila.push_back(it->first);
    while (!fila.empty()) {
        auto arc = fila.front(); fila.pop_front();
        string xi = arc.first, xj = arc.second;
        if (revisao(dominios, xi, xj, restricoes)) {
            if (dominios[xi].empty()) return false;
            // re-adiciona arcos (xk, xi)
            for (auto it = restricoes.begin(); it != restricoes.end(); ++it) {
                string xk = it->first.first;
                string _xi = it->first.second;
                if (_xi == xi && xk != xj) fila.push_back(make_pair(xk, xi));
            }
        }
    }
    return true;
}

// MRV
string selecionar_mrv(const map<string,string>& asign, const map<string, vector<string> >& dominios) {
    string best = "";
    size_t best_size = SIZE_MAX;
    for (auto it = dominios.begin(); it != dominios.end(); ++it) {
        const string &v = it->first;
        if (asign.count(v)) continue;
        size_t s = it->second.size();
        if (s < best_size) { best_size = s; best = v; }
    }
    return best;
}

// LCV
vector<string> ordenar_lcv(const string &var, const map<string, vector<string> >& dominios,
                           const map<string, set<string> >& vizinhos,
                           const map<pair<string,string>, ConstraintFn>& restricoes,
                           const map<string,string>& asign) {
    vector<string> valores = dominios.at(var);
    vector<pair<string,int> > scores;
    for (size_t i=0;i<valores.size();++i) {
        string val = valores[i];
        int total = 0;
        auto itv = vizinhos.find(var);
        if (itv != vizinhos.end()) {
            for (auto vn = itv->second.begin(); vn != itv->second.end(); ++vn) {
                const string &viz = *vn;
                if (asign.count(viz)) continue;
                auto itcons = restricoes.find(make_pair(viz, var));
                if (itcons != restricoes.end()) {
                    auto fn = itcons->second;
                    int cnt = 0;
                    for (size_t k=0;k<dominios.at(viz).size();++k) {
                        if (fn(dominios.at(viz)[k], val)) ++cnt;
                    }
                    total += cnt;
                } else {
                    total += (int)dominios.at(viz).size();
                }
            }
        }
        scores.push_back(make_pair(val, total));
    }
    sort(scores.begin(), scores.end(), [](const pair<string,int>& a, const pair<string,int>& b){
        return a.second > b.second;
    });
    vector<string> out;
    for (size_t i=0;i<scores.size();++i) out.push_back(scores[i].first);
    return out;
}

// Forward checking
bool forward_checking(const map<string, vector<string> >& dominios,
                      const string &var, const string &valor,
                      const map<pair<string,string>, ConstraintFn>& restricoes,
                      map<string, vector<string> >& out_novos) {
    out_novos = dominios;
    out_novos[var].clear(); out_novos[var].push_back(valor);
    for (auto it = out_novos.begin(); it != out_novos.end(); ++it) {
        const string &n = it->first;
        if (n == var) continue;
        auto rIt = restricoes.find(make_pair(n, var));
        if (rIt != restricoes.end()) {
            ConstraintFn r = rIt->second;
            vector<string> permitidos;
            for (size_t i=0;i<dominios.at(n).size();++i) {
                if (r(dominios.at(n)[i], valor)) permitidos.push_back(dominios.at(n)[i]);
            }
            if (permitidos.empty()) return false;
            out_novos[n] = permitidos;
        }
    }
    return true;
}

// Verificação final C1..C8
bool verificacao_final(const map<string,string>& asign, const json& dados) {
    vector<string> jogadores;
    for (auto it = dados["jogadores"].begin(); it != dados["jogadores"].end(); ++it) jogadores.push_back(it.key());
    vector<string> t1, t2;
    for (auto it = asign.begin(); it != asign.end(); ++it) {
        if (it->second == "T1") t1.push_back(it->first);
        else t2.push_back(it->first);
    }
    map<string,int> contar;
    contar["T1"] = (int)t1.size();
    contar["T2"] = (int)t2.size();

    if (abs(contar["T1"] - contar["T2"]) > 1) return false;
    if (asign.at("J1") == asign.at("J2")) return false;
    if (asign.at("J3") != asign.at("J4")) return false;
    if (contar["T1"] < 2 || contar["T2"] < 2) return false;
    if (asign.at("J5") == "T2") return false;
    if (asign.at("J3") == "T1" && asign.at("J4") == "T1") {
        if (asign.at("J1") != "T2") return false;
    }
    if (dados.find("restricoes") != dados.end()) {
        if (dados["restricoes"].find("C7") != dados["restricoes"].end()) {
            auto c7 = dados["posicoes"];
            for (size_t i=0;i<jogadores.size();++i) {
                for (size_t j=i+1;j<jogadores.size();++j) {
                    string a = jogadores[i], b = jogadores[j];
                    if (c7[a] == c7[b] && asign.at(a) == asign.at(b)) return false;
                }
            }
        }
    }
    if (dados.find("limite") != dados.end()) {
        double limite = -1;
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
                int ov = stoi(dados["overais"][j].get<string>());
                soma[t] += ov; cnt[t] += 1;
            }
            for (auto tt : {"T1","T2"}) {
                if (cnt[tt] > 0) {
                    double media = (double)soma[tt] / cnt[tt];
                    if (media > limite) return false;
                }
            }
        }
    }
    return true;
}

// Solver COM AC-3
pair<vector< map<string,string> >, map<string,double> > backtracking_solver_com_ac3(const json& dados) {
    vector<string> lista;
    for (auto it = dados["jogadores"].begin(); it != dados["jogadores"].end(); ++it) lista.push_back(it.key());
    sort(lista.begin(), lista.end());
    map<string, vector<string> > dominios;
    for (size_t i=0;i<lista.size();++i) {
        string v = lista[i];
        vector<string> dom;
        for (auto it = dados["jogadores"][v].begin(); it != dados["jogadores"][v].end(); ++it) dom.push_back(it->get<string>());
        dominios[v] = dom;
    }

    map<string,string> posmap;
    for (auto it = dados["posicoes"].begin(); it != dados["posicoes"].end(); ++it) posmap[it.key()] = it.value().get<string>();

    map<pair<string,string>, ConstraintFn> restricoes;
    map<string, set<string> > vizinhos;
    construir_restricoes_binarias(lista, posmap, restricoes, vizinhos);

    // AC-3 pré-processamento
    auto t_pre_start = chrono::high_resolution_clock::now();
    bool ac_ok = ac3(dominios, restricoes);
    auto t_pre_end = chrono::high_resolution_clock::now();
    double time_pre = chrono::duration<double>(t_pre_end - t_pre_start).count();

    if (!ac_ok) {
        // inconsistente no pré-processamento
        map<string,double> stats;
        stats["time"] = time_pre;
        stats["time_pre"] = time_pre;
        stats["time_search"] = 0.0;
        stats["nodes"] = 0;
        stats["backtracks"] = 0;
        stats["solutions"] = 0;
        return make_pair(vector< map<string,string> >(), stats);
    }

    long nodes = 0, backtracks = 0;
    vector< map<string,string> > solucoes;

    auto t_search_start = chrono::high_resolution_clock::now();

    function<void(map<string,string>&, map<string, vector<string> >&)> busca;
    busca = [&](map<string,string>& asign, map<string, vector<string> >& doms) {
        if ((int)asign.size() == (int)lista.size()) {
            if (verificacao_final(asign, dados)) solucoes.push_back(asign);
            else backtracks++;
            return;
        }
        string var = selecionar_mrv(asign, doms);
        vector<string> valores = ordenar_lcv(var, doms, vizinhos, restricoes, asign);
        for (size_t i=0;i<valores.size();++i) {
            nodes++;
            string v = valores[i];
            map<string, vector<string> > novos;
            if (!forward_checking(doms, var, v, restricoes, novos)) { backtracks++; continue; }
            asign[var] = v;
            busca(asign, novos);
            asign.erase(var);
        }
    };

    map<string,string> inicial;
    busca(inicial, dominios);

    auto t_search_end = chrono::high_resolution_clock::now();
    auto t_total_end = chrono::high_resolution_clock::now();

    double time_search = chrono::duration<double>(t_search_end - t_search_start).count();
    double time_total = chrono::duration<double>(t_total_end - t_pre_start).count();

    map<string,double> stats;
    stats["time"] = time_total;
    stats["time_pre"] = time_pre;
    stats["time_search"] = time_search;
    stats["nodes"] = nodes;
    stats["backtracks"] = backtracks;
    stats["solutions"] = solucoes.size();

    return make_pair(solucoes, stats);
}

int main(int argc, char* argv[]) {
    string arquivo = "medio.json";
    if (argc >= 2) arquivo = argv[1];

    ifstream f(arquivo.c_str());
    if (!f.is_open()) {
        cerr << "Erro ao abrir o arquivo JSON: " << arquivo << endl;
        return 1;
    }
    json dados; f >> dados; f.close();

    cout << "\n_________ Verificando o JSON: " << arquivo << " __________\n\nJSON:\n\n";
    cout << "-- Overais (ratings) --\n";
    for (auto it = dados["overais"].begin(); it != dados["overais"].end(); ++it)
        cout << "  " << it.key() << ": " << it.value().get<string>() << "\n";

    cout << "\n-- Domínios (times permitidos) --\n";
    for (auto it = dados["jogadores"].begin(); it != dados["jogadores"].end(); ++it) {
        cout << "  " << it.key() << ": ";
        for (auto t = it.value().begin(); t != it.value().end(); ++t) cout << t->get<string>() << " ";
        cout << "\n";
    }

    cout << "\n-- Posições --\n";
    for (auto it = dados["posicoes"].begin(); it != dados["posicoes"].end(); ++it)
        cout << "  " << it.key() << ": " << it.value().get<string>() << "\n";

    cout << "\n-- Restrições --\n";
    if (dados.find("restricoes") != dados.end()) {
        for (auto it = dados["restricoes"].begin(); it != dados["restricoes"].end(); ++it)
            cout << "  " << it.key() << ": " << it.value() << "\n";
    } else {
        cout << "  (nenhuma listada no JSON)\n";
    }
    if (dados.find("limite") != dados.end()) {
        if (dados["limite"].is_object() && dados["limite"].find("numero") != dados["limite"].end())
            cout << "\n-- Limite de força (C8): " << dados["limite"]["numero"] << " --\n";
        else
            cout << "\n-- Limite de força (C8): " << dados["limite"] << " --\n";
    }

    cout << "\nRodando solver COM AC-3 (pré), MRV e LCV...\n\n";
    pair<vector< map<string,string> >, map<string,double> > res = backtracking_solver_com_ac3(dados);
    vector< map<string,string> > solucoes = res.first;
    map<string,double> stats = res.second;

    cout << "=== Resultados COM AC-3 (pré) ===\n";
    double tp = 0.0;
    if (stats.find("time_pre") != stats.end()) tp = stats["time_pre"];
    cout << "Tempo total: " << fixed << setprecision(8) << stats["time"] << " s | Pré-processamento: " << tp << " s | Busca: " << stats["time_search"] << " s\n";
    cout << "Nós testados: " << (long)stats["nodes"] << " | Retrocessos: " << (long)stats["backtracks"] << "\n";
    cout << "Soluções encontradas: " << (long)stats["solutions"] << "\n\n";

    for (size_t i=0;i<solucoes.size();++i) {
        cout << "--- Solução " << (i+1) << " (COM AC-3) ---\n";
        auto s = solucoes[i];
        vector<string> keys;
        for (auto it = s.begin(); it != s.end(); ++it) keys.push_back(it->first);
        sort(keys.begin(), keys.end());
        for (size_t k=0;k<keys.size();++k) cout << "  " << keys[k] << ": " << s[keys[k]] << "\n";
        cout << "\n";
    }
    if (solucoes.empty()) cout << "Nenhuma solução válida encontrada.\n";

    return 0;
}
