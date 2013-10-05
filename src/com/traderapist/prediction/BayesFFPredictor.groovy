package com.traderapist.prediction

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 10/2/13
 * Time: 9:53 AM
 * To change this template use File | Settings | File Templates.
 */
class BayesFFPredictor extends Predictor {
	/**
	 * Expect input in the form of <name>,<fantasy Points>
	 *
	 * <tr.*?>.*?<td class="column-2\s+">([\w\s\.\-]+)<\/td><td class="column-3\s+">(\w+)<\/td>.*?<td class="column-9\s+">([\d\.]*)<\/td>.*?<\/tr>
	 * \1,\2,\3\n
	 *
	 * @param file
	 */
	def readInputFootball(file) {
		projections["FLEX"] = [:]

		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def positions = pieces[1].split("/")
			def projection = 0.0
			try {
				if(pieces.size() > 2) {
					projection = Double.parseDouble(pieces[2])
				}
			}
			catch(Exception e) {  println "Invalid projection ${pieces[2]}... ignoring."  }

			/*
			 * For baseball, we need to separate players that have two different positions
			 * listed, such as 2B/RF.
			 */
			for(position in positions) {
				def name = (position == "DEF") ? pieces[0].toLowerCase() : "${pieces[0].toLowerCase()}_${position}"

				/*
				 * Check if the map of projections for this position exists, and if not, create it.
				 */
				if(!projections.containsKey(position))
					projections[position] = [:]
				projections[position][name] = projection

				if(position.matches("RB|WR|TE")) {
					projections["FLEX"][name] = projection
				}
			}
		}

		// Figure out the smallest FLEX cost
		minCost["FLEX"] = Math.min(minCost["RB"], Math.min(minCost["WR"], minCost["TE"]))
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
			def position = pieces[1]
			def salary = pieces[2].toInteger()

			/*
			 * Football defense names come in different forms depending on the site we're using.
			 * For example, Draft Kings uses "<team name>" while FanDuel uses "<city> <team name>".
			 *
			 * We need to normalize this so defenses don't get filtered out when we do data cleaning.
			 */
			def name = transformName(pieces[0], position)

			if(salaries.containsKey(name)) {
				println "Salaries already contains ${ name }"
				return false
			}

			def positions = (position.contains("/")) ? position.split("/") : [position]
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

	def transformName(input, position) {
		def name = input.replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()
		if(position != "DEF") {
			def namePieces = name.split(" ")
			namePieces[0] = namePieces[0][0] + "."
			name = "${namePieces.join("")}_${position}"
		}
		else {
			name = normalizeFootballTeamName(name)
		}

		return name
	}

	def run() {
		def projectionFile = "data/${projectionSource}/${sport}.csv"
		def salaryFile = "data/${site}/salaries_${sport}.csv"

		readSalaries(salaryFile)

		readInputFootball(projectionFile)

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

		new BayesFFPredictor(site: args[0], projectionSource: args[1], budget: args[2].toInteger(), positionTypes: args[3], sport: args[4])
				.run()
	}
}
