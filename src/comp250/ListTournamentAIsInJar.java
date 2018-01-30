package comp250;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.Policy;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import tournaments.LoadTournamentAIs;

public class ListTournamentAIsInJar {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		Policy.setPolicy(new SandboxSecurityPolicy());
		System.setSecurityManager(new SecurityManager());

		String jarPath = args[0];
		//String jarPath = "../bot/bot.jar";

		ClassLoader loader = new PluginClassLoader(new File(jarPath).toURI().toURL());

		URL jar = new File(jarPath).toURI().toURL();
		ZipInputStream zip = new ZipInputStream(jar.openStream());
		while (true) {
			ZipEntry e = zip.getNextEntry();
			if (e == null)
				break;
			String name = e.getName();
			if (name.endsWith(".class")) {
				String className = name.substring(0, name.length() - 6).replace('/', '.');
				Class<?> c = loader.loadClass(className);
				if (!Modifier.isAbstract(c.getModifiers()) && LoadTournamentAIs.isTournamentAIClass(c)) {
					System.out.println(className);
				}
			}
		}
	}
}
