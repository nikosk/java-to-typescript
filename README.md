## Purpose

**java-to-typescript** creates namespaced Typescript classes from Java classes. It correctly handles inner classes (creates inner typescript namespaces). 

Using this library you can export your Java DTO objects for use in typescript/javascript.

## Example

``java -jar typescriptGenerator.jar -Dtoplevel-packages.0=com.somepackage -Dtoplevel-packages.1=com.otherpackage -Doutput-path=build``

See: reference.conf for info

## License 

This project is licensed under the MIT license.

## Credits

Inspired by [java2typescript](https://github.com/raphaeljolivet/java2typescript).
