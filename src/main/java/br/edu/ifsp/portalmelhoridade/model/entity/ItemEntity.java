package br.edu.ifsp.portalmelhoridade.model.entity;

import jakarta.persistence.*;

/**
 * Entidade JPA que representa um serviço oficial dentro de uma seção do catálogo.
 */
@Entity
@Table(name = "itens")
public class ItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID da seção à qual este item pertence (FK lógica). */
    @Column(name = "secao_id", length = 20, nullable = false)
    private String secaoId;

    @Column(nullable = false)
    private String icone;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String endereco;

    @Column(nullable = false, length = 50)
    private String telefone;

    @Column(nullable = false, length = 200)
    private String horario;

    @Column(name = "fonte_oficial", nullable = false, length = 500)
    private String fonteOficial;

    @Column(name = "data_atualizacao", nullable = false, length = 20)
    private String dataAtualizacao;

    /** Ordem de exibição dentro da seção. */
    @Column(nullable = false)
    private int ordem;

    protected ItemEntity() { }

    public ItemEntity(String secaoId, String icone, String titulo, String descricao,
                      String texto, String endereco, String telefone, String horario,
                      String fonteOficial, String dataAtualizacao, int ordem) {
        this.secaoId        = secaoId;
        this.icone          = icone;
        this.titulo         = titulo;
        this.descricao      = descricao;
        this.texto          = texto;
        this.endereco       = endereco;
        this.telefone       = telefone;
        this.horario        = horario;
        this.fonteOficial   = fonteOficial;
        this.dataAtualizacao = dataAtualizacao;
        this.ordem          = ordem;
    }

    public Long   getId()              { return id; }
    public String getSecaoId()         { return secaoId; }
    public String getIcone()           { return icone; }
    public String getTitulo()          { return titulo; }
    public String getDescricao()       { return descricao; }
    public String getTexto()           { return texto; }
    public String getEndereco()        { return endereco; }
    public String getTelefone()        { return telefone; }
    public String getHorario()         { return horario; }
    public String getFonteOficial()    { return fonteOficial; }
    public String getDataAtualizacao() { return dataAtualizacao; }
    public int    getOrdem()           { return ordem; }
}
