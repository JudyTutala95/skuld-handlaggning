package se.jtutala.handlaggning.arende;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ArendeRepository extends JpaRepository<Arende, Long> {

    /// Handläggarens arbetslista: filtrera på gäldenär och/eller status (null = alla).
    @Query("""
            select a from Arende a
            where (:galdenarId is null or a.galdenarId = :galdenarId)
              and (:status is null or a.status = :status)
            order by a.registrerad desc, a.id desc
            """)
    List<Arende> sok(@Nullable Long galdenarId, @Nullable Arendestatus status);
}
