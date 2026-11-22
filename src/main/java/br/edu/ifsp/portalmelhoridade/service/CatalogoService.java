package br.edu.ifsp.portalmelhoridade.service;

import br.edu.ifsp.portalmelhoridade.model.Catalogo;
import br.edu.ifsp.portalmelhoridade.model.Emergencia;
import br.edu.ifsp.portalmelhoridade.model.Item;
import br.edu.ifsp.portalmelhoridade.model.Secao;
import br.edu.ifsp.portalmelhoridade.model.entity.EmergenciaEntity;
import br.edu.ifsp.portalmelhoridade.model.entity.ItemEntity;
import br.edu.ifsp.portalmelhoridade.model.entity.SecaoEntity;
import br.edu.ifsp.portalmelhoridade.repository.EmergenciaRepository;
import br.edu.ifsp.portalmelhoridade.repository.ItemRepository;
import br.edu.ifsp.portalmelhoridade.repository.SecaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Catálogo de serviços públicos do portal, lido do banco de dados via JPA.
 *
 * <p>Mantém exatamente o mesmo contrato de retorno (records {@link Catalogo},
 * {@link Secao}, {@link Item} e {@link Emergencia}) que era fornecido pela
 * versão em memória, garantindo compatibilidade com o {@code PaginaController}
 * e todos os testes existentes.</p>
 *
 * <p>Em produção usa PostgreSQL; em testes usa H2 em memória.</p>
 */
@Service
public class CatalogoService {

    private final SecaoRepository      secaoRepo;
    private final ItemRepository       itemRepo;
    private final EmergenciaRepository emergenciaRepo;

    public CatalogoService(SecaoRepository secaoRepo,
                           ItemRepository itemRepo,
                           EmergenciaRepository emergenciaRepo) {
        this.secaoRepo      = secaoRepo;
        this.itemRepo       = itemRepo;
        this.emergenciaRepo = emergenciaRepo;
    }

    /** Retorna o catálogo completo com seções e emergências. */
    public Catalogo catalogo() {
        return new Catalogo(secoes(), emergencias());
    }

    /** Retorna as seções ordenadas, cada uma com seus itens carregados do banco. */
    public List<Secao> secoes() {
        return secaoRepo.findAllByOrderByOrdemAsc().stream()
                .map(this::toSecao)
                .toList();
    }

    /** Retorna os canais de emergência ordenados. */
    public List<Emergencia> emergencias() {
        return emergenciaRepo.findAllByOrderByOrdemAsc().stream()
                .map(e -> new Emergencia(e.getIcone(), e.getNome(), e.getNumero()))
                .toList();
    }

    // ─── Mapeamento entity → record ────────────────────────────────────────────

    private Secao toSecao(SecaoEntity e) {
        List<Item> itens = itemRepo.findBySecaoIdOrderByOrdemAsc(e.getId()).stream()
                .map(this::toItem)
                .toList();
        return new Secao(e.getId(), e.getTitulo(), e.getSubtitulo(), e.getCor(), e.getIcone(), itens);
    }

    private Item toItem(ItemEntity e) {
        return new Item(
                e.getIcone(),
                e.getTitulo(),
                e.getDescricao(),
                e.getTexto(),
                e.getEndereco(),
                e.getTelefone(),
                e.getHorario(),
                e.getFonteOficial(),
                e.getDataAtualizacao()
        );
    }
}
