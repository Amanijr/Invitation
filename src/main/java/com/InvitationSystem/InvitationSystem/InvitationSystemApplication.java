package com.InvitationSystem.InvitationSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

@SpringBootApplication
public class InvitationSystemApplication {

	public static void main(String[] args) {
		Properties dotenv = loadDotEnv();
		SpringApplication app = new SpringApplication(InvitationSystemApplication.class);
		if (!dotenv.isEmpty()) {
			app.addInitializers(ctx -> {
				ConfigurableEnvironment env = ctx.getEnvironment();
				env.getPropertySources().addFirst(new PropertiesPropertySource("dotenvFile", dotenv));
			});
		}
		app.run(args);
	}

	/** Loads repo-root .env. Highest priority so a stale EVOLUTION_INSTANCE export cannot win. */
	static Properties loadDotEnv() {
		Properties dotenv = new Properties();
		Path path = Path.of(".env");
		if (!Files.isRegularFile(path)) {
			path = Path.of(System.getProperty("user.dir", "."), ".env");
		}
		if (!Files.isRegularFile(path)) {
			return dotenv;
		}
		try {
			List<String> lines = Files.readAllLines(path);
			for (String raw : lines) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				int eq = line.indexOf('=');
				if (eq <= 0) {
					continue;
				}
				String key = line.substring(0, eq).trim();
				String value = line.substring(eq + 1).trim();
				if ((value.startsWith("\"") && value.endsWith("\""))
						|| (value.startsWith("'") && value.endsWith("'"))) {
					value = value.substring(1, value.length() - 1);
				}
				dotenv.setProperty(key, value);
				System.setProperty(key, value);
			}
		} catch (IOException ignored) {
			// Fall back to application.properties defaults.
		}
		return dotenv;
	}

}
