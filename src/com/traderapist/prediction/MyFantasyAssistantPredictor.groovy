package com.traderapist.prediction

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 9/11/13
 * Time: 9:46 PM
 * To change this template use File | Settings | File Templates.
 */
class MyFantasyAssistantPredictor extends Predictor {
	/**
	 * Expect input in the form of <name>,<position>,<fantasy points>
	 *
	 * FOOTBALL
	 * <tr><td>\d+\.\s*<\/td><td>(.*?)<\/td><td>([\w\d]+)<\/td><td>\w+<\/td><td>@*\w+<\/td><td>([\d\.]+)<\/td><\/tr>
	 * \1,\2,\3\n
	 *
	 * @param file
	 * @return
	 */
	def readInputFootball(file) {
		projections["FLEX"] = [:]

		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def projection = Double.parseDouble(pieces[2])
//			def name = (pieces[1] == "DEF") ? FootballTeamLookup.cityToTeamName[pieces[0]] : pieces[0].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()
			def name = pieces[0].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()

			/*
			 * Check if the map of projections for this position exists, and if not, create it.
			 */
			if(!projections.containsKey(pieces[1]))
				projections[pieces[1]] = [:]
			projections[pieces[1]][name] = projection

			/*
			 * For football, add RB, WR, and TEs to the flex position.
			 */
			if(pieces[1].matches("RB|WR|TE"))
				projections["FLEX"][name] = projection
		}
	}

	/**
	 * Expect input in the form of <name>,<position>,<fantasy points>
	 *
	 * BASEBALL
	 * <tr><td>(.*?)(<a.*?>.*?<\/a>)?<\/td>(<td>.*?<\/td>){7}<td>([\d\.]+)<\/td><\/tr>
	 * \1,<position>,\4\n
	 *
	 * @param file
	 * @return
	 */
	def readInputBaseball(file) {
		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[0].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()

			/*
			 * For baseball, we need to separate players that have two different positions
			 * listed, such as 2B/RF.
			 */
			def positions = pieces[1]
			if(pieces[1].indexOf("/") > -1) {
				positions = pieces[1].split("/")
			}
			else {
				positions = [positions]
			}

			for(position in positions) {
				position = normalizeBaseballPosition(position)

				/*
				 * Check if the map of projections for this position exists, and if not, create it.
				 */
				if(!projections.containsKey(position))
					projections[position] = [:]
				projections[position][name] = Double.parseDouble(pieces[2])
			}
		}
	}

	def run() {
		def projectionFile = "data/${projectionSource}/${sport}.csv"
		def salaryFile = "data/${site}/salaries_${sport}.csv"

		if(sport == SPORT_FOOTBALL)
			readInputFootball(projectionFile)
		else if(sport == SPORT_BASEBALL)
			readInputBaseball(projectionFile)

		readSalaries(salaryFile)
		readConsistencies("data/consistency/consistency.csv")

		cleanData()

		initializePositionTypes()
		table.initializeItemsList(positionTypes)

		long start = System.currentTimeMillis()
		generateOptimalTeamMemoization(0, budget, 0, [])
		long end = System.currentTimeMillis()
		println "Computed optimal roster in ${ (end-start)/1000.0 } seconds."

		printOptimalRoster()
	}

	public static void main(String[] args) {
		Predictor.validateInputs(args)

		def usingConsistency = false
		if(args.length >= 6)    usingConsistency = args[5] == "true"

		new MyFantasyAssistantPredictor(site: args[0], projectionSource: args[1], budget: args[2].toInteger(), positionTypes: args[3], sport: args[4], usingConsistency: usingConsistency)
				.run()
	}
}
