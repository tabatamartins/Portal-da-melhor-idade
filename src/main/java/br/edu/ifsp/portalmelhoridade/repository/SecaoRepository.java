package br.edu.ifsp.portalmelhoridade.repository;

import br.edu.ifsp.portalmelhoridade.model.entity.SecaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositório JPA para {@link SecaoEntity}. */
public interface SecaoRepository extends JpaRepository<SecaoEntity, String> {
    List<SecaoEntity> findAllByOrderByOrdemAsc();
}
