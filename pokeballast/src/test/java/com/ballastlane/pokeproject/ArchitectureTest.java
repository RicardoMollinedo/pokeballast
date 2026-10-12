package com.ballastlane.pokeproject;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The dependency rules of pokeballast/CLAUDE.md ("Project structure"), enforced by the build:
 * the business logic stays independent of the API and of data access.
 */
@AnalyzeClasses(packages = "com.ballastlane.pokeproject", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.ballastlane.pokeproject";

    /** bl orchestrates and maps to the API DTOs, but reaches services and data only through bl.port. */
    @ArchTest
    static final ArchRule blDoesNotDependOnAdaptersOrTheWebLayer = noClasses()
            .that().resideInAPackage(ROOT + ".bl..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".api..", ROOT + ".service..", ROOT + ".repository..", ROOT + ".config..",
                    ROOT + ".security..", ROOT + ".dto.pokeapi..");

    /** Controllers are entry points only: no mapping, no business logic. */
    @ArchTest
    static final ArchRule controllersDoNotMap = noClasses()
            .that().resideInAPackage(ROOT + ".api..")
            .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".mapper..", ROOT + ".bl.model..", ROOT + ".bl.port..");

    /** Services only call the external API: no orchestration through other bl classes or repositories. */
    @ArchTest
    static final ArchRule servicesDoNotOrchestrate = noClasses()
            .that().resideInAPackage(ROOT + ".service..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Bl");

    @ArchTest
    static final ArchRule blUsesNoFrameworkButService = noClasses()
            .that().resideInAPackage(ROOT + ".bl..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.hibernate..", "com.fasterxml.jackson..", "tools.jackson..",
                    "org.springframework.web..", "org.springframework.data..", "org.springframework.transaction..",
                    "org.springframework.security..", "org.springframework.cache..");

    @ArchTest
    static final ArchRule blClassesAreServices = classes()
            .that().resideInAPackage(ROOT + ".bl").and().haveSimpleNameEndingWith("Bl")
            .should().beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule exceptionsArePureJava = noClasses()
            .that().resideInAPackage(ROOT + ".exceptions..")
            .should().dependOnClassesThat().resideOutsideOfPackages("java..", ROOT + ".exceptions..");

    @ArchTest
    static final ArchRule controllersDoNotReachDataOrExternalServices = noClasses()
            .that().resideInAPackage(ROOT + ".api..")
            .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".repository..", ROOT + ".service..");

    @ArchTest
    static final ArchRule serviceDoesNotTouchApiOrRepository = noClasses()
            .that().resideInAPackage(ROOT + ".service..")
            .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".api..", ROOT + ".repository..");

    @ArchTest
    static final ArchRule repositoryDoesNotTouchApiServiceOrDto = noClasses()
            .that().resideInAPackage(ROOT + ".repository..")
            .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".api..", ROOT + ".service..", ROOT + ".dto..");

    @ArchTest
    static final ArchRule securityAdaptersOnlyImplementBlPorts = noClasses()
            .that().resideInAPackage(ROOT + ".security..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".api..", ROOT + ".service..", ROOT + ".repository..", ROOT + ".dto..", ROOT + ".mapper..");

    @ArchTest
    static final ArchRule controllersDoNotReachSecurityAdapters = noClasses()
            .that().resideInAPackage(ROOT + ".api..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".security..");

    @ArchTest
    static final ArchRule pokeApiDtosStayInServiceAndMapper = noClasses()
            .that().resideOutsideOfPackages(ROOT + ".service..", ROOT + ".mapper..", ROOT + ".dto.pokeapi..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".dto.pokeapi..");

    @ArchTest
    static final ArchRule utilDependsOnNoOtherProjectPackage = noClasses()
            .that().resideInAPackage(ROOT + ".util..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".api..", ROOT + ".bl..", ROOT + ".dto..", ROOT + ".service..", ROOT + ".repository..",
                    ROOT + ".mapper..", ROOT + ".config..", ROOT + ".exceptions..", ROOT + ".security..");
}
