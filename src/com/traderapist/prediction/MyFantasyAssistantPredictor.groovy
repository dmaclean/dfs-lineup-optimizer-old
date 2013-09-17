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
			def name = (pieces[1] == "DEF") ? FootballTeamLookup.cityToTeamName[pieces[0]] : pieces[0].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()

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
			for(p in positions) {
				if(!projections[p].containsKey(name)) {
					println "Could not find a projection for ${name}"
					return false
				}

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

	def cleanData() {
		// Clear out any salaries that don't have projections.
		def deletes = []
		salaries.each { name, salary ->
			def points = null

			projections.each { position, map ->
				if(!points && (points = map[name]) != null) {
					return
				}
			}

			if(!points) {
				println "Could not find projection for ${ name }, deleting..."
				deletes << name
			}
		}

		deletes.each { name -> salaries.remove(name) }

		// Clear out any projections that don't have salaries
		deletes.clear()
		projections.each { position, map ->
			map.each { k,v -> if(!salaries[k]) deletes << k}
			deletes.each { name -> map.remove(name) }
		}
	}

	def normalizeFootballTeamName(name) {
		if(site == FAN_DUEL) {
			def team = name.split(" ")
			return team[team.length-1]
		}

		return name
	}

	def run() {
		def projectionFile = "data/${projectionSource}/${sport}.csv"
		def salaryFile = "data/${site}/salaries_${sport}.csv"

		if(sport == SPORT_FOOTBALL)
			readInputFootball(projectionFile)
		else if(sport == SPORT_BASEBALL)
			readInputBaseball(projectionFile)

		readSalaries(salaryFile)

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

		new MyFantasyAssistantPredictor(site: args[0], projectionSource: args[1], budget: args[2].toInteger(), positionTypes: args[3], sport: args[4])
				.run()
	}
}
