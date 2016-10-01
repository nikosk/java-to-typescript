import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import gr.dsigned.typescript.generator.TypeScriptGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

@Mojo(name = "gr/dsigned/typescript/generator", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class GeneratorMojo extends AbstractMojo {

	@Parameter(property = "generator.configurationResource", defaultValue = "application")
	private String configurationResource;

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	public void execute() throws MojoExecutionException, MojoFailureException {
		List classPathElements = null;
		getLog().info("Project Path: " + project.getBasedir().getAbsolutePath());
		try {
			classPathElements = project.getCompileClasspathElements();
			URL[] runtimeUrls = new URL[classPathElements.size()];
			for (int i = 0; i < classPathElements.size(); i++) {
				String element = (String) classPathElements.get(i);
				runtimeUrls[i] = new File(element).toURI().toURL();
			}
			URLClassLoader newLoader = new URLClassLoader(runtimeUrls, Thread.currentThread().getContextClassLoader());
			final Config config = ConfigFactory.load(newLoader, configurationResource);
			getLog().info("Stating generator with config:" + configurationResource);
			getLog().info("Config: \n" + config.root().render());
			TypeScriptGenerator generator = new TypeScriptGenerator(config);
			generator.run();
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to execute generator", e);
		}
	}
}
