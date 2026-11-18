package se.jtutala.handlaggning.arende.service;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.dto.ArendeRadResponse;
import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.repository.ArendeRepository;

/// Användningsfall: **läsa ärenden** – ett enskilt ärende eller handläggarens arbetslista.
@Service
@Transactional(readOnly = true)
public class ArendeSokService {

    private final ArendeHamtare hamtare;
    private final ArendeRepository repository;

    ArendeSokService(ArendeHamtare hamtare, ArendeRepository repository) {
        this.hamtare = hamtare;
        this.repository = repository;
    }

    public ArendeResponse hamta(long id) {
        return ArendeMapper.tillResponse(hamtare.hamta(id));
    }

    /// Filtrera på gäldenär och/eller status (null = alla).
    public List<ArendeRadResponse> arbetslista(@Nullable Long galdenarId, @Nullable Arendestatus status) {
        return repository.sok(galdenarId, status).stream().map(ArendeMapper::tillRad).toList();
    }
}
