package com.traderapist.parsers

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/28/13
 * Time: 1:29 PM
 * To change this template use File | Settings | File Templates.
 */
class DraftKingsParser extends BaseParser {
    /**
     * DraftKings is nice enough to already give us a CSV, so all we're
     * doing is grabbing each line and adding it to the output variable.
     */
    @Override
    def parse() {
        output = []

        def pieces = source.split("\n")
        pieces.each { if(it) output << it }
    }
}
