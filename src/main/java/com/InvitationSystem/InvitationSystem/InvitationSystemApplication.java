package com.InvitationSystem.InvitationSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class InvitationSystemApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(InvitationSystemApplication.class, args);
	}

	/** Loads repo-root .env into system properties so ${SMTP_*} placeholders resolve. */
	static void loadDotEnv() {
		Path path = Path.of(".env");
		if (!Files.isRegularFile(path)) {
			path = Path.of(System.getProperty("user.dir", "."), ".env");
		}
		if (!Files.isRegularFile(path)) {
			return;
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
				if (System.getenv(key) == null && System.getProperty(key) == null) {
					System.setProperty(key, value);
				}
			}
		} catch (IOException ignored) {
			// Fall back to application.properties defaults.
		}
	}

}
