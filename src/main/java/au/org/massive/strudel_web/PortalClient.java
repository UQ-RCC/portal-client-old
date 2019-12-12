package au.org.massive.strudel_web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication
@EnableConfigurationProperties
public class PortalClient {
	public static void main(String[] args) {
		SpringApplication.run(PortalClient.class, args);
	}
}
