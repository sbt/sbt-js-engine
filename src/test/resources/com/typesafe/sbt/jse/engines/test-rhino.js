// Check that we have Rhino shell methods in scope
readFile("src/test/resources/com/typesafe/sbt/jse/engines/hello.js");

// Check CommonJS support
var sayHello = require("hello").sayHello;

print(sayHello(arguments[0]));