package br.edu.ifsp.portalmelhoridade.model;

import java.util.List;

/**
 * Seção do portal. {@code cor} é a classe de cor do cabeçalho ("", "hi", "ok" ou "em").
 * A seção de emergência não tem itens: usa a lista de {@link Emergencia}.
 */
public record Secao(String id, String titulo, String subtitulo, String cor, String icone, List<Item> itens) { }
