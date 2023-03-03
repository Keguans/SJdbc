package com.SJdbc.config;

import com.SJdbc.annotation.JdbcMapperScan;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class JdbcRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
        // 获取元注解信息
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(JdbcMapperScan.class.getName());
        if (Objects.isNull(annotationAttributes)) {
            return;
        }
        // 获取包名
        String[] basePackages = (String[]) annotationAttributes.get("basePackages");

        // 获取包下的 class
        for (String basePackage : basePackages) {
            try {
                Set<Class<?>> classes = this.getClasses(basePackage);
                // 注册 beanDefinition
                classes.forEach(aClass -> {
                    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(JdbcFactoryBean.class);
                    beanDefinitionBuilder.addConstructorArgValue(aClass);
                    registry.registerBeanDefinition(aClass.getName(), beanDefinitionBuilder.getBeanDefinition());
                });

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 获取包下的 class
     *
     * @param basePackages
     * @return
     * @throws Exception
     */
    private Set<Class<?>> getClasses(String basePackages) throws Exception {
        basePackages = "classpath*:" + basePackages.replace(".", "/") + "/*.class";
        Set<Class<?>> set = new HashSet<>(basePackages.length());
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory cachingMetadataReaderFactory = new CachingMetadataReaderFactory();
        Resource[] resources = pathMatchingResourcePatternResolver.getResources(basePackages);
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        for (Resource resource : resources) {
            MetadataReader reader = cachingMetadataReaderFactory.getMetadataReader(resource);
            String className = reader.getClassMetadata().getClassName();
            set.add(loader.loadClass(className));
        }
        return set;
    }
}
