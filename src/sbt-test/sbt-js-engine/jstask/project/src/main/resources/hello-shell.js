/*global process, require */

/**
 * A generic template that reads the files passed in and then writes them back out.
 * .js files are expected as input and a "compress" option can be passed in to write
 * it out as a .min.js instead. A "problem" option can also be passed which will generate
 * a problem object for each file read.
 */
(function () {

    "use strict";

    var args = process.argv,
        fs = require("fs"),
        mkdirp = require("mkdirp"),
        path = require("path");

    var SOURCE_FILE_MAPPINGS_ARG = 2;
    var TARGET_ARG = 3;
    var OPTIONS_ARG = 4;

    var sourceFileMappings = JSON.parse(args[SOURCE_FILE_MAPPINGS_ARG]);
    var target = args[TARGET_ARG];
    var options = JSON.parse(args[OPTIONS_ARG]);

    var sourcesToProcess = sourceFileMappings.length;
    var results = [];
    var problems = [];

    function processingDone() {
        if (--sourcesToProcess === 0) {
            console.log("\u0010" + JSON.stringify({results: results, problems: problems}));
        }
    }

    function throwIfErr(e) {
        if (e) throw e;
    }

    sourceFileMappings.forEach(function (sourceFileMapping) {

        var input = sourceFileMapping[0];
        var outputFile = sourceFileMapping[1].replace(".js", options.compress ? ".min.js" : ".js");
        var output = path.join(target, outputFile);

        fs.readFile(input, "utf8", function (e, contents) {
            throwIfErr(e);

            if (options.fail) {
                problems.push({
                    message: "Whoops",
                    severity: "error",
                    lineNumber: 10,
                    characterOffset: 5,
                    lineContent: "Fictitious problem",
                    source: input
                });
                results.push({
                    source: input,
                    result: null
                });

                processingDone();

            } else {
                mkdirp(path.dirname(output), function (e) {
                    throwIfErr(e);

                    fs.writeFile(output, contents, "utf8", function (e) {
                        throwIfErr(e);

                        results.push({
                            source: input,
                            result: {
                                filesRead: [input],
                                filesWritten: [output]
                            }
                        });

                        processingDone();
                    });
                });
            }

        });
    });
})();