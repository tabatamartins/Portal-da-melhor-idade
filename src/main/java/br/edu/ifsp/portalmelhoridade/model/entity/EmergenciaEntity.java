package br.edu.ifsp.portalmelhoridade.model.entity;

import jakarta.persistence.*;

/**
 * Entidade JPA que representa um canal de emergência do portal.
 */
@Entity
@Table(name = "emergencias")
public class EmergenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String icone;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 10)
    private String numero;

    /** Ordem de exibição no painel de emergência. */
    @Column(nullable = false)
    private int ordem;

    protected EmergenciaEntity() { }

    public EmergenciaEntity(String icone, String nome, String numero, int ordem) {
        this.icone  = icone;
        this.nome   = nome;
        this.numero = numero;
        this.ordem  = ordem;
    }

    public Long   getId()    { return id; }
    public String getIcone() { return icone; }
    public String getNome()  { return nome; }
    public String getNumero(){ return numero; }
    public int    getOrdem() { return ordem; }
}
