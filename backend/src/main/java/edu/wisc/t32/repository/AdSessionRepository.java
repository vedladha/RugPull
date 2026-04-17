package edu.wisc.t32.repository;

import edu.wisc.t32.model.AdSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for managing {@link AdSession} entities.
 */
@Repository
public interface AdSessionRepository extends CrudRepository<AdSession, String> {
}
