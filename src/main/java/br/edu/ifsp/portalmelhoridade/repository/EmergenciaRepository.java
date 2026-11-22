package br.edu.ifsp.portalmelhoridade.repository;

import br.edu.ifsp.portalmelhoridade.model.entity.EmergenciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositório JPA para {@link EmergenciaEntity}. */
public interface EmergenciaRepository extends JpaRepository<EmergenciaEntity, Long> {
    List<EmergenciaEntity> findAllByOrderByOrdemAsc();
}
