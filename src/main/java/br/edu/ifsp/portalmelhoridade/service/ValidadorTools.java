package br.edu.ifsp.portalmelhoridade.service;

import br.edu.ifsp.portalmelhoridade.model.ChamadaTool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Whitelist de ferramentas e validação de parâmetros (POL01, RNF02).
 * Qualquer coisa fora do contrato é bloqueada, mesmo que venha "do modelo".
 */
@Component
public class ValidadorTools {

    public static final int FONTE_MIN = -2;
    public static final int FONTE_MAX = 4;
    public static final Set<String> SECOES = Set.of("saude", "direitos", "emergencia", "lazer");
    private static final Set<String> WHITELIST = Set.of(
            "ajustarFonte", "alterarContraste", "navegarParaSecao", "executarLeituraVoz");

    /** @return mensagem de erro, ou vazio se a chamada é válida. */
    public Optional<String> validar(ChamadaTool chamada, int nivelAtual) {
        if (chamada == null || chamada.name() == null || !WHITELIST.contains(chamada.name())) {
            return Optional.of("ferramenta fora da whitelist");
        }
        Map<String, Object> args = chamada.arguments() == null ? Map.of() : chamada.arguments();

        switch (chamada.name()) {
            case "ajustarFonte" -> {
                Object val = args.get("delta");
                if (!(val instanceof Number num)) {
                    return Optional.of("delta inválido");
                }
                if (num.doubleValue() != num.intValue()) {
                    return Optional.of("delta deve ser um número inteiro");
                }
                int delta = num.intValue();
                if (Math.abs(delta) > 2) {
                    return Optional.of("delta inválido");
                }
                int novo = nivelAtual + delta;
                if (novo < FONTE_MIN || novo > FONTE_MAX) {
                    return Optional.of("fora do intervalo [" + FONTE_MIN + ", +" + FONTE_MAX + "]");
                }
            }
            case "alterarContraste" -> {
                if (!(args.get("ativo") instanceof Boolean)) {
                    return Optional.of("parâmetro 'ativo' inválido");
                }
            }
            case "navegarParaSecao", "executarLeituraVoz" -> {
                if (!(args.get("idSecao") instanceof String id) || !SECOES.contains(id)) {
                    return Optional.of("seção inexistente");
                }
            }
            default -> { return Optional.of("ferramenta fora da whitelist"); }
        }
        return Optional.empty();
    }
}
