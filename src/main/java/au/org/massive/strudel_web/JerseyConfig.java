package au.org.massive.strudel_web;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {
	public JerseyConfig() {
		this.register(JobControlEndpoints.class);
		this.register(LoginController.class);
	}
}
