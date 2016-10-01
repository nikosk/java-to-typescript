## Purpose

**java-to-typescript** creates namespaced Typescript classes from Java classes. It correctly handles inner classes (creates inner typescript namespaces) and inheritance. 

Using this library you can export your Java DTO objects for use in typescript/javascript.

## Example

``java -Dtoplevel-packages.0=com.somepackage -Dnamespace-prefix=pref -Doutput-path="../generated" -D -classpath "./typescriptGenerator.jar:../path/to/classes/or/jar" TypeScriptGenerator``

See: reference.conf for info

## License 

This project is licensed under the MIT license.

## Credits

Inspired by [java2typescript](https://github.com/raphaeljolivet/java2typescript).
