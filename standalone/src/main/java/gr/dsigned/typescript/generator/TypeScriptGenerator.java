package gr.dsigned.typescript.generator;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.reflect.ClassPath;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Created by nk on 29/11/15.
 * Project: travelms
 * Package: gr.andko.travelms.xml
 */
public class TypeScriptGenerator {


	private final Config config;

	private final String prefix;

	private final List<Pattern> EXCLUDE_FIELD_PATTERNS;

	private final List<Pattern> EXCLUDE_CLASS_PATTERNS;

	private final List<Pattern> INCLUDE_CLASS_PATTERNS;

	private final List<Pattern> EXCLUDE_FIELD_ANNOTATION_PATTERNS;

	private LinkedHashMap<String, Module> modules = new LinkedHashMap<>();

	private LinkedHashMap<Class<?>, Module> classToModule = new LinkedHashMap<>();

	public TypeScriptGenerator(Config conf) {
		this.config = conf;
		String prefix = config.getString("namespace-prefix");
		this.prefix = prefix.isEmpty() ? "" : prefix + ".";

		EXCLUDE_CLASS_PATTERNS = compilePatterns(config.getStringList(Constants.Config.EXLUDE_CLASS_NAMES));
		INCLUDE_CLASS_PATTERNS = compilePatterns(config.getStringList(Constants.Config.INCLUDE_CLASSES_NAMES));
		EXCLUDE_FIELD_PATTERNS = compilePatterns(config.getStringList(Constants.Config.EXCLUDE_FIELD_NAMES));
		EXCLUDE_FIELD_ANNOTATION_PATTERNS = compilePatterns(config.getStringList(Constants.Config.EXCLUDE_FIELD_ANNOTATION_NAMES));

		System.out.println(format(
			"\n Configuration: " +
				"\n\t Top level packages: %s" +
				"\n\t Exclude classes: %s" +
				"\n\t Include classes: %s" +
				"\n\t Exclude fields : %s" +
				"\n\t Exclude field annotations : %s",
			Joiner.on(", ").join(conf.getStringList(Constants.Config.TOP_LEVEL_PACKAGES)),
			Joiner.on(", ").join(conf.getStringList(Constants.Config.EXLUDE_CLASS_NAMES)),
			Joiner.on(", ").join(conf.getStringList(Constants.Config.INCLUDE_CLASSES_NAMES)),
			Joiner.on(", ").join(conf.getStringList(Constants.Config.EXCLUDE_FIELD_NAMES)),
			Joiner.on(", ").join(conf.getStringList(Constants.Config.EXCLUDE_FIELD_ANNOTATION_NAMES))
		));
	}


	public static void main(String[] args) {
		Config conf = ConfigFactory.load();
		TypeScriptGenerator generator = new TypeScriptGenerator(conf);
		try {
			generator.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() throws IOException {
		ClassPath classPath = ClassPath.from(TypeScriptGenerator.class.getClassLoader());
		if (config.hasPathOrNull(Constants.Config.DEBUG)) {
			System.out.println(format("Classpath: \n %s", Joiner.on("\n").join(classPath.getAllClasses())));
		}
		if (config.hasPath(Constants.Config.INCLUDE_CLASSES_NAMES) && config.getStringList(Constants.Config.INCLUDE_CLASSES_NAMES).size() > 0) {
			System.out.println("Found included class patterns. Proceeding with class filtering.");
			processIncluded(classPath);
		} else {
			System.out.println("Proceeding with package filtering.");
			processFromPackages(classPath);
		}
		writeModules();
	}

	private void processFromPackages(ClassPath classPath) {
		List<String> packageNames = config.getStringList(Constants.Config.TOP_LEVEL_PACKAGES);
		for (String packageName : packageNames) {
			System.out.println(format("Searching in %s", packageName));
			ImmutableSet<ClassPath.ClassInfo> classesRecursive = classPath.getTopLevelClassesRecursive(packageName);
			System.out.println(format("Found %s classes", classesRecursive.size()));
			for (ClassPath.ClassInfo classInfo : classesRecursive) {
				String name = classInfo.getName();
				if (!name.contains("-") && !matchesExcludePattern(name)) {
					Class<?> clazz = classInfo.load();
					System.out.println(format("Processing class %s", name));
					process(clazz);
				} else {
					System.out.println(format("Rejected %s", name));
				}
			}
		}
	}

	private boolean matchesExcludePattern(String toTest) {
		Pattern result = matchesPatterns(EXCLUDE_CLASS_PATTERNS, toTest);
		if (result != null) {
			System.out.println(format("Excluding %s because it matches '%s'", toTest, result));
			return true;
		}
		return false;
	}

	private boolean matchesIncludePattern(String toTest) {
		Pattern result = matchesPatterns(INCLUDE_CLASS_PATTERNS, toTest);
		if (result != null) {
			System.out.println(format("Including %s because it matches '%s'", toTest, result));
			return true;
		}
		return false;
	}

	private boolean matchesExcludeFieldPattern(String toTest) {
		Pattern result = matchesPatterns(EXCLUDE_FIELD_PATTERNS, toTest);
		if (result != null) {
			System.out.println(format("\t\t%s matches '%s'", toTest, result));
			return true;
		}
		return false;
	}


	private Pattern matchesPatterns(List<Pattern> patterns, String toTest) {
		for (Pattern pattern : patterns) {
			if (pattern.matcher(toTest).matches()) {
				return pattern;
			}
		}
		return null;
	}

	private boolean hasMatchingAnnotations(List<Pattern> patterns, Field field) {
		for (Annotation annotation : field.getDeclaredAnnotations()) {
			final Pattern matches = matchesPatterns(patterns, annotation.annotationType().getCanonicalName());
			if (matches != null) {
				return true;
			}
		}
		return false;
	}

	private void processIncluded(ClassPath classPath) {
		final UnmodifiableIterator<ClassPath.ClassInfo> it = classPath.getAllClasses().iterator();
		while (it.hasNext()) {
			final ClassPath.ClassInfo classInfo = it.next();
			String name = classInfo.getName();
			if (!matchesExcludePattern(name)) {
				if (matchesIncludePattern(name)) {
					process(classInfo.load());
				}
			}
		}
	}

	private List<Pattern> compilePatterns(List<String> globs) {
		List<Pattern> patterns = new ArrayList<>();
		for (String glob : globs) {
			patterns.add(Pattern.compile(GlobUtils.convertGlobToRegex(glob)));
		}
		return patterns;
	}

	private void writeModules() throws IOException {
		System.out.println(format("Found %s modules.", modules.size()));
		final ArrayList<String> refs = new ArrayList<>();
		String outputPath = config.getString(Constants.Config.OUTPUT_PATH);
		for (Module module : modules.values()) {
			new File(outputPath).mkdirs();
			if (module.getParent() == null) {
				final String fileName = format(outputPath + "/%s.ts", module.getName());
				FileWriter writer = new FileWriter(fileName);
				writeModule(writer, module);
				writer.flush();
				writer.close();
				refs.add(format("%s.ts", module.getName()));
			}
		}
		FileWriter writer = new FileWriter(outputPath + "/index.d.ts");
		for (String ref : refs) {
			writer.write(format("/// <reference path='./%s' />\n", ref));
		}
		writer.flush();
		writer.close();
	}

	private void writeModule(Writer writer, Module module) throws IOException {
		System.out.println(format("Writing module %s.", module.getName()));
		if (module.getClasses().isEmpty()) {
			return;
		}
		if (module.getParent() == null) {
			writer.write(format("/// <reference path='./index.d.ts' />\n\n"));
		}
		writer.write(format("namespace %s { \n\n", module.getName()));
		final ArrayList<Class<?>> classes = new ArrayList<>(new LinkedHashSet<>(module.getClasses()));
		for (Class<?> aClass : ClassSort.sort(classes)) {
			System.out.println(format("\tWriting class %s.", aClass.getName()));
			writer.write(format("\t/** %s */\n", aClass.getCanonicalName()));
			Class<?> superclass = aClass.getSuperclass();
			final Module superModule = classToModule.get(superclass);
			if (superModule != null) {
				final String superClassName = format("%s%s", module.equals(superModule) ? "" : superModule.getName() + ".", superclass.getSimpleName());
				writer.write(format("\texport class %s extends %s {\n", aClass.getSimpleName(), superClassName));
			} else {
				writer.write(format("\texport class %s {\n", aClass.getSimpleName()));
			}
			if (aClass.isEnum()) {
				for (Object o : aClass.getEnumConstants()) {
					writer.write(format("\t\tpublic static %s : string =  \"%s\";\n", o, o));
				}
			} else {
				for (Field field : aClass.getDeclaredFields()) {
					final boolean excludeByName = matchesExcludeFieldPattern(field.getName());
					System.out.println(format("\t\tWriting field %s.%s.", aClass.getSimpleName(), field.getName()));
					final boolean excludeByAnnotation = hasMatchingAnnotations(EXCLUDE_FIELD_ANNOTATION_PATTERNS, field);
					if (!excludeByName &&
						!excludeByAnnotation) {
						writer.write(format("\t\t%s : %s;\n", field.getName(), getTSType(module, field.getType(), field)));
					} else {
						System.out.println(format("\t\tField %s excluded. Reason: %s", field.getName(), excludeByName ? "excluded by name" : "excluded by annotation"));
					}
				}
			}
			writer.write("\t}\n");
		}
		writer.write("}\n");
		for (Module submodule : module.getChildren()) {
			writeModule(writer, submodule);
		}
	}

	private String getTSType(Module currentModule, Class<?> type, Field field) {
		if (type.isArray()) {
			return format("%s[]", getTSType(currentModule, type.getComponentType(), null));
		}
		if (Collection.class.isAssignableFrom(type)) {
			Class<?> generic = ((Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
			return format("%s[]", getTSType(currentModule, generic, null));
		}
		if (String.class.isAssignableFrom(type)) {
			return "string";
		}
		if (Number.class.isAssignableFrom(type)
			|| int.class.isAssignableFrom(type)
			|| long.class.isAssignableFrom(type)
			|| float.class.isAssignableFrom(type)
			|| double.class.isAssignableFrom(type)
			|| short.class.isAssignableFrom(type)
			|| byte.class.isAssignableFrom(type)) {
			return "number";
		}
		if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
			return "boolean";
		}
		if (Date.class.isAssignableFrom(type) || XMLGregorianCalendar.class.isAssignableFrom(type)) {
			return "Date";
		}
		Module module = classToModule.get(type);
		if (module != null) {
			return (module.equals(currentModule) ? "" : module.getName() + ".") + type.getSimpleName();
		}
		return "any";
	}

	private void process(Class<?> clazz) {
		String moduleName = getName(clazz);
		Module module = modules.getOrDefault(moduleName, new Module(moduleName));
		module.getClasses().add(clazz);
		modules.put(moduleName, module);
		classToModule.put(clazz, module);
		for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
			String subModuleName = getName(declaredClass);
			Module subModule = modules.getOrDefault(subModuleName, new Module(module, subModuleName));
			modules.put(subModuleName, subModule);
			classToModule.put(declaredClass, subModule);
			process(declaredClass);
		}
	}

	private String getName(Class<?> clazz) {
		Class<?> declaringClass = clazz.getDeclaringClass();
		String packageName = clazz.getPackage().getName();
		int dotIndex = packageName.lastIndexOf(".");
		String part = packageName.substring(dotIndex + 1);
		part = Character.toString(part.charAt(0)).toUpperCase() + part.substring(1);
		if (declaringClass != null) {
			StringBuilder sb = new StringBuilder();
			this.buildParentNames(clazz, sb);
			return format(prefix + "%s.%s", part, sb.toString());
		} else {
			return format(prefix + "%s", part);
		}
	}

	private void buildParentNames(Class<?> clazz, StringBuilder sb) {
		sb.insert(0, clazz.getSimpleName());
		Class<?> declaringClass = clazz.getDeclaringClass();
		if (declaringClass != null) {
			sb.insert(0, ".");
			buildParentNames(declaringClass, sb);
		}
	}

	public LinkedHashMap<String, Module> getModules() {
		return modules;
	}

	public static class Module {

		private String name;

		private List<Module> children = new ArrayList<>();

		private List<Class<?>> classes = new ArrayList<>();

		private Module parent;

		public Module(String name) {
			this.name = name;
		}

		public Module(Module parent, String name) {
			this.parent = parent;
			this.name = name;
			this.parent.getChildren().add(this);
		}

		public String getName() {
			return name;
		}

		public List<Class<?>> getClasses() {
			return classes;
		}

		public Module getParent() {
			return parent;
		}

		public List<Module> getChildren() {
			return children;
		}
	}
}
