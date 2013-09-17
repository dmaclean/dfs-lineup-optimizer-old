package com.traderapist.prediction

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 9/15/13
 * Time: 8:49 PM
 * To change this template use File | Settings | File Templates.
 */
class FootballTeamLookup {
	/**
	 * Mapping that takes the city as a key and gives back the team name.  This is needed because
	 * we're getting DST projections as the city name and DraftKings is giving the salaries by the
	 * team name.
	 */
	static def cityToTeamName = [
	        "Arizona": "Cardinals",
			"Atlanta": "Falcons",
			"Baltimore": "Ravens",
			"Buffalo": "Bills",
			"Carolina": "Panthers",
			"Chicago": "Bears",
			"Cincinnati": "Bengals",
			"Cleveland": "Browns",
			"Dallas": "Cowboys",
			"Denver": "Broncos",
			"Detroit": "Lions",
			"Green Bay": "Packers",
			"Houston": "Texans",
			"Indianapolis": "Colts",
			"Jacksonville": "Jaguars",
			"Kansas City": "Chiefs",
			"Miami": "Dolphins",
			"Minnesota": "Vikings",
			"New England": "Patriots",
			"New Orleans": "Saints",
			"New York Giants": "Giants",
			"New York Jets": "Jets",
			"Oakland": "Raiders",
			"Philadelphia": "Eagles",
			"Pittsburgh": "Steelers",
			"San Francisco": "49ers",
			"San Diego": "Chargers",
			"Seattle": "Seahawks",
			"St. Louis": "Rams",
			"Tampa Bay": "Buccaneers",
			"Tennessee": "Titans",
			"Washington": "Redskins",
	]
}
