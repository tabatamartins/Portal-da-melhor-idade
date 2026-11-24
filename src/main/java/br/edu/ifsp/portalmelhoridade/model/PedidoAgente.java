package br.edu.ifsp.portalmelhoridade.model;

import java.util.List;

/** Comando do usuário (texto ou voz já transcrita), estado da fonte e histórico recente. */
public record PedidoAgente(String texto, int nivelFonte, List<String> historico) {
    public PedidoAgente(String texto, int nivelFonte) {
        this(texto, nivelFonte, List.of());
    }
}
