package com.traderapist.prediction

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 9/17/13
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
class DailyFantasyProjectionsPredictor extends Predictor {
	def positionsMap = [:]

	/**
	 * Expect input in the form of <name>,<fantasy Points>
	 *
	 * ([\w\s\.\-]+),\s+\w+\s+([\d\.\-]+)
	 * \1,\2
	 *
	 * @param file
	 */
	def readInputFootball(file) {
		projections["FLEX"] = [:]

		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[0].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()

			/*
			 * For baseball, we need to separate players that have two different positions
			 * listed, such as 2B/RF.
			 */
			def positions = positionsMap[name]
			if(!positions)
				return

			for(position in positions) {
				/*
				 * Check if the map of projections for this position exists, and if not, create it.
				 */
				if(!projections.containsKey(position))
					projections[position] = [:]
				projections[position][name] = Double.parseDouble(pieces[1])

				if(position.matches("RB|WR|TE")) {
					projections["FLEX"][name] = Double.parseDouble(pieces[1])
				}
			}
		}

		// Figure out the smallest FLEX cost
		minCost["FLEX"] = Math.min(minCost["RB"], Math.min(minCost["WR"], minCost["TE"]))
	}

	/**
	 * Expect input in the form of <name>,<position>,<fantasy points>
	 *
	 * (\w+), ([\w\.-]+)\s+([\d\.]+)
	 * \2 \1,\3
	 *
	 * IMPORTANT: Because we don't get positions along with the names and projections, we
	 * need to run the salaries first so we can get a
	 *
	 * @param file
	 */
	def readInputBaseball(file) {
		projections["FLEX"] = [:]

		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[0].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()

			/*
			 * For baseball, we need to separate players that have two different positions
			 * listed, such as 2B/RF.
			 */
			def positions = positionsMap[name]
			if(!positions)
				return

			for(position in positions) {
				position = normalizeBaseballPosition(position)

				/*
				 * Check if the map of projections for this position exists, and if not, create it.
				 */
				if(!projections.containsKey(position))
					projections[position] = [:]
				projections[position][name] = Double.parseDouble(pieces[1])

				if(position.matches("C|1B|2B|3B|SS|OF")) {
					projections["FLEX"][name] = Double.parseDouble(pieces[1])
				}
			}
		}

		minCost["FLEX"] = Math.min(minCost["C"],
				Math.min(minCost["1B"],
						Math.min(minCost["2B"],
								Math.min(minCost["3B"],
										Math.min(minCost["SS"], minCost["OF"])))))
	}

	/**
	 * Expect input in the form of <name>,<position>,<salary>
	 *
	 * FanDuel
	 * <tr.*?><td.*?><span>(\w+)<\/span><\/td><td><div.*?>([\w\. ]+)(<span class="icon-\w+">\w+<\/span>)?<\/div><\/td><td>.*?<\/td><td>.*?<\/td><td>.*?<\/td><td>\$(.*?),(\d+)<\/td>.*?<\/tr>
	 * \2,\1,\4\5\n
	 *
	 * @return
	 */
	def readSalaries(file) {
		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[0].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()
			def position = pieces[1]
			def salary = pieces[2].toInteger()

			/*
			 * Football defense names come in different forms depending on the site we're using.
			 * For example, Draft Kings uses "<team name>" while FanDuel uses "<city> <team name>".
			 *
			 * We need to normalize this so defenses don't get filtered out when we do data cleaning.
			 */
			if(sport == SPORT_FOOTBALL && position == "DEF")
				name = normalizeFootballTeamName(name)

			if(salaries.containsKey(name)) {
				println "Salaries already contains ${ name }"
				return false
			}

			def positions = (position.contains("/")) ? position.split("/") : [position]
			// Add name/position combo to map
			positionsMap[name] = positions

			for(p in positions) {
				/*
				 * For baseball, make sure we normalize the positions because sometimes, for
				 * instance, a pitcher will come over as SP or RP and needs to be P.
				 */
				if(sport == SPORT_BASEBALL)
					p = normalizeBaseballPosition(p)

//				if(!projections[p].containsKey(name)) {
//					println "Could not find a projection for ${name}"
//					continue
//				}

				salaries[name] = salary
				if(!minCost.containsKey(p))
					minCost[p] = Integer.MAX_VALUE

				if(salary > 0 && salary < minCost[p])
					minCost[p] = salary

				salaries[name] = salary
			}
		}

		// Figure out the smallest FLEX cost
		if(sport == SPORT_FOOTBALL)
			minCost["FLEX"] = Math.min(minCost["RB"], Math.min(minCost["WR"], minCost["TE"]))
	}

	def run() {
		def projectionFile = "data/${projectionSource}/${sport}.csv"
		def salaryFile = "data/${site}/salaries_${sport}.csv"

		readSalaries(salaryFile)

		if(sport == SPORT_FOOTBALL)
			readInputFootball(projectionFile)
		else if(sport == SPORT_BASEBALL)
			readInputBaseball(projectionFile)

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

		new DailyFantasyProjectionsPredictor(site: args[0], projectionSource: args[1], budget: args[2].toInteger(), positionTypes: args[3], sport: args[4])
				.run()
	}
}
