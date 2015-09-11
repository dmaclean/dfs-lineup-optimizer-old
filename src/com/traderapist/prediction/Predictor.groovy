package com.traderapist.prediction

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/24/13
 * Time: 7:38 PM
 * To change this template use File | Settings | File Templates.
 */
class Predictor {
	/**
	 * The service we are doing predictions for - FanDuel or Draft Kings.
	 */
	def site

	/**
	 * Which sport are we predicting for?
	 *
	 * - baseball
	 * - football
	 * - basketball
	 * - golf
	 */
	def sport

	/**
	 * Our lookup table.
	 */
	def table = new MemoTable()

	/**
	 * A map of the salaries for players, key is player name and value is their salary.
	 * Because the key is the player name, we need to clean the data beforehand to ensure
	 * there are no duplicates.  Usually, a sufficient way is to append the team name
	 * to resolve collisions.
	 */
	def salaries = [:]

	/**
	 * Map of maps containing projections for each position.  The key is the position name
	 * and the value is a map, keyed by player name with a value of their projected point total.
	 *
	 * [
	 *  "QB": ["Matt Ryan": 19.02, "Tom Brady": 21.00, ...],
	 *  "RB": [...],
	 *  ...
	 *  ...
	 * ]
	 */
	def projections = [:]

	/**
	 * Map of players-->consistency stats.
	 */
	def consistency = [:]

	/**
	 * An array of maps that contain the options for positions at each depth.  So, if the
	 * positionTypes string contains QB,RB,RB then the array will have three elements, at
	 * 0 will be a map for QBs, and at 1 and 2 will be the map for RBs.
	 */
	def positionAtDepth = []

	/**
	 * The input string (comma-separated) dictating which positions are used.  For example,
	 * the string might look like QB,RB,RB,WR,WR,TE,DEF,K
	 */
	def positionTypes

	/**
	 * The variable positionTypes as an array.
	 */
	def positionTypesAsArray

	/**
	 * The best point total encountered so far for all roster configurations.
	 */
	def bestPoints = 0

	/**
	 * The roster of players that comprise the optimal lineup so far.
	 */
	def bestRosters = []

	/**
	 * A map that keeps track of which indices in the roster are for each position type.
	 * The key is the position and value is an array of indices.
	 */
	def positionIndices = [:]

	/**
	 * Keeps track of which player is currently being evaluated at each roster position.
	 */
	def indexTracker = []

	/**
	 * Variables to keep statistics for our lookup table.  The tableHitRate array helps keep track
	 * of the percentage of all table hits that are encountered at each depth.
	 */
	def tableHits = 0
	def tableMisses = 0
	def tableHitRate = []

	/**
	 * Keeps track of how much money, at any given point, we have left
	 * to spend on remaining players to fill the roster.
	 */
	def budget

	/**
	 * Flag to determine if we are taking consistency/ceiling into account.
	 */
	def usingConsistency

	/**
	 * The minimum salary an athlete can be at and still be considered for rostering.  This is
	 * helpful for cash games to create a more balanced lineup.
	 */
	def minUsableSalary

	/**
	 * The maximum salary an athlete can be at and still be considered for rostering.  This is
	 * helpful for cash games to create a more balanced lineup.
	 */
	def maxUsableSalary

	/**
	 * Flag to indicate if we want to parse FantasyPros projections or use what is in the CSVs.
	 */
	def useFantasyPros

	/**
	 * The lowest-priced player at each position.
	 */
	def minCost = [:]

	def minTotalCost = []

	def count = 0

	static def DRAFT_KINGS = "DRAFT_KINGS"
	static def FAN_DUEL = "FAN_DUEL"
    static def VICTIV = "VICTIV"
	static def YAHOO = "YAHOO"

	static def SPORT_BASEBALL = "baseball"
	static def SPORT_FOOTBALL = "football"
	static def SPORT_BASKETBALL = "basketball"
    static def SPORT_GOLF = "golf"

	def initializePositionTypes() {
		def pieces = positionTypes.split(",")

		positionTypesAsArray = pieces

		pieces.eachWithIndex { position, i ->
			positionAtDepth << projections[position]

			// Manage position indices
			(!positionIndices.containsKey(position)) ? positionIndices[position] = [i] : positionIndices[position] << i

			indexTracker << 0

			minTotalCost << 0

			tableHitRate << 0
		}

		def costSoFar = 0
		for(int i=minTotalCost.size()-1; i>= 0; i--) {
			def minCostAtIndex = minCost[positionTypesAsArray[i]]
			costSoFar += minCostAtIndex
			minTotalCost[i] = costSoFar
		}
	}

	/**
	 * Expect input in the form of <name>,<position>,<fantasy points>,<salary>
	 *
	 * FOOTBALL (NF predictions)
	 * <tr.*?><td.*?><a.*?>(.*?) \(([A-Z]+), [A-Z]+\)<\/a>.*?(<td class="nf strong">(.*?)<\/td>)<td.*?>.*?<\/td><td>\$(\d+)<\/td>.*?<\/tr>
	 * \1,\2,\4,\5\n
	 *
	 * (Site predictions)
	 * <tr.*?><td.*?><a.*?>(.*?) \(([A-Z]+), [A-Z]+\)<\/a>.*?(<td class="nf strong">(.*?)<\/td>)<td class="sep">([\d\.]+)<\/td><td>\$(\d+)<\/td>.*?<\/tr>
	 * \1,\2,\5,\6\n
	 *
	 * BASEBALL
	 * <tr.*?><td.*?>\*?<a.*?>(.*?) \(([\d\/A-Z]+), [A-Z]+\)<\/a><\/td>(<td.*?>[\d\.]+<\/td>){0,13}<td class="sep">([\d\.]+)<\/td><td>\$(\d+)<\/td>.*?<\/tr>
	 * \1,\2,\4,\5\n
	 *
	 * @param file
	 * @return
	 */
	def readInputFootball(List<String> data) {
		projections["FLEX"] = [:]

		// Use this as a short-term solution for dealing with early/late slates and excluding athletes.
		// Just put the team or opponent abbreviation in there and it'll disregard the athlete.
		def filter = []

		data.each { line ->
			def pieces = line.split(",")
			def name = pieces[0]

			if(pieces.length >= 6 && filter.contains(pieces[5])) {
				return
			}

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

    def readInputGolf(file) {
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
        }
    }

	/**
	 * Take the input data for optimizations and parse names, positions, projections, and salaries.
	 *
	 * Expect input in the form of <name>,<position>,<fantasy points>,<salary>
	 *
	 * @param data		A list of Strings in the format of <name>,<position>,<fantasy points>,<salary>
	 */
	def readInputBaseball(List<String> data) {
		projections["FLEX"] = [:]

		// Use this as a short-term solution for dealing with early/late slates and excluding athletes.
		// Just put the team or opponent abbreviation in there and it'll disregard the athlete.
		def filter = []

		data.each { line ->
			def pieces = line.split(",")
			def name = pieces[0]

			if(pieces.length >= 6 && filter.contains(pieces[5])) {
				return
			}

			/*
			 * Grab the salary and figure out if this is the lowest value for this position.
			 */
			def salary = Integer.parseInt(pieces[3])
			if(salary == 0 || salary < minUsableSalary || salary > maxUsableSalary)
				return

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
				if(position.matches("LF|CF|RF")) {
					position = "OF"
				}
				else if(position.matches("DH")) {
					position = "1B"
				}
				else if(position.matches("SP|RP")) {
					position = "P"
				}

                if(site == VICTIV && position.matches("C|1B|2B|3B|SS")) {
                    position = "IF";
                }

				salaries[name] = salary
				if(!minCost.containsKey(position))
					minCost[position] = Integer.MAX_VALUE

				if(salary > 0 && salary < minCost[position])
					minCost[position] = salary

				/*
				 * Check if the map of projections for this position exists, and if not, create it.
				 */
				if(!projections.containsKey(position))
					projections[position] = [:]
				projections[position][name] = Double.parseDouble(pieces[2])

				if(position.matches("C|1B|2B|3B|SS|OF")) {
					projections["FLEX"][name] = Double.parseDouble(pieces[2])
				}
			}
		}

        if(site != VICTIV) {
            minCost["FLEX"] = Math.min(minCost["C"],
                    Math.min(minCost["1B"],
                            Math.min(minCost["2B"],
                                    Math.min(minCost["3B"],
                                            Math.min(minCost["SS"], minCost["OF"])))))
        } else {
            minCost["FLEX"] = Math.min(minCost["IF"], minCost["OF"])
        }
	}

	def readInputBasketball(file) {
		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[0]

			/*
			 * Grab the salary and figure out if this is the lowest value for this position.
			 */
			def salary = Integer.parseInt(pieces[3])
			if(salary == 0)
				return

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
				if(position.matches("LF|CF|RF")) {
					position = "OF"
				}
				else if(position.matches("DH")) {
					position = "1B"
				}
				else if(position.matches("SP|RP")) {
					position = "P"
				}

				salaries[name] = salary
				if(!minCost.containsKey(position))
					minCost[position] = Integer.MAX_VALUE

				// Account for G and F
				if(position.matches("SF|PF")) {
					if(!minCost.containsKey("F")) {
						minCost["F"] = Integer.MAX_VALUE
					}
					if(salary > 0 && salary < minCost["F"]) {
						minCost["F"] = salary
					}
				} else if(position.matches("PG|SG")) {
					if(!minCost.containsKey("G")) {
						minCost["G"] = Integer.MAX_VALUE
					}
					if(salary > 0 && salary < minCost["G"]) {
						minCost["G"] = salary
					}
				}

				if(salary > 0 && salary < minCost[position])
					minCost[position] = salary

				if(!minCost.containsKey("FLEX")) {
					minCost["FLEX"] = Integer.MAX_VALUE
				}
				if(salary > 0 && salary < minCost["FLEX"])
					minCost["FLEX"] = salary

				/*
				 * Check if the map of projections for this position exists, and if not, create it.
				 */
				if(!projections.containsKey(position))
					projections[position] = [:]
				projections[position][name] = Double.parseDouble(pieces[2])

				if(position.matches("SF|PF")) {
					if(!projections.containsKey("F")) {
						projections["F"] = [:]
					}
					projections["F"][name] = Double.parseDouble(pieces[2])
				} else if(position.matches("PG|SG")) {
					if(!projections.containsKey("G")) {
						projections["G"] = [:]
					}
					projections["G"][name] = Double.parseDouble(pieces[2])
				}

				if(!projections.containsKey("FLEX")) {
					projections["FLEX"] = [:]
				}
				projections["FLEX"][name] = Double.parseDouble(pieces[2])
			}
		}
	}

	def reportTableStats() {
		def total = tableHits + tableMisses
		if(total % 100000 == 0 && tableHits > 0) {
			def s = "Table hits ${ tableHits/total }% \t\tTable misses ${ tableMisses/total }%\t\t\t("
			table.items.eachWithIndex{ ArrayList item, int i ->
				s += "${item.size()}"
				if(i != table.items.size() -1) {
					s += "/"
				}
				else {
					s += ")\t\t\t("
				}
			}

			tableHitRate.eachWithIndex{ rate, i ->
				s += "${rate/tableHits}"
				if(i != tableHitRate.size() -1) {
					s += "/"
				}
				else {
					s += ")"
				}
			}

			println s
		}
	}

	/**
	 * Print out the current best roster configuration to the console.
	 */
	def printOptimalRoster() {
		println "Found best configuration so far with ${ bestPoints } for the following players:"

		bestRosters.each { roster ->
			def sal = 0
			roster.eachWithIndex { p, i ->
				def points = projections[positionTypesAsArray[i]][p]
				println "\t${ p } (${ salaries[p] } - ${ points })"
				sal += salaries[p]
			}
			println "==========================\n\tTotal salary: ${sal}"
		}
	}

	/**
	 * Evaluates the previous roster indices that contain the same position
	 * to make sure the currently-chosen player is not already in the roster.
	 *
	 * @param depth     The current roster index that we're at.
	 * @param player    The player being considered.
	 * @param roster    The current roster, minus the player being considered.
	 * @return          True, if the player name already exists in the roster.  False, otherwise.
	 */
	def isDuplicate(depth, player, roster) {
		if(sport == SPORT_FOOTBALL) {
			def position = positionTypesAsArray[depth]
			def foundDupe = false

			for(index in positionIndices[position]) {
				if(index >= depth)
					break

				if(roster[index] == player) {
					foundDupe = true
					break
				}
			}

			if(position.matches("RB|WR|TE")) {
				for(index in positionIndices["FLEX"]) {
					if(index >= depth)
						break

					if(roster[index] == player) {
						foundDupe = true
						break
					}
				}
			}

			return foundDupe
		}
		else {
			for(currPlayer in roster) {
				if(player == currPlayer)
					return true
			}

			return false
		}
	}

	def isCorrectStartingIndex(depth, index) {
		// Is there a previous roster entry for this position?
		def position = positionTypesAsArray[depth]
		def closestIndex = -1
//		def i=0     // Where in positionIndices are we?
		for(currIdx in positionIndices[position]) {
			if(currIdx == depth)
				break
			if(currIdx > closestIndex && currIdx < depth) {
				closestIndex = currIdx
			}
		}

		// No previous index or the previous one is pointing to a player that is
		// earlier in the list than the player at this index.
		//
		// Whatever was passed in is fine.
		if(closestIndex == -1 || indexTracker[closestIndex] < index) {
			return true
		}

		return false
	}

	/**
	 * Dynamic programming solution for determining the optimal roster.
	 *
	 * @param depth
	 * @param budget
	 * @param totalPoints
	 * @param roster
	 * @return
	 */
	def generateOptimalTeamMemoization(depth, budget, totalPoints, roster) {

		if(depth == table.items.size()-1) {
			def bestPointsForFlex = 0
			def best = null

			/*
			 * Do we have a pre-computed solution for this budget?
			 */
			def result = table.getSolution(depth, budget)
			if(result) {
				tableHits++
				tableHitRate[depth]++
				return result
			}
			else {
				tableMisses++
			}

			reportTableStats()

			/*
			 * No pre-computed solution.  Go through each option and determine the most valuable
			 * for the budget provided.
			 */
			indexTracker[depth] = 0
			for(e in positionAtDepth[depth]) {
				def name = e.key
				if(isDuplicate(depth, name, roster) || !isCorrectStartingIndex(depth, indexTracker[depth])) {
					indexTracker[depth]++
					continue
				}

				if(salaries[name] <= budget && e.value > bestPointsForFlex) {
					bestPointsForFlex = e.value
					best = name
				}

				indexTracker[depth]++
			}

			def newItem
			if(best) {
				//noinspection GroovyAssignabilityCheck
				newItem = new MemoItem(cost: budget, points: bestPointsForFlex, roster: [best])

				// Write to table
				table.writeSolution(depth, newItem)

				if(table.items.size() == 1) {
					bestRosters = [newItem.roster]
					printOptimalRoster()
				}
			}

			return newItem
		}
		else {
			def result = table.getSolution(depth, budget)
			if(result) {
				tableHits++
				tableHitRate[depth]++
				return result
			}
			else {
				tableMisses++
			}

			reportTableStats()

			indexTracker[depth] = 0
			for(e in positionAtDepth[depth]) {
				if(isDuplicate(depth, e.key, roster) || !isCorrectStartingIndex(depth, indexTracker[depth])) {
					indexTracker[depth]++
					continue
				}

				// Can I afford this player?
				def cost = salaries[e.key]
				def points = e.value
				if(cost <= budget) {
					result = null
					if(budget-cost >= minTotalCost[depth+1]) {
						roster << e.key
						result = generateOptimalTeamMemoization(depth+1, budget - cost, totalPoints + points, roster)
						roster.remove(roster.size()-1)

						// Do we have an optimal solution?
						if(result && totalPoints + e.value + result.points > bestPoints) {
							def newRoster = []
							newRoster.addAll(roster)
							newRoster.add(e.key)
							newRoster.addAll(result.roster)
							Set set = new HashSet(newRoster)

							if(set.size() == newRoster.size()) {
								bestPoints = totalPoints + e.value + result.points
								bestRosters = [newRoster]

								printOptimalRoster()
							}
						}
						else if(result && totalPoints + e.value + result.points == bestPoints) {
							def newRoster = []
							newRoster.addAll(roster)
							newRoster.add(e.key)
							newRoster.addAll(result.roster)
							Set set = new HashSet(newRoster)

							if(set.size() == newRoster.size()) {
								bestRosters << newRoster

								printOptimalRoster()
							}
						}

						if(result) {
							def newItem = new MemoItem(cost: salaries[e.key] + result.cost, points: e.value + result.points, roster: [e.key])
							newItem.roster.addAll(result.roster)

							Set s = new HashSet(newItem.roster)

							if(s.size() == newItem.roster.size()) {
								table.writeSolution(depth, newItem)
							}
						}
					}
				}

				indexTracker[depth]++
			}
		}
	}

	def generateOptimalTeamByBruteForce(depth, roster) {
		indexTracker[depth] = 0

		for(e in positionAtDepth[depth]) {
			if(isDuplicate(depth, e.key, roster)) {
				continue
			}

			roster << e.key

			if(depth == positionAtDepth.size()-1) {
				def total = 0
				def points = 0.0
				def over = false
				def index = 0
				for(name in roster) {
					total += salaries[name]
					points += positionAtDepth[index][name]
					if(total > budget) {
						over = true
						break
					}

					index++
				}

				if(!over && points > bestPoints) {
					bestPoints = points
					bestRosters = [roster]

					printOptimalRoster()
				}

				count++

				if(count % 1000000 == 0) {
					println "Processed ${count} roster combinations."
				}
			}
			else {
				generateOptimalTeamByBruteForce(depth+1, roster)
			}

			roster.remove(roster.size()-1)

			indexTracker[depth]++
		}
	}

	/**
	 * Remap position names for baseball.
	 *
	 * @param position      The position to remap.
	 * @return              The new position name.
	 */
	def normalizeBaseballPosition(position) {
        if(site == VICTIV && position.matches("C|1B|2B|3B|SS")) {
            return "IF";
        }

        if(position.matches("LF|CF|RF")) {
			return "OF"
		}
		else if(position.matches("DH")) {
			return "1B"
		}
		else if(position.matches("SP|RP")) {
			return "P"
		}

		return position
	}

	def normalizeFootballTeamName(name) {
		if(site == FAN_DUEL) {
			def team = name.split(" ")
			return team[team.length-1]
		}

		return name
	}

	def run() {
		// First read in what's stored in the CSV file.
		def file = "data/${site}_${sport}.csv"
		def data = new File(file).readLines()

		// If we're using FantasyPros, go scrape that and override the data array.
		if(useFantasyPros) {
			if(sport == SPORT_BASEBALL) {
				data = scrapeFantasyPros()
			} else if(sport == SPORT_FOOTBALL) {
				data = scrapeFantasyProsNfl()
			}
		}

		if(sport == SPORT_FOOTBALL)
			readInputFootball(data)
		else if(sport == SPORT_BASEBALL)
			readInputBaseball(data)
		else if(sport == SPORT_BASKETBALL)
			readInputBasketball(file)
        else if(sport == SPORT_GOLF)
            readInputGolf(file)

		initializePositionTypes()
		table.initializeItemsList(positionTypes)

		long start = System.currentTimeMillis()
		generateOptimalTeamMemoization(0, budget, 0, [])
//		generateOptimalTeamByBruteForce(0,[])
		long end = System.currentTimeMillis()
		println "Computed optimal roster in ${ (end-start)/1000.0 } seconds."

		printOptimalRoster()
	}

	/**
	 * Scrape FantasyPros projections and format them into the <name>,<position>,<projection>,<salary> strings
	 * that can be processed by the optimizer.
	 *
	 * @return	A list of comma-separated values; one for each athlete.
	 */
	def scrapeFantasyPros() {
		// Determine the name of the cheat sheet to use based on the site we're creating lineups for.
		def siteUrl = site == YAHOO ? "yahoo" : site == DRAFT_KINGS ? "draftkings" : "fanduel"

		// Process Pitchers
		def pitchersByFantasyPoints = []
		def playersData = []
		def data = new URL("http://www.fantasypros.com/mlb/${siteUrl}-cheatsheet.php").getText()
		Document doc = Jsoup.parse(data)
		Element table = doc.getElementById("data-table")
		def processedFirstRow = false
		table.getElementsByTag("tr").each {Element tr ->
			if(!processedFirstRow) {
				processedFirstRow = true
				return
			}

			def playerData = ""
			def time = null
			def team = null
			def opponent = null
			tr.getElementsByTag("td").eachWithIndex {Element td, i ->
				if(i == 0) {
					playerData += td.getElementsByTag("a").get(0).text() + ",P,"
					team = td.getElementsByTag("small").get(0).text().replace("(", "").replace(" - P)", "")
				} else if(i == 2) {
					time = td.text()
				} else if(i == 3) {
					opponent = td.text().replace("@","")
				} else if(i == 10) {
					playerData += td.text().replace(" pts", "") + ","
				} else if(i == 11) {
					playerData += td.text().replace("\$", "").replace(",","") + ","
				}

			}
			playerData += time + "," + opponent

			// The closers get listed in the pitcher list as well.  Since the list is sorted by fantasy points,
			// we know the starters will be listed first
			if(pitchersByFantasyPoints.contains(team)) {
				return
			}
			playersData.add(playerData)
			pitchersByFantasyPoints.add(team)
		}

		// Create a filter for the top half of pitchers.  If a batter is facing one of these pitchers/teams, skip them.
		pitchersByFantasyPoints = pitchersByFantasyPoints.subList(0, pitchersByFantasyPoints.size()/2 as int)

		// Process batters
		data = new URL("http://www.fantasypros.com/mlb/${siteUrl}-cheatsheet.php?position=H").getText()
		doc = Jsoup.parse(data)
		table = doc.getElementById("data-table")
		processedFirstRow = false
		table.getElementsByTag("tr").each { Element tr ->
			if (!processedFirstRow) {
				processedFirstRow = true
				return
			}

			def playerData = ""
			def time = null
			def battingOrder = null
			def opponent = null
			def skip = false
			tr.getElementsByTag("td").eachWithIndex { Element td, i ->
				if (i == 0) {
					playerData += td.getElementsByTag("a").get(0).text() + ","
					playerData += td.getElementsByTag("small").get(0).text().replaceFirst("\\(\\w+ - ", "").replace(")", "") + ","
				} else if (i == 2) {
					battingOrder = td.text()
				} else if (i == 3) {
					time = td.text()
				} else if (i == 4) {
					opponent = td.text().replace("@","")

					// Batter has less-than optimal matchup.  Skip them.
					if(pitchersByFantasyPoints.contains(opponent)) {
						skip = true
					}
				} else if (i == 5 && td.className().equals("tough-highlighted")) {
					return;
				} else if (i == 11) {
					playerData += td.text().replace(" pts", "") + ","
				} else if (i == 12) {
					playerData += td.text().replace("\$", "").replace(",", "") + ","
				}
			}

			// Don't add the batter if they're not in the lineup
			if(skip || battingOrder.equals("X")) {
				return
			}

			playerData += time + "," + opponent
			playersData.add(playerData)
		}

		return playersData
	}

	/**
	 * Scrape FantasyPros projections and format them into the <name>,<position>,<projection>,<salary> strings
	 * that can be processed by the optimizer.
	 *
	 * @return	A list of comma-separated values; one for each athlete.
	 */
	def scrapeFantasyProsNfl() {
		// Determine the name of the cheat sheet to use based on the site we're creating lineups for.
		def siteUrl = site == DRAFT_KINGS ? "draftkings" : "fanduel"

		def positions = ["QB", "RB", "WR", "TE", "K", "DEF"]
		def playersData = []
		positions.each {position ->
			if(site == DRAFT_KINGS && position == "K") {
				return
			}

			// The rest of the script uses "DEF" to indicate a defense, but FantasyPros uses "DST" in their URL.
			def adjustedPositionForUrl = position == "DEF" ? "DST" : position
			def data = new URL("http://www.fantasypros.com/nfl/${siteUrl}-cheatsheet.php?position=${adjustedPositionForUrl}").getText()
			Document doc = Jsoup.parse(data)
			Element table = doc.getElementById("data-table")
			def processedFirstRow = false
			table.getElementsByTag("tr").each {Element tr ->
				if(!processedFirstRow) {
					processedFirstRow = true
					return
				}

				def playerData = ""
				def time = null
				def team = null
				def opponent = null
				tr.getElementsByTag("td").eachWithIndex {Element td, i ->
					if(i == 0) {
						playerData += td.getElementsByTag("a").get(0).text()
						playerData += ",${position},"
						team = td.getElementsByTag("small").get(0).text().replace("(", "").replace(" - ${position})", "")
					} else if(i == 1) {
						time = td.text()
					} else if(i == 2) {
						opponent = td.text().replace("@","")
					} else if(i == 10) {
						def projection = td.text().replace(" pts", "")
						if(projection == "") {
							projection = 0
						}
						playerData += projection + ","
					} else if(i == 11) {
						playerData += td.text().replace("\$", "").replace(",","") + ","
					}

				}
				playerData += time + "," + opponent
				playersData.add(playerData)
			}
		}

		return playersData
	}

	def readConsistencies(file) {
		new File(file).eachLine { line ->
			def pieces = line.split(",")

			def name = pieces[0].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()
			def position = pieces[1]
			def floor = Double.parseDouble(pieces[14])
			def ceiling = Double.parseDouble(pieces[15])
			def consistent = Double.parseDouble(pieces[16])

			if( (position == "QB" && consistent >= 60) || (position != "QB" && consistent >= 50)) {
				consistency[name] = consistent
			}
		}
	}

	static def validateInputs(args) {
		if(args.length < 4 || !args[0].matches("${FAN_DUEL}|${DRAFT_KINGS}|${VICTIV}|${YAHOO}") ||
				!args[1].matches("\\d+") || !args[3].matches("${SPORT_BASEBALL}|${SPORT_FOOTBALL}|${SPORT_BASKETBALL}|${SPORT_GOLF}")) {
			println "Usage: Predictor <FAN_DUEL|DRAFT_KINGS|VICTIV|YAHOO> <budget> <roster types> <baseball|basketball|football|golf> <min salary> <max salary> <use fantasy-pros>"
			return false
		}

		return true
	}

	public static void main(String[] args) {
		validateInputs(args)

		def minUsableSalary = 0
		def maxUsableSalary = Integer.MAX_VALUE
		def useFantasyPros = false
		if(args.length >= 5)	useFantasyPros = true
		if(args.length >= 6)    minUsableSalary = Integer.parseInt(args[5])
		if(args.length >= 7)    maxUsableSalary = Integer.parseInt(args[6])

		def p = new Predictor(site: args[0], budget: args[1].toInteger(), positionTypes: args[2], sport: args[3],
				minUsableSalary: minUsableSalary, maxUsableSalary: maxUsableSalary, useFantasyPros: useFantasyPros)

		p.run()
	}
}
