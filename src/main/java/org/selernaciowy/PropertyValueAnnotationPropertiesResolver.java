package org.selernaciowy;

import org.selernaciowy.annotations.PropertyValue;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.type.StandardAnnotationMetadata;

import java.lang.reflect.Field;
import java.util.Arrays;

public class PropertyValueAnnotationPropertiesResolver implements BeanFactoryPostProcessor,
        EnvironmentAware,
        BeanNameAware {
    private Environment environment;
    private String beanName;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Arrays.stream(beanFactory.getBeanDefinitionNames())
                .filter(name -> !name.equals(beanName))
                .map(beanFactory::getBeanDefinition)
                .forEach(beanDefinition -> {
                    Class<?> beanClass = null;
                    if (beanDefinition instanceof ScannedGenericBeanDefinition definition) {
                        try {
                            beanClass = Class.forName(definition.getMetadata().getClassName());
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (beanDefinition instanceof AnnotatedGenericBeanDefinition definition) {
                        StandardAnnotationMetadata metadata = (StandardAnnotationMetadata) definition.getMetadata();
                        beanClass = metadata.getIntrospectedClass();
                    }
                    if (beanDefinition instanceof RootBeanDefinition definition) {
                        if (definition.getBeanClassName() != null) {
                            beanClass = definition.getBeanClass();
                        }
                    }
                    if (beanClass != null) {
                        Field[] fields = beanClass.getDeclaredFields();
                        for (Field field : fields) {
                            Arrays.stream(field.getAnnotations())
                                    .filter(annotation -> annotation.annotationType().equals(PropertyValue.class))
                                    .findFirst()
                                    .ifPresent(annotation -> {
                                        PropertyValue valueAnnotation = (PropertyValue) annotation;
                                        String propertyValue = environment.resolvePlaceholders(valueAnnotation.value());
                                        Object converted = DefaultConversionService.getSharedInstance()
                                                .convert(propertyValue, field.getType());
                                        if (converted != null) {
                                            beanDefinition.getPropertyValues()
                                                    .addPropertyValue(field.getName(), converted);
                                        }
                                    });
                        }
                    }
                });
    }
}
