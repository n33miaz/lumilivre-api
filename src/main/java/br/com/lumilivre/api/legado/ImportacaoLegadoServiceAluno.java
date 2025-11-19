package br.com.lumilivre.api.legado;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.*;
import br.com.lumilivre.api.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ImportacaoLegadoServiceAluno {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoLegadoServiceAluno.class);
    private static final int BATCH_SIZE = 50;

    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final ModuloRepository moduloRepository;
    private final TurnoRepository turnoRepository;
    private final PasswordEncoder passwordEncoder;

    public ImportacaoLegadoServiceAluno(
            AlunoRepository alunoRepository,
            UsuarioRepository usuarioRepository,
            CursoRepository cursoRepository,
            ModuloRepository moduloRepository,
            TurnoRepository turnoRepository,
            PasswordEncoder passwordEncoder) {
        this.alunoRepository = alunoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.moduloRepository = moduloRepository;
        this.turnoRepository = turnoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String importarAlunos(MultipartFile file) throws Exception {
        List<AlunoModel> alunosParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> matriculasNoExcel = new HashSet<>();
        Set<String> cpfsNoExcel = new HashSet<>();
        Set<String> emailsNoExcel = new HashSet<>();

        // Cache para evitar múltiplas buscas no banco dentro do loop
        Set<String> matriculasExistentes = alunoRepository.findAllMatriculas();
        Set<String> cpfsExistentes = alunoRepository.findAllCpfs();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMap = mapearCabecalhos(sheet.getRow(0));

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue; // Pula o cabeçalho

                int linhaNum = row.getRowNum() + 1;
                try {
                    String matricula = ExcelUtils.getString(row.getCell(headerMap.get("matricula")));
                    if (matricula == null || matricula.isBlank()) {
                        logErros.add(new ErroImportacao(linhaNum, "Matrícula é obrigatória."));
                        continue;
                    }

                    // Validações de duplicidade
                    if (!matriculasNoExcel.add(matricula)) {
                        logErros.add(new ErroImportacao(linhaNum, "Matrícula duplicada na planilha: " + matricula));
                        continue;
                    }
                    if (matriculasExistentes.contains(matricula)) {
                        logErros.add(
                                new ErroImportacao(linhaNum, "Matrícula já existe no banco de dados: " + matricula));
                        continue;
                    }

                    String cpf = ExcelUtils.getString(row.getCell(headerMap.get("cpf")));
                    if (cpf != null && !cpf.isBlank()) {
                        if (!cpfsNoExcel.add(cpf)) {
                            logErros.add(new ErroImportacao(linhaNum, "CPF duplicado na planilha: " + cpf));
                            continue;
                        }
                        if (cpfsExistentes.contains(cpf)) {
                            logErros.add(new ErroImportacao(linhaNum, "CPF já existe no banco de dados: " + cpf));
                            continue;
                        }
                    }

                    String email = ExcelUtils.getString(row.getCell(headerMap.get("email")));
                    if (email != null && !email.isBlank()) {
                        if (!emailsNoExcel.add(email)) {
                            logErros.add(new ErroImportacao(linhaNum, "Email duplicado na planilha: " + email));
                            continue;
                        }
                        if (usuarioRepository.existsByEmail(email)) {
                            logErros.add(new ErroImportacao(linhaNum, "Email já existe no banco de dados: " + email));
                            continue;
                        }
                    }

                    AlunoModel aluno = criarAlunoFromRow(row, headerMap);
                    UsuarioModel usuario = criarUsuarioParaAluno(aluno);
                    aluno.setUsuario(usuario); // Associa o usuário ao aluno

                    alunosParaSalvar.add(aluno);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, e.getMessage()));
                }
            }
        }

        // Salva em lotes
        int totalSalvos = 0;
        for (int i = 0; i < alunosParaSalvar.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, alunosParaSalvar.size());
            List<AlunoModel> lote = alunosParaSalvar.subList(i, end);
            try {
                alunoRepository.saveAll(lote);
                totalSalvos += lote.size();
            } catch (Exception e) {
                logErros.add(
                        new ErroImportacao(-1, "Erro ao salvar lote " + (i / BATCH_SIZE + 1) + ": " + e.getMessage()));
            }
        }

        return gerarResumoImportacao("alunos", totalSalvos, logErros);
    }

    private AlunoModel criarAlunoFromRow(Row row, Map<String, Integer> headerMap) {
        AlunoModel aluno = new AlunoModel();

        aluno.setMatricula(ExcelUtils.getString(row.getCell(headerMap.get("matricula"))));
        aluno.setNomeCompleto(ExcelUtils.getString(row.getCell(headerMap.get("nome_completo"))));
        aluno.setCpf(ExcelUtils.getString(row.getCell(headerMap.get("cpf"))));
        aluno.setCelular(ExcelUtils.getString(row.getCell(headerMap.get("celular"))));
        aluno.setDataNascimento(ExcelUtils.getLocalDate(row.getCell(headerMap.get("data_nascimento"))));
        aluno.setEmail(ExcelUtils.getString(row.getCell(headerMap.get("email"))));

        Integer emprestimosCount = ExcelUtils.getInteger(row.getCell(headerMap.get("emprestimos_count")));
        aluno.setEmprestimosCount(emprestimosCount != null ? emprestimosCount : 0);

        // Busca as entidades relacionadas pelo ID
        Integer cursoId = ExcelUtils.getInteger(row.getCell(headerMap.get("curso_id")));
        CursoModel curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso com ID " + cursoId + " não encontrado."));
        aluno.setCurso(curso);

        Integer moduloId = ExcelUtils.getInteger(row.getCell(headerMap.get("modulo_id")));
        ModuloModel modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new IllegalArgumentException("Módulo com ID " + moduloId + " não encontrado."));
        aluno.setModulo(modulo);

        Integer turnoId = ExcelUtils.getInteger(row.getCell(headerMap.get("turno_id")));
        TurnoModel turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno com ID " + turnoId + " não encontrado."));
        aluno.setTurno(turno);

        return aluno;
    }

    private UsuarioModel criarUsuarioParaAluno(AlunoModel aluno) {
        UsuarioModel usuario = new UsuarioModel();
        usuario.setEmail(aluno.getEmail());
        // Login e Senha são a matrícula
        usuario.setSenha(passwordEncoder.encode(aluno.getMatricula()));
        usuario.setRole(Role.ALUNO);
        usuario.setAluno(aluno);
        return usuario;
    }

    private Map<String, Integer> mapearCabecalhos(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) {
            throw new IllegalArgumentException("A planilha está vazia ou não possui uma linha de cabeçalho.");
        }
        for (Cell cell : headerRow) {
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String headerText = cell.getStringCellValue().trim().toLowerCase();
                map.put(headerText, cell.getColumnIndex());
            }
        }
        return map;
    }

    private String gerarResumoImportacao(String tipo, int totalSalvos, List<ErroImportacao> logErros) {
        String resumo = String.format("Importação de %s concluída. Salvos: %d | Erros: %d", tipo, totalSalvos,
                logErros.size());
        if (!logErros.isEmpty()) {
            String detalhes = logErros.stream()
                    .map(ErroImportacao::toString)
                    .limit(10) // Limita para não poluir a resposta
                    .collect(Collectors.joining("; "));
            resumo += " | Primeiros erros: " + detalhes;
        }
        log.info(resumo);
        return resumo;
    }

    private static class ErroImportacao {
        private final int linha;
        private final String detalhe;

        public ErroImportacao(int linha, String detalhe) {
            this.linha = linha;
            this.detalhe = detalhe;
        }

        @Override
        public String toString() {
            return (linha > 0 ? "Linha " + linha + ": " : "") + detalhe;
        }
    }
}