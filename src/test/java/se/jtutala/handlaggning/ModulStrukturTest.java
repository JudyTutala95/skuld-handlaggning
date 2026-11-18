package se.jtutala.handlaggning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/// **Arkitekturtest** med Spring Modulith.
///
/// `verify()` misslyckas om:
/// - en modul använder en annan moduls *interna* (package-private eller underpaket) typer,
/// - modulerna har cykliska beroenden (A → B → A).
///
/// Testet gör alltså arkitekturen till något som *kontrolleras*, inte bara dokumenteras.
class ModulStrukturTest {

    ApplicationModules moduler = ApplicationModules.of(HandlaggningApplication.class);

    @Test
    void modulgranserFoljs() {
        moduler.verify();
    }

    @Test
    void forvantadeModulerFinns() {
        assertThat(moduler.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
                .containsExactlyInAnyOrder("arende", "common", "galdenar", "notifiering", "regelverk");
    }

    /// Genererar diagram (PlantUML) och modulbeskrivningar till `target/spring-modulith-docs`.
    @Test
    void skrivDokumentation() {
        new Documenter(moduler).writeDocumentation();
    }
}
