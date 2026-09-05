package com.softprimesolutions.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class CleanArchitectureTest {

    private static final String ROOT_PACKAGE = "com.softprimesolutions";
    private static final String SECURITY_PACKAGE = "com.softprimesolutions.security";

    @Test
    void domainDoesNotDependOnOuterLayersOrFrameworks() {
        var classes = productionClasses();

        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..application..",
                        "..api..",
                        "..infrastructure..",
                        "..adapter..",
                        "org.springframework..",
                        "jakarta.persistence..")
                .because("el dominio debe permanecer independiente de capas externas y frameworks")
                .check(classes);
    }

    @Test
    void applicationDoesNotDependOnInfrastructureOrAdapters() {
        var classes = productionClasses();

        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure..", "..adapter..")
                .because("Clean Architecture exige que las dependencias apunten hacia adentro")
                .check(classes);
    }

    @Test
    void securityApplicationDoesNotDependOnItsApi() {
        var classes = productionClasses();

        noClasses()
                .that().resideInAPackage(SECURITY_PACKAGE + ".application..")
                .should().dependOnClassesThat().resideInAPackage(SECURITY_PACKAGE + ".api..")
                .because("la API es un adaptador de entrada y no puede ser conocida por Application")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void apiDoesNotDependOnInfrastructure() {
        var classes = productionClasses();

        noClasses()
                .that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("la API debe invocar puertos de Application y no implementaciones técnicas")
                .check(classes);
    }

    @Test
    void controllersDoNotDependOnPersistenceRepositories() {
        var classes = productionClasses();

        noClasses()
                .that().resideInAPackage("..api.controller..")
                .should().dependOnClassesThat().resideInAPackage(
                        "..infrastructure.persistence..repository..")
                .because("los controllers deben invocar puertos de entrada, no repositorios")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void persistenceReadAndWriteModelsRemainIndependent() {
        var classes = productionClasses();

        var readDoesNotDependOnWrite = noClasses()
                .that().resideInAPackage("..infrastructure.persistence.read..")
                .should().dependOnClassesThat().resideInAPackage(
                        "..infrastructure.persistence.write..")
                .because("el modelo CQRS de lectura no debe depender del modelo de escritura")
                .allowEmptyShould(true);

        var writeDoesNotDependOnRead = noClasses()
                .that().resideInAPackage("..infrastructure.persistence.write..")
                .should().dependOnClassesThat().resideInAPackage(
                        "..infrastructure.persistence.read..")
                .because("el modelo CQRS de escritura no debe depender del modelo de lectura")
                .allowEmptyShould(true);

        readDoesNotDependOnWrite.check(classes);
        writeDoesNotDependOnRead.check(classes);
    }

    @Test
    void persistenceEntitiesDoNotLeakOutsideInfrastructure() {
        var classes = productionClasses();

        noClasses()
                .that().resideOutsideOfPackage("..infrastructure..")
                .should().dependOnClassesThat().resideInAPackage(
                        "..infrastructure.persistence.write.entity..")
                .because("las entidades de persistencia deben permanecer dentro de Infrastructure")
                .allowEmptyShould(true)
                .check(classes);
    }

    private static com.tngtech.archunit.core.domain.JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT_PACKAGE);
    }
}
