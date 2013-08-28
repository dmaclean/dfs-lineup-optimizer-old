package com.traderapist.parsers

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/28/13
 * Time: 12:35 PM
 * To change this template use File | Settings | File Templates.
 */
class FanDuelParser extends BaseParser {
    /**
     * Create output in the format of <position>,<name>,<salary>
     */
    def parse() {
        // Sanity check.  Make sure we have some text to search on.
        if(source == null) {
            println "Source data cannot be null."
            return
        }

        if(!output || !output.empty)
            output = []

        // Strip out all tabs and newlines first.
        def temp = (source =~ /[\n|\t]+/).replaceAll("")

        // Grab the position, player name, and salary out of each table row, turn it into a CSV line, and add it to the output
        def m = temp =~ /<tr.*?><td.*?><span>(\w+)<\/span><\/td><td><div.*?>(.*?)<\/div><\/td><td>.*?<\/td><td>.*?<\/td><td>.*?<\/td><td>(.*?)<\/td>.*?<\/tr>/
        m.each { output << "${it[1]},${it[2]},${it[3].replaceAll("[\$|,]","")}" }
    }

    public static void main(String[] args) {

    }
}
