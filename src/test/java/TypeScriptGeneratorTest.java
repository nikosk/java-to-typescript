import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by nk on 11/12/15.
 * Project: java-to-typescript
 * Package: PACKAGE_NAME
 */
public class TypeScriptGeneratorTest {

	@Test
	public void testRun() throws Exception {
		System.setProperty(Constants.Config.TOP_LEVEL_PACKAGES + ".0", "testclasses");
		TypeScriptGenerator generator = new TypeScriptGenerator(ConfigFactory.load());
		generator.run();
	}


}
