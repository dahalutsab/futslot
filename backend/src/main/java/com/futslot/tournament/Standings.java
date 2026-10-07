package com.futslot.tournament;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * League and group tables from decided matches. Ordered by points, then goal difference, then goals
 * scored, then name.
 */
final class Standings {

	private Standings() {
	}

	static final class Row {

		final Registration team;

		int played;

		int won;

		int drawn;

		int lost;

		int goalsFor;

		int goalsAgainst;

		int points;

		Row(Registration team) {
			this.team = team;
		}

		int goalDifference() {
			return this.goalsFor - this.goalsAgainst;
		}

	}

	static List<Row> table(List<Registration> teams, List<TournamentMatch> matches, int pointsForWin,
			int pointsForDraw) {
		Map<Registration, Row> rows = new LinkedHashMap<>();
		teams.forEach(team -> rows.put(team, new Row(team)));
		for (TournamentMatch match : matches) {
			if (!match.getStatus().isDecided() || match.getAway() == null) {
				continue;
			}
			Row home = rows.get(match.getHome());
			Row away = rows.get(match.getAway());
			if (home == null || away == null) {
				continue;
			}
			record(home, match.getHomeScore(), match.getAwayScore(), pointsForWin, pointsForDraw);
			record(away, match.getAwayScore(), match.getHomeScore(), pointsForWin, pointsForDraw);
		}
		List<Row> sorted = new ArrayList<>(rows.values());
		sorted.sort(Comparator.comparingInt((Row row) -> row.points)
			.thenComparingInt(Row::goalDifference)
			.thenComparingInt(row -> row.goalsFor)
			.reversed()
			.thenComparing(row -> row.team.getTeam().getName()));
		return sorted;
	}

	private static void record(Row row, int scored, int conceded, int pointsForWin, int pointsForDraw) {
		row.played++;
		row.goalsFor += scored;
		row.goalsAgainst += conceded;
		if (scored > conceded) {
			row.won++;
			row.points += pointsForWin;
		}
		else if (scored == conceded) {
			row.drawn++;
			row.points += pointsForDraw;
		}
		else {
			row.lost++;
		}
	}

}
