package se.jtutala.handlaggning.arende;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// Användningsfall: **läsa ärenden** – ett enskilt ärende eller handläggarens arbetslista.
@Service
@Transactional(readOnly = true)
public class ArendeUppslag {

    private final Arendelager lager;
    private final ArendeRepository repository;

    ArendeUppslag(Arendelager lager, ArendeRepository repository) {
        this.lager = lager;
        this.repository = repository;
    }

    public ArendeSvar hamta(long id) {
        return ArendeSvarMapper.tillSvar(lager.hamta(id));
    }

    /// Filtrera på gäldenär och/eller status (null = alla).
    public List<ArendeRad> arbetslista(@Nullable Long galdenarId, @Nullable Arendestatus status) {
        return repository.sok(galdenarId, status).stream().map(ArendeSvarMapper::tillRad).toList();
    }
}
