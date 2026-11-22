package br.edu.ifsp.portalmelhoridade.repository;

import br.edu.ifsp.portalmelhoridade.model.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositório JPA para {@link ItemEntity}. */
public interface ItemRepository extends JpaRepository<ItemEntity, Long> {
    List<ItemEntity> findBySecaoIdOrderByOrdemAsc(String secaoId);
}
