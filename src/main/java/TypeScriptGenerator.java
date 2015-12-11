import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static java.lang.String.format;

/**
 * Created by nk on 29/11/15.
 * Project: travelms
 * Package: gr.andko.travelms.xml
 */
public class TypeScriptGenerator {


	private final Config config;
	private LinkedHashMap<String, Module> modules = new LinkedHashMap<>();
	private LinkedHashMap<Class<?>, Module> classToModule = new LinkedHashMap<>();

	public TypeScriptGenerator(Config conf) {
		this.config = conf;
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
		ClassPath from = ClassPath.from(TypeScriptGenerator.class.getClassLoader());
		List<String> packageNames = config.getStringList(Constants.Config.TOP_LEVEL_PACKAGES);
		List<String> exludedClassNames = config.getStringList(Constants.Config.EXLUDED_CLASS_NAMES);
		for (String packageName : packageNames) {
			ImmutableSet<ClassPath.ClassInfo> classesRecursive = from.getTopLevelClassesRecursive(packageName);
			for (ClassPath.ClassInfo classInfo : classesRecursive) {
				String name = classInfo.getName();
				if (!name.contains("-") && !exludedClassNames.contains(name)) {
					Class<?> clazz = classInfo.load();
					process(clazz);
				}
			}
		}
		System.out.println(format("Found %s modules.", modules.size()));
		for (Module module : modules.values()) {
			String outputPath = config.getString(Constants.Config.OUTPUT_PATH);
			if (module.getParent() == null) {
				FileWriter writer = new FileWriter(format(outputPath + "/%s.ts", module.getName()));
				writeModule(writer, module);
				writer.flush();
				writer.close();
			}
		}
	}

	private void writeModule(Writer writer, Module module) throws IOException {
		writer.write(format("namespace %s { \n\n", module.getName()));
		for (Class<?> aClass : module.getClasses()) {
			writer.write(format("\t/** %s */\n", aClass.getCanonicalName()));
			writer.write(format("\texport class %s {\n", aClass.getSimpleName()));
			if (aClass.isEnum()) {
				for (Object o : aClass.getEnumConstants()) {
					writer.write(format("\t\t%s : string =  \"%s\";\n", o, o));
				}
			} else {
				for (Field field : aClass.getDeclaredFields()) {
					writer.write(format("\t\t%s : %s;\n", field.getName(), getTSType(module, field.getType(), field)));
				}
			}
			writer.write("\t}\n");
		}
		writer.write(format("}\n"));
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

	public void process(Class<?> clazz) {
		String prefix = config.getString("namespace-prefix");
		String moduleName = getName(prefix, clazz);
		Module module = modules.getOrDefault(moduleName, new Module(moduleName));
		module.getClasses().add(clazz);
		modules.put(moduleName, module);
		classToModule.put(clazz, module);
		for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
			String subModuleName = getName(prefix, declaredClass);
			Module subModule = modules.getOrDefault(subModuleName, new Module(module, subModuleName));
			modules.put(subModuleName, subModule);
			classToModule.put(declaredClass, subModule);
			process(declaredClass);
		}
	}

	public String getName(String prefix, Class<?> clazz) {
		prefix = prefix.isEmpty() ? "" : prefix + ".";
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


	public static class Module {

		private List<Class<?>> classes = new ArrayList<>();
		private String name;
		private List<Module> children = new ArrayList<>();

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
