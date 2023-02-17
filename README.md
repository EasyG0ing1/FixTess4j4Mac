# FixTess4j4Mac

[Tess4j](https://github.com/nguyenq/tess4j) is a Java wrapper library for the tesseract program, an excellent OCR tool.

FixTess4j4Mac is a simple library that adds the required C library from tesseract into the tess4j jar file so that you can use tess4j on a Mac without the missing resource error.

### Adding this library to your project
The library is available as a Maven dependency on Central. Add the following to your POM file:

```xml
<dependency>
    <groupId>com.simtechdata</groupId>
    <artifactId>FixTess4j4Mac</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or, if using Gradle to build, add this to your Gradle build file

```groovy
compile group: 'com.simtechdata', name: 'FixTess4j4Mac', version: 1.0.0
```

You can even use it from a Groovy script

```groovy
@Grapes(
  @Grab(group='com.simtechdata', module='FixTess4j4Mac', version=1.0.0)
)
```
If your project is modular, then make sure this is in your `module-info.java` file
```Java
requires com.simtechdata.FixTess4j4Mac;
```

### Prerequisites
You must have tesseract installed on your Mac before you can even use the tess4j library since the library is a wrapper for the tesseract program. And for this library to work properly, you can only have one version of tesseract installed, so I recommend meeting this requirement by running these commands from terminal:

```bash
brew update
brew uninstall tesseract
sudo rm -R /usr/local/Cellar/tesseract
brew install tesseract
```
Also, both of these are required:
- Either `bash` or `zsh` must be in your PATH environment variable.
- Maven users must have the `mvn` command in their PATH environment variable.

### Setup

If you aren't using Maven as your build tool, or you don't have the tess4j jar file in your local Maven repository or you are using a version of tess4j that is NOT the latest version, then you will need to add the full path to the tess4j jar file before doing anything else:

```Java
FixTess4j4Mac.setPathToTess4jJarFile(path);
```
The path must include the full name of the jar file that you're using, for example: `"/my/path/tess4j-5.6.0.jar"`

If you're using Maven and you're using the latest version of tess4j, then this setting is not necessary. What is necessary is that the latest tess4j jar file exists in your local Maven repository, which is usually done for you when you add the dependency into your projects POM file. But if you need to pull down the latest version of tess4j and have it installed into your local Maven repository, then make this call one time:
```Java
FixTess4j4Mac.pullLatestMavenTess4JLibrary();
```

### Patching tess4j

tess4j needs access to a specific C library file `libtesseract.dylib`, which is automatically available to tess4j when it is used in a Windows environment. But for us Mac users, simply installing tesseract doesn't make it available to tess4j, so we need to patch the tess4j jar file and put the C library inside of it so that we don't have problems using tess4j.

This is where FixTess4j4Mac comes in handy.

I recommend leveraging a command line argument so that these calls are not invoked in a runtime scenario after you have compiled your project into an executable `jar` or `app` file. With IntelliJ, this is accomplished by editing your run configurations and having one that has some word in the `program arguments` field. So lets say that the word is `dev` in which case you would add this to your Main class

```Java
if(args.contains("dev") {
    if(!FixTess4j4Mac.jarHasCLibrary())
        FixTess4j4Mac.addCLibraryToJarFile();
}
```
Once the library is patched, when you use the tess4j library, you will need to specify the data directory so that the library has access to the training data in order to perform its OCR methods successfully. So wherever you instantiate tess4j in your project you will need to add that path, which you can pull from this library like so:
```Java
Tesseract tesseract = new Tesseract();
tesseract.setDatapath(FixTess4j4Mac.getDataPath());
```

## Issues
If you find yourself having problems and you're getting RuntimeErrors, then just manually set the full path to the tess4j jar file that you're referencing from your project and then patch it by following those instructions above. Manually specifying the full path to the jar file will work every time as long as the prerequisites have been done and verified.

Hope this library helps make your use of tess4j easy.
