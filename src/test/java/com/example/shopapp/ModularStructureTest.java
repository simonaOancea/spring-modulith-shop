package com.example.shopapp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularStructureTest {

    static final ApplicationModules modules = ApplicationModules.of(ShopApplication.class);

    @Test
    void verifyModularStructure() {
        modules.verify();
    }

    @Test
    void printModuleStructure() {
        modules.forEach(System.out::println);
    }

    @Test
    void createModuleDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
