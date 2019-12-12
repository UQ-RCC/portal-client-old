package au.org.massive.strudel_web;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionManagerConfiguration {
	@Bean
	public ServletListenerRegistrationBean<SessionManager> sessionListener() {
		return new ServletListenerRegistrationBean<>(new SessionManager());
	}
}
