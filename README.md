# FixTess4j4Mac

[Tess4j](https://github.com/nguyenq/tess4j) is a Java wrapper library for the tesseract program, an excellent OCR tool.

FixTess4j4Mac is a simple library that adds the required C library from tesseract into the tess4j jar file so that you can use tess4j on a Mac without the missing resource error.

## Executable Now Available
There is now a GRAALVM compiled native-image available from the [releases page](https://github.com/EasyG0ing1/FixTess4j4Mac/releases/latest) that you can download and run to patch your Tess4j jar file. Just get the program and run it and the menu will be self explanatory.

### Adding this library to your project
The library is available as a Maven dependency on Central. Add the following to your POM file:

```xml
<dependency>
    <groupId>com.simtechdata</groupId>
    <artifactId>FixTess4j4Mac</artifactId>
    <version>2.0.0</version>
</dependency>
```

Or, if using Gradle to build, add this to your Gradle build file

```groovy
compile group: 'com.simtechdata', name: 'FixTess4j4Mac', version: 2.0.0
```

You can even use it from a Groovy script

```groovy
@Grapes(
  @Grab(group='com.simtechdata', module='FixTess4j4Mac', version=2.0.0)
)
```
If your project is modular, then make sure this is in your `module-info.java` file
```Java
requires com.simtechdata.FixTess4j4Mac;
```

### Prerequisites
You can have the library check to see if everything is in place and working so that the patch will go snoothly,

```Java
String response = FixTess4j4Mac.checkEnvironment();
System.out.println(response);
```

If there are any issues that you need to address, the output will tell you exactly what you need to do to fix it.

Basically, the library needs to be able to execute: `mvn` and `jar` which means they need to be in your PATH. You also need to have tesseract installed via Homebrew because thats where the library gets the file to patch the jar. If you don't have it installed, or you aren't sure, this will fix you up in a jiffy:

```bash
brew update
brew uninstall tesseract
sudo rm -R /usr/local/Cellar/tesseract
brew install tesseract
```

### Setup

If you dont have Tess4J in a local maven repository because you either don't use Maven or you manually downloaded the library or whatever the reason, you can patch your jar file specifically by creating a Path object with that files full path, then simply pass that Path object to the `patchJar()` method:

```Java
Path path = Paths.get("MyFolder","subfolder","tess4j-5.6.0.jar");
FixTess4j4Mac.patchJar(path);
```
If you're using Maven, read on...

### Patching tess4j

tess4j needs access to a specific C library file `libtesseract.dylib`, which is automatically available to tess4j when it is used in a Windows environment. But for us Mac users, simply installing tesseract doesn't make it available to tess4j, so we need to patch the tess4j jar file and put the C library inside of it so that we don't have problems using tess4j.

This is where FixTess4j4Mac comes in handy.

```Java
FixTess4j4Mac.patchJar();
```

If you want to patch specific versions of the Tess4J library, you can get a list of Path objects for each version you have in your local maven repo, then simply pass one of the paths into the patch method as an argument.

```Java
List<Path> list = FixTess4j4Mac.getMavenVersions();
for(Path path : list) {
    FixTess4j4Mac.patchJar(path);
}
```

### Using Tess4J After Patching

Once the library is patched, when you use the tess4j library, you will need to specify the data directory so that the library has access to the training data in order to perform its OCR methods successfully. So wherever you instantiate tess4j in your project you will need to add that path, which you can pull from this library like so:
```Java
Tesseract tesseract = new Tesseract();
tesseract.setDatapath(FixTess4j4Mac.getDataPath());
```

Hope this library helps make your use of tess4j easy.

# Updates

### 2.0.0
* - Rewrote most of the code to take advantage of the JavaProc library for running processes. Easier to use and more reliable.
* - Added the getMavenVersions() method
* - Added overloaded patchJar() method for patching specific maven paths.
* - Added the patchJar(Path) method to make is easier to patch a specific maven version of Tess4J - see documentation above for details.
* - Added the checkEnvironment() method to make it easier to use the library and get things done quickly.
* - Updated test class.
* - Removed the `setPathToTess4jJarFile` method - use the `patchJar(Path)` method instead. 
