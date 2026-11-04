package br.com.senai.duvidas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.springframework.stereotype.Service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

/**
 * Serviço responsável por converter strings HTML em arquivos PDF utilizando OpenHTMLtoPDF.
 * Utiliza o Jsoup para garantir que o HTML seja convertido em XHTML bem-formado.
 */
@Service
public class PdfGeneratorService {

    public byte[] generatePdfFromHtml(String htmlContent) {
        // 1. Normaliza o HTML para XHTML estrito (requisito do OpenHTMLtoPDF)
        Document document = Jsoup.parse(htmlContent);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");
        String xhtmlContent = document.html();

        // 2. Renderiza o XHTML em um fluxo de bytes PDF
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtmlContent, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar o arquivo PDF a partir do HTML", e);
        }
    }
}