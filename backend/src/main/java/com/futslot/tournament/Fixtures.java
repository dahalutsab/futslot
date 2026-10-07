package com.futslot.tournament;

import java.util.ArrayList;
import java.util.List;

/** Who plays whom. Pure functions over any team type, so the draw can be tested on its own. */
final class Fixtures {

	private Fixtures() {
	}

	record Pairing<T>(T home, T away) {

	}

	/**
	 * Everyone plays everyone once (circle method). With an odd number of teams one team rests each round.
	 * Home and away alternate so nobody is always home.
	 */
	static <T> List<List<Pairing<T>>> roundRobin(List<T> teams) {
		List<T> circle = new ArrayList<>(teams);
		if (circle.size() % 2 == 1) {
			circle.add(null);
		}
		int n = circle.size();
		List<List<Pairing<T>>> rounds = new ArrayList<>();
		for (int round = 0; round < n - 1; round++) {
			List<Pairing<T>> pairings = new ArrayList<>();
			for (int i = 0; i < n / 2; i++) {
				T first = circle.get(i);
				T second = circle.get(n - 1 - i);
				if (first != null && second != null) {
					boolean swap = (i == 0) ? round % 2 == 1 : i % 2 == 1;
					pairings.add(swap ? new Pairing<>(second, first) : new Pairing<>(first, second));
				}
			}
			rounds.add(pairings);
			// Keep the first team fixed and rotate the rest one place
			circle.add(1, circle.removeLast());
		}
		return rounds;
	}

	/** Smallest power of two that fits {@code teams}. */
	static int bracketSize(int teams) {
		int size = 2;
		while (size < teams) {
			size *= 2;
		}
		return size;
	}

	/**
	 * Standard seeding positions for a bracket of {@code size}: seed 1 and 2 can only meet in the final,
	 * 1-4 not before the semis, and so on. For 8: 1, 8, 4, 5, 2, 7, 3, 6.
	 */
	static List<Integer> seedOrder(int size) {
		List<Integer> order = new ArrayList<>(List.of(1, 2));
		while (order.size() < size) {
			int next = order.size() * 2 + 1;
			List<Integer> expanded = new ArrayList<>();
			for (int seed : order) {
				expanded.add(seed);
				expanded.add(next - seed);
			}
			order = expanded;
		}
		return order;
	}

	/**
	 * Bracket slots, top to bottom, for teams already in seed order. Missing seeds are byes (null), and
	 * they always face the top seeds.
	 */
	static <T> List<T> seededSlots(List<T> seeded) {
		int size = bracketSize(seeded.size());
		List<T> slots = new ArrayList<>();
		for (int seed : seedOrder(size)) {
			slots.add((seed <= seeded.size()) ? seeded.get(seed - 1) : null);
		}
		return slots;
	}

	/** Snake draw into {@code groups} groups: 1-2-3-4, 8-7-6-5, 9-10-11-12... keeps groups balanced. */
	static <T> List<List<T>> snakeGroups(List<T> seeded, int groups) {
		List<List<T>> result = new ArrayList<>();
		for (int g = 0; g < groups; g++) {
			result.add(new ArrayList<>());
		}
		for (int i = 0; i < seeded.size(); i++) {
			int row = i / groups;
			int column = i % groups;
			result.get((row % 2 == 0) ? column : groups - 1 - column).add(seeded.get(i));
		}
		return result;
	}

	/**
	 * Knockout slots from group standings (each list: winner, runner-up). Group winners meet runners-up of
	 * the neighbouring group, and a group's top two land in opposite halves: A1-B2, C1-D2, ... then B1-A2,
	 * D1-C2, ...
	 */
	static <T> List<T> crossGroups(List<List<T>> topTwo) {
		List<T> top = new ArrayList<>();
		List<T> bottom = new ArrayList<>();
		for (int g = 0; g < topTwo.size(); g += 2) {
			List<T> first = topTwo.get(g);
			List<T> second = topTwo.get(g + 1);
			top.add(first.get(0));
			top.add(second.get(1));
			bottom.add(second.get(0));
			bottom.add(first.get(1));
		}
		top.addAll(bottom);
		return top;
	}

	static String groupName(int index) {
		return String.valueOf((char) ('A' + index));
	}

	/** Knockout stage by the number of teams still in it. */
	static Stage stageFor(int teamsInRound) {
		return switch (teamsInRound) {
			case 2 -> Stage.FINAL;
			case 4 -> Stage.SEMI_FINAL;
			case 8 -> Stage.QUARTER_FINAL;
			case 16 -> Stage.ROUND_OF_16;
			case 32 -> Stage.ROUND_OF_32;
			default -> throw new IllegalArgumentException("Unsupported knockout round of " + teamsInRound);
		};
	}

}
