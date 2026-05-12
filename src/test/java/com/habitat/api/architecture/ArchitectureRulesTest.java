package com.habitat.api.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architectural rules enforced on every `mvn verify`. Failures here block the build.
 *
 * Every rule traces to a backroom-api incident or our development-standards.md.
 */
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.habitat.api");
    }

    // ── Layering ───────────────────────────────────────────────────────────

    @Test
    void controllers_only_called_by_themselves() {
        ArchRule rule = noClasses().that().resideInAPackage("..service..")
                .or().resideInAPackage("..repository..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");
        rule.check(classes);
    }

    @Test
    void services_do_not_depend_on_controllers() {
        ArchRule rule = noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");
        rule.check(classes);
    }

    @Test
    void repositories_only_used_from_services() {
        ArchRule rule = noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..");
        rule.check(classes);
    }

    // ── Naming ─────────────────────────────────────────────────────────────

    @Test
    void controllers_have_RestController_annotation() {
        ArchRule rule = classes().that().resideInAPackage("..controller..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class);
        rule.check(classes);
    }

    @Test
    void services_have_Service_annotation() {
        ArchRule rule = classes().that().resideInAPackage("..service..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class);
        rule.check(classes);
    }

    // ── Exception discipline ───────────────────────────────────────────────

    @Test
    void no_bare_RuntimeException_thrown_anywhere() {
        // SonarQube + the pre-commit hook also catch this; this is the third line.
        ArchRule rule = noClasses().that().resideOutsideOfPackages("..exception..")
                .should().callConstructor(RuntimeException.class, String.class)
                .orShould().callConstructor(RuntimeException.class, String.class, Throwable.class);
        rule.check(classes);
    }

    // ── DTOs ───────────────────────────────────────────────────────────────

    @Test
    void controllers_dont_return_entities() {
        ArchRule rule = noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..entity..")
                .orShould().dependOnClassesThat().resideInAPackage("..entity.base..");
        rule.because("Use DTOs on the wire — never expose JPA entities.");
        // Note: services may still touch entity packages.
        rule.check(classes);
    }
}
