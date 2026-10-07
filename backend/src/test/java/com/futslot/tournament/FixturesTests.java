package com.futslot.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class FixturesTests {

	@Test
	void everyoneMeetsEveryoneOnceInARoundRobin() {
		for (int teams = 2; teams <= 9; teams++) {
			List<Integer> field = IntStream.rangeClosed(1, teams).boxed().toList();
			List<List<Fixtures.Pairing<Integer>>> rounds = Fixtures.roundRobin(field);
			Set<Set<Integer>> pairs = new HashSet<>();
			for (List<Fixtures.Pairing<Integer>> round : rounds) {
				Set<Integer> playing = new HashSet<>();
				for (Fixtures.Pairing<Integer> pairing : round) {
					assertThat(playing.add(pairing.home())).isTrue();
					assertThat(playing.add(pairing.away())).isTrue();
					assertThat(pairs.add(Set.of(pairing.home(), pairing.away()))).isTrue();
				}
			}
			assertThat(pairs).hasSize(teams * (teams - 1) / 2);
			assertThat(rounds).hasSize((teams % 2 == 0) ? teams - 1 : teams);
		}
	}

	@Test
	void topSeedsAreKeptApartAndGetTheByes() {
		assertThat(Fixtures.seedOrder(8)).containsExactly(1, 8, 4, 5, 2, 7, 3, 6);
		assertThat(Fixtures.seededSlots(List.of("S1", "S2", "S3", "S4", "S5", "S6")))
			.containsExactly("S1", null, "S4", "S5", "S2", null, "S3", "S6");
		assertThat(Fixtures.bracketSize(5)).isEqualTo(8);
		assertThat(Fixtures.bracketSize(2)).isEqualTo(2);
	}

	@Test
	void groupsAreDrawnSnakeWiseAndCrossedIntoTheKnockout() {
		assertThat(Fixtures.snakeGroups(List.of(1, 2, 3, 4, 5, 6, 7, 8), 2))
			.containsExactly(List.of(1, 4, 5, 8), List.of(2, 3, 6, 7));
		assertThat(Fixtures.crossGroups(List.of(List.of("A1", "A2"), List.of("B1", "B2"), List.of("C1", "C2"),
				List.of("D1", "D2"))))
			.containsExactly("A1", "B2", "C1", "D2", "B1", "A2", "D1", "C2");
		assertThat(Fixtures.stageFor(8)).isEqualTo(Stage.QUARTER_FINAL);
	}

}
