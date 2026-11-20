package se.jtutala.handlaggning.galdenar;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/// **Arkitekturtest för lagren inuti varje modul** (ArchUnit).
///
/// ```
/// web ──▶ service ──▶ repository
///  │         │            │
///  ▼         ▼            ▼
/// dto      domain ◀───────┘
/// ```
///
/// Bryter någon mot en regel – t.ex. en controller som anropar ett repository direkt –
/// misslyckas bygget. Gränserna *mellan* tjänsterna behöver inget test längre: tjänsterna
/// ligger i olika Maven-moduler och kan inte se varandras klasser alls.
@AnalyzeClasses(packages = "se.jtutala.handlaggning.galdenar", importOptions = ImportOption.DoNotIncludeTests.class)
class LagerArkitekturTest {

    @ArchTest
    static final ArchRule lagrenAnvandsUppifranOchNed = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Web").definedBy("..web..")
            .layer("Dto").definedBy("..dto..")
            .layer("Service").definedBy("..service..")
            .layer("Domain").definedBy("..domain..")
            .layer("Regelverk").definedBy("..regelverk..")
            .layer("Repository").definedBy("..repository..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            // Regelverket är ren Java som alla lager får använda, men det använder inget annat lager
            .whereLayer("Regelverk").mayNotAccessAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Web")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Repository")
            .whereLayer("Dto").mayOnlyBeAccessedByLayers("Web", "Service");

    @ArchTest
    static final ArchRule domanenKannerInteTillWebbenEllerDtoer = noClasses()
            .that().resideInAnyPackage("..domain..", "..regelverk..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "..dto..", "..web..")
            .because("affärsreglerna ska gå att förstå och testa utan HTTP");

    @ArchTest
    static final ArchRule controllersLiggerIWeb = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..web..")
            .andShould().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule servicesLiggerIService = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage("..service..")
            .andShould().haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule repositoriesLiggerIRepository = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("..repository..")
            .andShould().beInterfaces();

    @ArchTest
    static final ArchRule dtoerArRequestEllerResponse = classes()
            .that().resideInAPackage("..dto..")
            .should().beRecords()
            .andShould().haveSimpleNameEndingWith("Request").orShould().haveSimpleNameEndingWith("Response");
}
