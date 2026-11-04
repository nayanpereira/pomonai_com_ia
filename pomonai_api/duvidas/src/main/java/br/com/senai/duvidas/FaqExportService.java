package br.com.senai.duvidas;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

/**
 * Serviço responsável por buscar as dúvidas cadastradas, montar o contexto
 * do Thymeleaf e orquestrar a conversão para arquivo PDF.
 */
@Service
public class FaqExportService {

    private final DuvidaRepository duvidaRepository;
    private final TemplateEngine templateEngine;
    private final PdfGeneratorService pdfGeneratorService;

    public FaqExportService(DuvidaRepository duvidaRepository,
                            TemplateEngine templateEngine,
                            PdfGeneratorService pdfGeneratorService) {
        this.duvidaRepository = duvidaRepository;
        this.templateEngine = templateEngine;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    /**
     * Busca todas as dúvidas do banco de dados, renderiza o template HTML e gera o PDF.
     *
     * @return array de bytes contendo o PDF gerado
     */
    public byte[] gerarFaqPdf() {
        // 1. Busca as dúvidas cadastradas no banco ordenadas pelas mais recentes
        List<Duvida> duvidas = duvidaRepository.findAllByOrderByDataHoraDesc();

        // 2. Prepara o contexto de variáveis para o Thymeleaf
        Context context = new Context();
        context.setVariable("duvidas", duvidas);

        // 3. Renderiza o template faq-pdf.html com as variáveis
        String htmlRenderizado = templateEngine.process("faq-pdf", context);

        // 4. Converte o HTML resultante em PDF
        return pdfGeneratorService.generatePdfFromHtml(htmlRenderizado);
    }
}
