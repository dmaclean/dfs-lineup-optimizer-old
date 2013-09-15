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
	 * <tr.*?><td.*?><a.*?>(.*?) \(([A-Z]+), [A-Z]+\)<\/a>.*?(<td class="nf strong">(.*?)<\/td>)<td.*?>.*?<\/td><td>\$(\d+)<\/td>.*?<\/tr>
	 * \1,\2,\4,\5\n
	 *
	 * @param file
	 * @return
	 */
	def readInputFootball(file) {
		projections["FLEX"] = [:]

		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[0]

			/*
			 * Grab the salary and figure out if this is the lowest value for this position.
			 */
			def salary = Integer.parseInt(pieces[3])
			if(salary == 0)
				return

			salaries[name] = salary
			if(!minCost.containsKey(pieces[1]))
				minCost[pieces[1]] = Integer.MAX_VALUE

			if(salary > 0 && salary < minCost[pieces[1]])
				minCost[pieces[1]] = salary

			/*
			 * Check if the map of projections for this position exists, and if not, create it.
			 */
			if(!projections.containsKey(pieces[1]))
				projections[pieces[1]] = [:]
			projections[pieces[1]][name] = Double.parseDouble(pieces[2])

			/*
			 * For football, add RB, WR, and TEs to the flex position.
			 */
			if(pieces[1].matches("RB|WR|TE"))
				projections["FLEX"][name] = Double.parseDouble(pieces[2])
		}

		// Figure out the smallest FLEX cost
		minCost["FLEX"] = Math.min(minCost["RB"], Math.min(minCost["WR"], minCost["TE"]))
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
	def readSalaries() {
		def file = "data/${site}/salaries_${sport}.csv"

		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[0].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()
			def salary = pieces[2].toInteger()

			if(salaries.containsKey(name)) {
				println "Salaries already contains ${ name }"
				return false
			}

			def position
			projections.each { pos, proj ->
				if(position) return

				if(proj.containsKey(name)) {
					position = pos
				}
			}

			if(!position) {
				println "Could not find a projection for ${name}"
				return false
			}

			salaries[name] = salary
			if(!minCost.containsKey(position))
				minCost[position] = Integer.MAX_VALUE

			if(salary > 0 && salary < minCost[position])
				minCost[position] = salary

			salaries[name] = salary
		}
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

	def run() {
		def file = "data/${projectionSource}/${site}_${sport}.csv"

		if(sport == SPORT_FOOTBALL)
			readInputFootball(file)
		else if(sport == SPORT_BASEBALL)
			readInputBaseball(file)

		readSalaries()

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
