package br.edu.ifsp.portalmelhoridade.controller;

import br.edu.ifsp.portalmelhoridade.model.PedidoAgente;
import br.edu.ifsp.portalmelhoridade.model.RespostaAgente;
import br.edu.ifsp.portalmelhoridade.service.AgenteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/agente")
public class AgenteController {

    private static final int LIMITE_POR_MINUTO = 20;
    private static final long JANELA_MS = 60_000L;
    private final ConcurrentHashMap<String, JanelaUso> usoPorCliente = new ConcurrentHashMap<>();
    private final AgenteService agente;

    public AgenteController(AgenteService agente) {
        this.agente = agente;
    }

    @PostMapping
    public ResponseEntity<RespostaAgente> interpretar(@RequestBody PedidoAgente pedido,
                                                       HttpServletRequest request) {
        String cliente = request.getRemoteAddr();
        long agora = Instant.now().toEpochMilli();
        JanelaUso janela = usoPorCliente.compute(cliente, (chave, atual) ->
                atual == null || agora - atual.inicio() >= JANELA_MS
                        ? new JanelaUso(agora, 1)
                        : new JanelaUso(atual.inicio(), atual.quantidade() + 1));

        if (janela.quantidade() > LIMITE_POR_MINUTO) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.ok(agente.processar(pedido));
    }

    private record JanelaUso(long inicio, int quantidade) { }
}
