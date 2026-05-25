package br.com.lumilivre.api.architecture;

import java.net.http.HttpClient;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "br.com.lumilivre.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule domainPoliciesAreFrameworkFree = noClasses()
            .that().resideInAPackage("..domain.policy..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..");

    @ArchTest
    static final ArchRule controllersDoNotUseRepositoriesDirectly = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule dtosDoNotDependOnJpaEntities = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAPackage("..model..");

    @ArchTest
    static final ArchRule httpClientsStayInInfraOrConfig = noClasses()
            .that().resideOutsideOfPackages("..service.infra..", "..config..")
            .should().dependOnClassesThat().belongToAnyOf(
                    RestTemplate.class,
                    RestClient.class,
                    HttpClient.class);
}
