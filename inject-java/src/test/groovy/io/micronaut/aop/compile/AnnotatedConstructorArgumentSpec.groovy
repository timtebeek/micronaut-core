/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.aop.compile

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.aop.InterceptorBinding
import io.micronaut.aop.simple.Mutating
import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.AnnotationUtil
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.InstantiatableBeanDefinition
import io.micronaut.inject.writer.BeanDefinitionWriter
/**
 * @author graemerocher
 * @since 1.0
 */
class AnnotatedConstructorArgumentSpec extends AbstractTypeElementSpec{


    void "test that constructor arguments propagate annotation metadata"() {
        when:
        BeanDefinition beanDefinition = buildBeanDefinition('test.$MyBean' + BeanDefinitionWriter.CLASS_SUFFIX + BeanDefinitionWriter.PROXY_SUFFIX, '''
package test;

import io.micronaut.aop.simple.*;
import io.micronaut.context.annotation.*;

@Mutating("someVal")
@jakarta.inject.Singleton
class MyBean {

    private String myValue;

    MyBean(@Value("${foo.bar}") String val) {
        this.myValue = val;
    }

    public String someMethod(String someVal) {
        return myValue + ' ' + someVal;
    }

    String someMethodPackagePrivateMethod(String someVal) {
        return myValue + ' ' + someVal;
    }
}
''')
        then:
        !beanDefinition.isAbstract()
        beanDefinition != null
        beanDefinition.injectedFields.size() == 0
        beanDefinition.constructor.arguments.size() == 5
        beanDefinition.constructor.arguments[0].name == 'val'
        beanDefinition.constructor.arguments[1].name == '$beanResolutionContext'
        beanDefinition.constructor.arguments[2].name == '$beanContext'
        beanDefinition.constructor.arguments[3].name == '$qualifier'
        beanDefinition.constructor.arguments[4].name == '$interceptors'
        beanDefinition.constructor.arguments[4]
                .annotationMetadata
                .getAnnotation(AnnotationUtil.ANN_INTERCEPTOR_BINDING_QUALIFIER)
                .getAnnotations(AnnotationMetadata.VALUE_MEMBER, InterceptorBinding)[0]
                .stringValue().get() == Mutating.name

        when:
        def context = ApplicationContext.run('foo.bar':'test')
        def instance = ((InstantiatableBeanDefinition)beanDefinition).instantiate(context)


        then:
        instance.someMethod("foo") == 'test changed'
        instance.someMethodPackagePrivateMethod("foo") == 'test changed'
    }


    void "test that constructor arguments propagate annotation metadata - method level AOP"() {
        when:
        BeanDefinition beanDefinition = buildBeanDefinition('test.$MyBean' + BeanDefinitionWriter.CLASS_SUFFIX + BeanDefinitionWriter.PROXY_SUFFIX, '''
package test;

import io.micronaut.aop.simple.*;
import io.micronaut.context.annotation.*;


@jakarta.inject.Singleton
class MyBean {

    private String myValue;

    MyBean(@Value("${foo.bar}") String val) {
        this.myValue = val;
    }

    @Mutating("someVal")
    public String someMethod(String someVal) {
        return myValue+ ' ' + someVal;
    }

    @Mutating("someVal")
    String someMethodPackagePrivateMethod(String someVal) {
        return myValue + ' ' + someVal;
    }
}
''')
        then:
        !beanDefinition.isAbstract()
        beanDefinition != null
        beanDefinition.injectedFields.size() == 0
        beanDefinition.constructor.arguments.size() == 5
        beanDefinition.constructor.arguments[0].name == 'val'
        beanDefinition.constructor.arguments[1].name == '$beanResolutionContext'
        beanDefinition.constructor.arguments[2].name == '$beanContext'
        beanDefinition.constructor.arguments[3].name == '$qualifier'
        beanDefinition.constructor.arguments[4].name == '$interceptors'
        beanDefinition.constructor.arguments[4]
                .annotationMetadata
                .getAnnotation(AnnotationUtil.ANN_INTERCEPTOR_BINDING_QUALIFIER)
                .getAnnotations(AnnotationMetadata.VALUE_MEMBER, InterceptorBinding)[0]
                .stringValue().get() == Mutating.name

        when:
        def context = ApplicationContext.run('foo.bar':'test')
        def instance = ((InstantiatableBeanDefinition)beanDefinition).instantiate(context)


        then:
        instance.someMethod("foo") == 'test changed'
        instance.someMethodPackagePrivateMethod("foo") == 'test changed'
    }
}
