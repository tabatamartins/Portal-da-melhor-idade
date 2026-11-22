package br.edu.ifsp.portalmelhoridade.model;

/** Comando do usuário (texto ou voz já transcrita) e o estado atual da fonte. */
public record PedidoAgente(String texto, int nivelFonte) { }
