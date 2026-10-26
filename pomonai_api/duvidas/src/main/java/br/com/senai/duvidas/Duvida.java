package br.com.senai.duvidas;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Classe de Entidade (representa uma tabela)
@Table(name = "duvida")
@Entity(name = "Duvida")
public class Duvida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Column(name = "datahora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "tipo_pausa", nullable = false, length = 10)
    private String tipoPausa;

    @Column(name = "resposta_ia", columnDefinition = "TEXT")
    private String respostaIa;

    @Column(name = "resposta_professor", columnDefinition = "TEXT")
    private String respostaProfessor;

    public Duvida() {
    }

    public Duvida(DadosCadastroDuvida dados) {
        this.texto = dados.texto();
        this.tipoPausa = dados.tipoPausa();
        this.dataHora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public String getTipoPausa() { return tipoPausa; }
    public void setTipoPausa(String tipoPausa) { this.tipoPausa = tipoPausa; }

    public String getRespostaIa() { return respostaIa; }
    public void setRespostaIa(String respostaIa) { this.respostaIa = respostaIa; }

    public String getRespostaProfessor() { return respostaProfessor; }
    public void setRespostaProfessor(String respostaProfessor) { this.respostaProfessor = respostaProfessor; }
}