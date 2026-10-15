package com.erasmos.stubborncontractdemo.loans;

import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;
import java.util.List;

// Generated test methods can't carry @DisplayName, so derive one from the method name:
// validate_returns_the_book_when_it_exists -> "returns the book when it exists."
class ContractDisplayNames extends DisplayNameGenerator.Standard {
    @Override
    public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
        String name = testMethod.getName().replaceFirst("^validate_", "").replace('_', ' ');
        return name + ".";
    }
}