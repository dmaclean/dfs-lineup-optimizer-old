package com.traderapist.parsers

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/28/13
 * Time: 1:29 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class BaseParser {
    /*
     * Input data that needs parsing
     */
    def source

    /*
     * String that should be consumable by Predictor after call to parse().
     */
    def output

    /**
     * Parses the salaries from the salaries CSV into a map keyed by the player name.
     *
     * @return
     */
    def readSalaries(file) {
        new File(file).eachLine { line ->
            source += "${line.replaceAll("\"", "")}\n"
        }
    }

    abstract def parse()
}
