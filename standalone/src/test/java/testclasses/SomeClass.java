package testclasses;

import typescript.annotations.Ignore;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by nk on 11/12/15.
 * Project: java-to-typescript
 * Package: testclasses
 */
public class SomeClass extends WDoNotInclude<String> {

	private static final Logger logger = Logger.getAnonymousLogger();

	private String somefield;

	private Date aDate;

	@Ignore
	private boolean aBoolean;

}
