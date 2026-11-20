package se.jtutala.handlaggning.galdenar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import se.jtutala.handlaggning.galdenar.domain.Galdenar;
import se.jtutala.handlaggning.galdenar.domain.Personnummer;

/// Databasåtkomst för gäldenärer. Används bara av service-lagret i den här modulen
/// (Spring Modulith hindrar andra moduler, ArchUnit hindrar controllers).
public interface GaldenarRepository extends JpaRepository<Galdenar, Long> {

    Optional<Galdenar> findByPersonnummer(Personnummer personnummer);

    boolean existsByPersonnummer(Personnummer personnummer);
}
