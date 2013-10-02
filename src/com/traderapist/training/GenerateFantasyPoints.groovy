package com.traderapist.training

import groovy.sql.Sql

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 9/25/13
 * Time: 8:40 PM
 * To change this template use File | Settings | File Templates.
 */
class GenerateFantasyPoints {
	static String RUSHING_YARDS = "RUSHING_YARDS"
	static String RUSHING_TOUCHDOWNS = "RUSHING_TOUCHDOWNS"
	static String PASSING_YARDS = "PASSING_YARDS"
	static String PASSING_TOUCHDOWNS = "PASSING_TOUCHDOWNS"
	static String INTERCEPTIONS = "INTERCEPTIONS"
	static String RECEPTION_YARDS = "RECEPTION_YARDS"
	static String RECEPTION_TOUCHDOWNS = "RECEPTION_TOUCHDOWNS"
	static String RECEPTIONS = "RECEPTIONS"
	static String RETURN_TOUCHDOWNS = "RETURN_TOUCHDOWNS"
	static String FUMBLES_LOST = "FUMBLES_LOST"
	static String OFFENSIVE_FUMBLE_RETURN_TD = "OFFENSIVE_FUMBLE_RETURN_TD"
	static String TWO_POINT_CONVERSIONS = "TWO_POINT_CONVERSIONS"
	static String FIELD_GOALS_0_19_YARDS = "FIELD_GOALS_0_19_YARDS"
	static String FIELD_GOALS_20_29_YARDS = "FIELD_GOALS_20_29_YARDS"
	static String FIELD_GOALS_30_39_YARDS = "FIELD_GOALS_30_39_YARDS"
	static String FIELD_GOALS_40_49_YARDS = "FIELD_GOALS_40_49_YARDS"
	static String FIELD_GOALS_50_PLUS_YARDS = "FIELD_GOALS_50_PLUS_YARDS"
	static String POINT_AFTER_ATTEMPT_MADE = "POINT_AFTER_ATTEMPT_MADE"
	static String SACK = "SACK"
	static String FUMBLE_RECOVERY = "FUMBLE_RECOVERY"
	static String DEFENSIVE_TOUCHDOWN = "TOUCHDOWN"
	static String KICKOFF_AND_PUNT_RETURN_TOUCHDOWNS = "KICKOFF_AND_PUNT_RETURN_TOUCHDOWNS"
	static String SAFETY = "SAFETY"
	static String BLOCK_KICK = "BLOCK_KICK"
	static String INTERCEPTION = "INTERCEPTION"
	static String POINTS_ALLOWED = "POINTS_ALLOWED"
	static String POINTS_ALLOWED_0 = "POINTS_ALLOWED_0"
	static String POINTS_ALLOWED_1_6 = "POINTS_ALLOWED_1_6"
	static String POINTS_ALLOWED_7_13 = "POINTS_ALLOWED_7_13"
	static String POINTS_ALLOWED_14_20 = "POINTS_ALLOWED_14_20"
	static String POINTS_ALLOWED_28_34 = "POINTS_ALLOWED_28_34"
	static String POINTS_ALLOWED_35_PLUS = "POINTS_ALLOWED_35_PLUS"

	def run() {
		def sql = Sql.newInstance("jdbc:mysql://localhost:3306/fantasy_yahoo", "fantasy", "fantasy", "com.mysql.jdbc.Driver")
		def fpODFL = sql.dataSet("fantasy_points_odfl")

		// Fan Duel
		sql.eachRow("select * from stats_oneline where id not in (select stats_oneline_id from fantasy_points_odfl where odfl_site = ?)", ["FAN_DUEL"]) {
			def points = generateFanDuelPoints(it)

			println "FAN_DUEL\t\t${it.name}/${it.position}/${it.season}/${it.week} - ${points}"
			fpODFL.add(player_id: it.player_id, stats_oneline_id: it.id, season: it.season, week: it.week, odfl_site: "FAN_DUEL", points: points)
		}

		// Draft Kings
		sql.eachRow("select * from stats_oneline where id not in (select stats_oneline_id from fantasy_points_odfl where odfl_site = ?)", ["DRAFT_KINGS"]) {
			def points = generateDraftKingsPoints(it)

			println "DRAFT_KINGS\t\t${it.name}/${it.position}/${it.season}/${it.week} - ${points}"
			fpODFL.add(player_id: it.player_id, stats_oneline_id: it.id, season: it.season, week: it.week, odfl_site: "DRAFT_KINGS", points: points)
		}

		// Draft Street
		sql.eachRow("select * from stats_oneline where id not in (select stats_oneline_id from fantasy_points_odfl where odfl_site = ?)", ["DRAFT_STREET"]) {
			def points = generateDraftStreetPoints(it)

			println "DRAFT_STREET\t\t${it.name}/${it.position}/${it.season}/${it.week} - ${points}"
			fpODFL.add(player_id: it.player_id, stats_oneline_id: it.id, season: it.season, week: it.week, odfl_site: "DRAFT_STREET", points: points)
		}
	}

	def generateFanDuelPoints(row) {
		def points = 0.0

		points += row[RUSHING_YARDS] * 0.1
		points += row[RUSHING_TOUCHDOWNS] * 6
		points += row[PASSING_YARDS] * 0.04
		points += row[PASSING_TOUCHDOWNS] * 4
		points += row[INTERCEPTIONS] * -1
		points += row[RECEPTION_YARDS] * 0.1
		points += row[RECEPTION_TOUCHDOWNS] * 6
		points += row[RECEPTIONS] * 0.5
		points += row[RETURN_TOUCHDOWNS] * 6        // Punts and kicks
		points += row[FUMBLES_LOST] * -2
		points += row[OFFENSIVE_FUMBLE_RETURN_TD] * 6
		points += row[TWO_POINT_CONVERSIONS] * 2
		points += row[FIELD_GOALS_0_19_YARDS] * 3
		points += row[FIELD_GOALS_20_29_YARDS] * 3
		points += row[FIELD_GOALS_30_39_YARDS] * 3
		points += row[FIELD_GOALS_40_49_YARDS] * 4
		points += row[FIELD_GOALS_50_PLUS_YARDS] * 5
		points += row[POINT_AFTER_ATTEMPT_MADE] * 1

		points += row[SACK] * 1
		points += row[FUMBLE_RECOVERY] * 2
		points += row[DEFENSIVE_TOUCHDOWN] * 6
		points += row[KICKOFF_AND_PUNT_RETURN_TOUCHDOWNS] * 6
		points += row[SAFETY] * 2
		points += row[BLOCK_KICK] * 2
		points += row[INTERCEPTION] * 2

		points += row[POINTS_ALLOWED_0] * 10
		points += row[POINTS_ALLOWED_1_6] * 7
		points += row[POINTS_ALLOWED_7_13] * 4
		points += row[POINTS_ALLOWED_14_20] * 1
		points += row[POINTS_ALLOWED_28_34] * -1
		points += row[POINTS_ALLOWED_35_PLUS] * -4

		return points
	}

	def generateDraftKingsPoints(row) {
		def points = 0.0

		points += row[RUSHING_YARDS] * 0.1
		if(row[RUSHING_YARDS] >= 100)
			points += 3

		points += row[RUSHING_TOUCHDOWNS] * 6
		points += row[PASSING_YARDS] * 0.04
		if(row[PASSING_YARDS] >= 300)
			points += 3

		points += row[PASSING_TOUCHDOWNS] * 4
		points += row[INTERCEPTIONS] * -1
		points += row[RECEPTION_YARDS] * 0.1
		if(row[RECEPTION_YARDS] >= 100)
			points += 3

		points += row[RECEPTION_TOUCHDOWNS] * 6
		points += row[RECEPTIONS] * 1
		points += row[RETURN_TOUCHDOWNS] * 6        // Punts and kicks
		points += row[FUMBLES_LOST] * -1
		points += row[TWO_POINT_CONVERSIONS] * 2
		points += row[FIELD_GOALS_0_19_YARDS] * 3
		points += row[FIELD_GOALS_20_29_YARDS] * 3
		points += row[FIELD_GOALS_30_39_YARDS] * 3
		points += row[FIELD_GOALS_40_49_YARDS] * 4
		points += row[FIELD_GOALS_50_PLUS_YARDS] * 5
		points += row[POINT_AFTER_ATTEMPT_MADE] * 1

		points += row[SACK] * 1
		points += row[FUMBLE_RECOVERY] * 2
		points += row[DEFENSIVE_TOUCHDOWN] * 6
		points += row[KICKOFF_AND_PUNT_RETURN_TOUCHDOWNS] * 6
		points += row[SAFETY] * 2
		points += row[BLOCK_KICK] * 2
		points += row[INTERCEPTION] * 2

		points += row[POINTS_ALLOWED_0] * 10
		points += row[POINTS_ALLOWED_1_6] * 7
		points += row[POINTS_ALLOWED_7_13] * 4
		points += row[POINTS_ALLOWED_14_20] * 1
		points += row[POINTS_ALLOWED_28_34] * -1
		points += row[POINTS_ALLOWED_35_PLUS] * -4

		return points
	}

	def generateDraftStreetPoints(row) {
		def points = 0.0

		points += row[RUSHING_YARDS] * 0.1
		points += row[RUSHING_TOUCHDOWNS] * 6
		points += row[PASSING_YARDS] * 0.04
		points += row[PASSING_TOUCHDOWNS] * 4
		points += row[INTERCEPTIONS] * -1
		points += row[RECEPTION_YARDS] * 0.1
		points += row[RECEPTION_TOUCHDOWNS] * 6
		points += row[RECEPTIONS] * 0.5
		points += row[RETURN_TOUCHDOWNS] * 6        // Punts and kicks
		points += row[FUMBLES_LOST] * -1
		points += row[TWO_POINT_CONVERSIONS] * 2

		points += row[SACK] * 0.5
		points += row[FUMBLE_RECOVERY] * 1
		points += row[DEFENSIVE_TOUCHDOWN] * 6
		points += row[KICKOFF_AND_PUNT_RETURN_TOUCHDOWNS] * 6
		points += row[SAFETY] * 2
		points += row[BLOCK_KICK] * 2
		points += row[INTERCEPTION] * 1

		points += row[POINTS_ALLOWED] * -0.5

		return points
	}

	public static void main(String[] args) {
		def gfp = new GenerateFantasyPoints()
		gfp.run()
	}
}
