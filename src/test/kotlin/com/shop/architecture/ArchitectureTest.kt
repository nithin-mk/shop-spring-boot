package com.shop.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service

@AnalyzeClasses(packages = ["com.shop"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {
    @ArchTest
    val modelsShouldNotDependOnOuterLayers: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..model..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..controller..", "..service..", "..repository..")

    @ArchTest
    val repositoriesShouldNotDependOnControllersOrServices: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..repository..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..controller..", "..service..")

    @ArchTest
    val servicesShouldNotDependOnControllers: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..controller..")

    @ArchTest
    val classesNamedControllerShouldResideInControllerPackage: ArchRule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .resideInAPackage("..controller..")

    @ArchTest
    val classesNamedServiceShouldResideInServicePackage: ArchRule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Service")
            // ShopUserDetailsService implements Spring Security's UserDetailsService
            // contract and belongs with the rest of the security setup, not the
            // business-logic service layer — "Service" here is Spring's naming
            // convention for that interface, not this project's layer naming.
            .and()
            .resideOutsideOfPackage("..security..")
            .should()
            .resideInAPackage("..service..")

    @ArchTest
    val classesNamedRepositoryShouldResideInRepositoryPackage: ArchRule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .resideInAPackage("..repository..")

    @ArchTest
    val controllersShouldBeAnnotatedWithController: ArchRule =
        classes()
            .that()
            .resideInAPackage("..controller..")
            .and()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .beAnnotatedWith(Controller::class.java)

    @ArchTest
    val servicesShouldBeAnnotatedWithService: ArchRule =
        classes()
            .that()
            .resideInAPackage("..service..")
            .and()
            .areNotInterfaces()
            // Kotlin compiles a class's `companion object` to a separate nested
            // `$Companion` class, which doesn't (and can't meaningfully) carry
            // its own @Service annotation.
            .and()
            .haveSimpleNameNotEndingWith("Companion")
            .should()
            .beAnnotatedWith(Service::class.java)

    @ArchTest
    val packagesShouldBeFreeOfCycles: ArchRule =
        slices().matching("com.shop.(*)..").should().beFreeOfCycles()
}
