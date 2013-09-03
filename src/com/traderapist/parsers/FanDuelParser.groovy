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
        m.each { output << "${it[1]},${convertTeamName(it[2])},${it[3].replaceAll("[\$|,]","")}" }
    }

	def convertTeamName(name) {
		if(name.toLowerCase() == "arizona cardinals")   return "cardinals"
		else if(name.toLowerCase() == "atlanta falcons")   return "falcons"
		else if(name.toLowerCase() == "buffalo bills")   return "bills"
		else if(name.toLowerCase() == "carolina panthers")   return "panthers"
		else if(name.toLowerCase() == "chicago bears")   return "bears"
		else if(name.toLowerCase() == "cincinnati bengals")   return "bengals"
		else if(name.toLowerCase() == "cleveland browns")   return "browns"
		else if(name.toLowerCase() == "dallas cowboys")   return "cowboys"
		else if(name.toLowerCase() == "detroit lions")   return "lions"
		else if(name.toLowerCase() == "green bay packers")   return "packers"
		else if(name.toLowerCase() == "houston texans")   return "texans"
		else if(name.toLowerCase() == "indianapolis colts")   return "colts"
		else if(name.toLowerCase() == "jacksonville jaguars")   return "jaguars"
		else if(name.toLowerCase() == "kansas city chiefs")   return "chiefs"
		else if(name.toLowerCase() == "miami dolphins")   return "dolphins"
		else if(name.toLowerCase() == "chicago bears")   return "bears"
		else if(name.toLowerCase() == "minnesota vikings")   return "vikings"
		else if(name.toLowerCase() == "new england patriots")   return "patriots"
		else if(name.toLowerCase() == "new orleans saints")   return "saints"
		else if(name.toLowerCase() == "new york giants")   return "giants"
		else if(name.toLowerCase() == "new york jets")   return "jets"
		else if(name.toLowerCase() == "oakland raiders")   return "raiders"
		else if(name.toLowerCase() == "philadelphia eagles")   return "eagles"
		else if(name.toLowerCase() == "pittsburgh steelers")   return "steelers"
		else if(name.toLowerCase() == "san diego chargers")   return "chargers"
		else if(name.toLowerCase() == "san francisco 49ers")   return "49ers"
		else if(name.toLowerCase() == "seattle seahawks")   return "seahawks"
		else if(name.toLowerCase() == "st louis rams")   return "rams"
		else if(name.toLowerCase() == "tampa bay buccaneers")   return "buccaneers"
		else if(name.toLowerCase() == "tennessee titans")   return "titans"
		else if(name.toLowerCase() == "washington redskins")   return "redskins"

		return name
	}

    public static void main(String[] args) {

    }
}
