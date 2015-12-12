import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by nk on 11/12/15.
 * Project: java-to-typescript
 * Package: PACKAGE_NAME
 */
public class TypeScriptGeneratorTest {

	@Test
	public void testRun() throws Exception {
		final Config config = ConfigFactory.load("all-in-package");
		TypeScriptGenerator generator = new TypeScriptGenerator(config);
		generator.run();
		final LinkedHashMap<String, TypeScriptGenerator.Module> modules = generator.getModules();
		assertThat(modules.size(), is(equalTo(1)));
		assertThat(modules.values().iterator().next().getClasses().size(), is(equalTo(2)));
	}


	@Test
	public void testRunExcludeSome() throws Exception {
		final Config config = ConfigFactory.load("exclude-some");
		TypeScriptGenerator generator = new TypeScriptGenerator(config);
		generator.run();
		final LinkedHashMap<String, TypeScriptGenerator.Module> modules = generator.getModules();
		assertThat(modules.size(), is(equalTo(1)));
		assertThat(modules.values().iterator().next().getClasses().size(), is(equalTo(1)));
	}


	@Test
	public void testRunIncludedOnly() throws Exception {
		final Config config = ConfigFactory.load("include-some");
		TypeScriptGenerator generator = new TypeScriptGenerator(config);
		generator.run();
		final LinkedHashMap<String, TypeScriptGenerator.Module> modules = generator.getModules();
		assertThat(modules.size(), is(equalTo(1)));
		assertThat(modules.values().iterator().next().getClasses().size(), is(equalTo(1)));
	}

	@Test
	public void testRunExcludeFields() throws Exception {
		final Config config = ConfigFactory.load("exclude-some-fields");
		TypeScriptGenerator generator = new TypeScriptGenerator(config);
		generator.run();
	}

}
