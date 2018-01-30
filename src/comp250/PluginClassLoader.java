package comp250;

import java.net.URL;
import java.net.URLClassLoader;

//Based on https://blog.jayway.com/2014/06/13/sandboxing-plugins-in-java/
public class PluginClassLoader extends URLClassLoader {
	public PluginClassLoader(URL jarFileUrl) {
		super(new URL[] {jarFileUrl});
	}
}
