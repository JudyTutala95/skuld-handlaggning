package se.jtutala.handlaggning.galdenar;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/// Package-private: bara den här modulen får läsa och skriva gäldenärer i databasen.
interface GaldenarRepository extends JpaRepository<Galdenar, Long> {

    Optional<Galdenar> findByPersonnummer(Personnummer personnummer);

    boolean existsByPersonnummer(Personnummer personnummer);
}
