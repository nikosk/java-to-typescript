## Purpose

**java-to-typescript** creates namespaced Typescript classes from Java classes. It correctly handles inner classes (creates inner typescript namespaces) and inheritance.

Using this library you can export your Java DTO objects for use in typescript/javascript.

## Usage

Register the repository in your pom.xml

``` 
<repositories>
    <repository>
        <id>java-to-typescript-repo</id>
        <url>https://raw.github.com/nikosk/java-to-typescript/releases/</url>        
    </repository>
</repositories>
```

Add the plugin in your build:

```
<plugin>
		<groupId>gr.dsigned</groupId>
		<artifactId>java-to-typescript-plugin</artifactId>
 		<version>1.0.0</version>
    <dependencies>
			[Add your dependencies here. The generator will scan only the dependencies registered here.]
			<dependency>
      		<groupId>my.group</groupId>
      		<artifactId>my.classes.to.be.converted</artifactId>
      		<version>${dto.version}</version>
      </dependency>
    </dependencies>
</plugin>

```


then run ```mvn gr.dsigned:java-to-typescript-plugin:1.0.0:generator``` to generate your Typescript classes.

See: reference.conf for info

## License

This project is licensed under the MIT license.

## Credits

Inspired by [java2typescript](https://github.com/raphaeljolivet/java2typescript).
