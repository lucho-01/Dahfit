package com.dafit.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reglas de arquitectura hexagonal, verificadas en cada build.
 *
 * <p>Sin esto, la estructura de paquetes se degrada sola: alguien importa
 * {@code @Service} en el dominio "solo esta vez" y en tres meses el hexagono
 * es decorativo. Las reglas se ejecutan sobre bytecode, no sobre convenciones.
 */
class HexagonalArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.dafit");
    }

    @Test
    void domainDoesNotDependOnFrameworks() {
        noClasses()
                .that().resideInAPackage("com.dafit.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "com.fasterxml.jackson..",
                        "org.hibernate..",
                        "org.slf4j..")
                .because("el dominio es Java puro: si necesita un framework, la logica esta en el sitio equivocado")
                .check(classes);
    }

    @Test
    void domainOnlyDependsOnItselfAndTheJdk() {
        classes()
                .that().resideInAPackage("com.dafit.domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..",
                        "com.dafit.domain..")
                .because("el dominio no puede mirar hacia application ni infrastructure")
                .check(classes);
    }

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("com.dafit.application..")
                .should().dependOnClassesThat().resideInAPackage("com.dafit.infrastructure..")
                .because("application orquesta el dominio; no conoce controladores ni bases de datos")
                .check(classes);
    }

    @Test
    void inputPortsAreInterfaces() {
        classes()
                .that().resideInAPackage("com.dafit.domain.port.in")
                .and().haveSimpleNameEndingWith("UseCase")
                .should().beInterfaces()
                .because("un puerto de entrada es un contrato, no una implementacion")
                .check(classes);
    }

    @Test
    void outputPortsAreInterfaces() {
        classes()
                .that().resideInAPackage("com.dafit.domain.port.out")
                .and().haveSimpleNameEndingWith("Repository")
                .should().beInterfaces()
                .because("un puerto de salida es un contrato; el adaptador concreto vive en infrastructure")
                .check(classes);
    }

    @Test
    void useCasesImplementAnInputPort() {
        classes()
                .that().resideInAPackage("com.dafit.application.usecase")
                .should(implementPortIn("com.dafit.domain.port.in"))
                .because("un caso de uso sin puerto de entrada no lo puede invocar nadie")
                .check(classes);
    }

    @Test
    void persistenceAdaptersImplementAnOutputPort() {
        classes()
                .that().resideInAPackage("com.dafit.infrastructure.adapter.out.persistence.adapter")
                .should(implementPortIn("com.dafit.domain.port.out"))
                .because("un adaptador suelto no es un adaptador: no implementa ningun contrato")
                .check(classes);
    }

    private static ArchCondition<JavaClass> implementPortIn(String portPackage) {
        return new ArchCondition<>("implementar un puerto de " + portPackage) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean implementsPort = item.getAllRawInterfaces().stream()
                        .anyMatch(iface -> iface.getPackageName().equals(portPackage));

                events.add(implementsPort
                        ? SimpleConditionEvent.satisfied(item,
                                item.getName() + " implementa un puerto de " + portPackage)
                        : SimpleConditionEvent.violated(item,
                                item.getName() + " no implementa ningun puerto de " + portPackage));
            }
        };
    }
}
