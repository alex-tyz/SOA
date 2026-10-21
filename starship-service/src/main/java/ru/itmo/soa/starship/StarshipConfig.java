package ru.itmo.soa.starship;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.sql.DataSource;
import javax.xml.stream.XMLInputFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import ru.itmo.soa.starship.generated.client.api.SpaceMarinesApi;
import ru.itmo.soa.starship.generated.client.ApiClient;
import ru.itmo.soa.starship.generated.client.model.SpaceMarineDto;

@Configuration
@EnableWebMvc
@EnableTransactionManagement
@ComponentScan(basePackageClasses = StarshipConfig.class)
public class StarshipConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins("*")
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept");
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    @Bean
    DataSource dataSource() {
        String password = System.getenv("STARSHIP_DB_PASSWORD");
        return new DriverManagerDataSource(
                required("STARSHIP_DB_URL"),
                required("STARSHIP_DB_USER"),
                password == null || password.isBlank() ? null : password);
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    URI spaceMarineBaseUri() {
        URI uri = URI.create(required("SPACE_MARINE_BASE_URL"));
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("SPACE_MARINE_BASE_URL must use HTTPS");
        }
        return uri;
    }

    @Bean
    XmlMapper xmlMapper() {
        XMLInputFactory input = XMLInputFactory.newFactory();
        // Запрещаем DTD в XML-ответе первого сервиса.
        input.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        // Запрещаем загрузку внешних сущностей из XML.
        input.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XmlMapper mapper = XmlMapper.builder(XmlFactory.builder().xmlInputFactory(input).build())
                .addModule(new JavaTimeModule()).build();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.addMixIn(SpaceMarineDto.class, SpaceMarineXml.class);
        return mapper;
    }

    @Bean
    SpaceMarinesApi spaceMarinesApi(URI spaceMarineBaseUri, XmlMapper xmlMapper) {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(3000);
        requests.setReadTimeout(5000);
        RestTemplate client = new RestTemplate(requests);
        client.getMessageConverters().add(0, new MappingJackson2XmlHttpMessageConverter(xmlMapper));
        ApiClient apiClient = new ApiClient(client)
                .setBasePath(spaceMarineBaseUri.toString().replaceAll("/+$", ""));
        return new SpaceMarinesApi(apiClient);
    }

    @JacksonXmlRootElement(localName = "spaceMarine")
    private abstract static class SpaceMarineXml {}

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }
}
