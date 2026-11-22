package br.edu.ifsp.portalmelhoridade.model.entity;

import jakarta.persistence.*;

/**
 * Entidade JPA que representa uma seção do catálogo (saude, direitos, emergencia, lazer).
 */
@Entity
@Table(name = "secoes")
public class SecaoEntity {

    @Id
    @Column(length = 20, nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String subtitulo;

    /** Classe de cor do cabeçalho: "", "hi", "ok" ou "em". */
    @Column(nullable = false)
    private String cor;

    @Column(nullable = false)
    private String icone;

    /** Ordem de exibição no portal (0 = primeiro). */
    @Column(nullable = false)
    private int ordem;

    protected SecaoEntity() { }

    public SecaoEntity(String id, String titulo, String subtitulo, String cor, String icone, int ordem) {
        this.id = id;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.cor = cor;
        this.icone = icone;
        this.ordem = ordem;
    }

    public String getId()       { return id; }
    public String getTitulo()   { return titulo; }
    public String getSubtitulo(){ return subtitulo; }
    public String getCor()      { return cor; }
    public String getIcone()    { return icone; }
    public int    getOrdem()    { return ordem; }
}
