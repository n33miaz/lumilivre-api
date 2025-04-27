package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import br.com.lumilivre.api.data.AlunoRequestDTO;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.EnderecoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.AlunoRepository;
import br.com.lumilivre.api.repository.CursoRepository;
import br.com.lumilivre.api.repository.EnderecoRepository;

import java.util.Map;

@Service
public class AlunoService {

    @Autowired
    private CursoRepository cr;

    @Autowired
    private AlunoRepository ar;

    @Autowired
    private ResponseModel rm;

    @Autowired
    private EnderecoRepository er;

    private final RestTemplate restTemplate = new RestTemplate();

    public Iterable<AlunoModel> listar() {
        return ar.findAll();
    }

    public ResponseEntity<?> cadastrarAluno(AlunoRequestDTO alunoRequestDTO, String cep) {
        // Verifica se já existe um aluno com a mesma matrícula
        if (ar.findByMatricula(alunoRequestDTO.getMatricula()) != null) {
            return new ResponseEntity<>("Aluno já cadastrado com essa matrícula.", HttpStatus.BAD_REQUEST);
        }
    
        // Verifica se o curso_id foi fornecido e busca o curso correspondente
        if (alunoRequestDTO.getCursoId() != null) {
            // Buscar o curso no banco de dados
            CursoModel curso = cr.findById(alunoRequestDTO.getCursoId())(null);
            if (curso != null) {
                alunoRequestDTO.setCursoId(curso.getId()); // Associa o cursoId ao aluno
            } else {
                return new ResponseEntity<>("Curso não encontrado.", HttpStatus.BAD_REQUEST);
            }
        }
    
        // Se o CEP for fornecido, busca o endereço
        if (cep != null && !cep.isEmpty()) {
            EnderecoModel endereco = buscarPorEndereco(cep);
            if (endereco != null) {
                alunoRequestDTO.setCep(endereco.getCep()); // Associa o endereço ao aluno, mas com o cep em formato string
            }
        }
    
        // Cria um aluno a partir do DTO e persiste no banco de dados
        AlunoModel aluno = new AlunoModel();
        aluno.setMatricula(alunoRequestDTO.getMatricula());
        aluno.setNome(alunoRequestDTO.getNome());
        aluno.setSobrenome(alunoRequestDTO.getSobrenome());
        aluno.setCpf(alunoRequestDTO.getCpf());
        aluno.setDataNascimento(alunoRequestDTO.getDataNascimento());
        aluno.setCelular(alunoRequestDTO.getCelular());
        aluno.setEmail(alunoRequestDTO.getEmail());
        aluno.setCursoId(alunoRequestDTO.getCursoId()); // Atribui o cursoId ao aluno
        aluno.setEndereco(new EnderecoModel()); // Associa o endereço (será salvo posteriormente)
    
        // Salva o aluno no banco de dados
        AlunoModel alunoCadastrado = ar.save(aluno);
        return new ResponseEntity<>(alunoCadastrado, HttpStatus.CREATED);
    }
    



    // Alteração do aluno existente
    public ResponseEntity<?> alterarAluno(String matricula, AlunoModel alunoRequestDTO, String cep) {
        // Verifica se o aluno existe no banco de dados
        AlunoModel alunoExistente = ar.findByMatricula(matricula);
        if (alunoExistente == null) {
            return new ResponseEntity<>("Aluno não encontrado para a matrícula fornecida.", HttpStatus.NOT_FOUND);
        }

        // Atualiza os campos do aluno com os novos valores
        alunoExistente.setNome(alunoRequestDTO.getNome());
        alunoExistente.setSobrenome(alunoRequestDTO.getSobrenome());
        alunoExistente.setCpf(alunoRequestDTO.getCpf());
        alunoExistente.setDataNascimento(alunoRequestDTO.getDataNascimento());
        alunoExistente.setCelular(alunoRequestDTO.getCelular());
        alunoExistente.setEmail(alunoRequestDTO.getEmail());

        // Se um novo CEP foi fornecido, busca o endereço e atualiza o aluno
        if (cep != null && !cep.isEmpty()) {
            EnderecoModel endereco = buscarPorEndereco(cep);
            if (endereco != null) {
                er.save(endereco); // Salva o novo endereço
                alunoExistente.setEndereco(endereco); // Atualiza o aluno com o novo endereço
            }
        }

        // Salva o aluno alterado no banco de dados
        ar.save(alunoExistente);
        return new ResponseEntity<>(alunoExistente, HttpStatus.OK);
    }

    private EnderecoModel buscarPorEndereco(String cep) {
        String url = "https://viacep.com.br/ws/" + cep + "/json/";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        // Verifica se a resposta foi bem-sucedida e se não tem o campo "erro"
        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> dados = response.getBody();

            if (dados != null && !dados.containsKey("erro")) {
                EnderecoModel endereco = new EnderecoModel();
                endereco.setLogradouro((String) dados.get("logradouro"));
                endereco.setBairro((String) dados.get("bairro"));
                endereco.setCidade((String) dados.get("localidade"));
                endereco.setUf((String) dados.get("uf"));
                endereco.setCep((String) dados.get("cep"));

                return endereco;
            }
        }

        return null;  // Caso a resposta não seja válida, retorna null
    }

    public ResponseEntity<ResponseModel> delete(String matricula) {
        ar.deleteById(matricula);
        rm.setMensagem("O aluno foi removido com sucesso");
        return new ResponseEntity<>(rm, HttpStatus.OK);
    }
}