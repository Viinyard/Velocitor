package pro.vinyard.velocitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import pro.vinyard.velocitor.core.environment.EnvironmentProperties;

@SpringBootApplication
@EnableConfigurationProperties(EnvironmentProperties.class)
public class VelocitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(VelocitorApplication.class, args);
    }

}
