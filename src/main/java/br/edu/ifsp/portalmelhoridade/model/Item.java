package br.edu.ifsp.portalmelhoridade.model;

/**
 * Serviço listado dentro de uma seção com dados detalhados e oficiais de atendimento.
 */
public record Item(String icone,
                   String titulo,
                   String descricao,
                   String texto,
                   String endereco,
                   String telefone,
                   String horario,
                   String fonteOficial,
                   String dataAtualizacao) { }

