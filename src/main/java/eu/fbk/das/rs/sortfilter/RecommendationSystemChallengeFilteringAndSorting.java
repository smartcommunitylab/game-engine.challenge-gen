package eu.fbk.das.rs.sortfilter;

import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemStatistics;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Player;
import eu.trentorise.game.challenges.rest.PointConcept;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.rs.utils.ArrayUtils.pos;
import static eu.fbk.das.rs.utils.Utils.dbg;

public class RecommendationSystemChallengeFilteringAndSorting {

    private static final Logger logger = LogManager
            .getLogger(RecommendationSystemChallengeFilteringAndSorting.class);

    private double[] leaderboard;

    private DateTime execDate;

    public List<ChallengeDataDTO> filter(List<ChallengeDataDTO> challenges, Player player, DateTime date) {
        this.execDate = date;

        List<ChallengeDataDTO> result = new ArrayList<ChallengeDataDTO>();

        /* REMOVED LEADERBOARD
        List<ChallengeDataDTO> improvingLeaderboard = new ArrayList<ChallengeDataDTO>();
        List<ChallengeDataDTO> notImprovingLeaderboard = new ArrayList<ChallengeDataDTO>();
        */

        for (ChallengeDataDTO challenge : challenges) {
            Double baseline = (Double) challenge.getData().get("baseline");
            Double target = 0.0;
            if (challenge.getData().get("target") instanceof Integer) {
                target = new Double((Integer) challenge.getData().get("target"));
            } else {
                target = (Double) challenge.getData().get("target");
            }
            Integer weight = ChallengesConfig.getWeight((String) challenge.getData().get("counterName"));
            Double percentageImprovment = 0.0;
            if (baseline != null) {
                if (challenge.getModelName().equals("percentageIncrement")) {
                    Double p = (Double) challenge.getData().get("percentage");
                    percentageImprovment = p;
                } else {
                    percentageImprovment = Math.round(Math.abs(baseline - target) * 100.0 / baseline) / 100.0;
                }
            } else {
                percentageImprovment = 1.0;
            }
            int prize = (int) challenge.getData().get("bonusScore");
            // calculating the WI for each mode based on weight of Mode and
            // improvement percentage
            double wi = percentageImprovment * weight;
            challenge.getData().put("wi", wi);
            // finding the position of the player in the leader board


            /* REMOVED LEADERBOARD

            int position = findPosition(leaderboard, player);


            if (position == 0) {
                // all the challenges;
                improvingLeaderboard.add(challenge);
            } else {

                double currentScore = leaderboard[position];
                double nextScore = currentScore;
                int nextPosition = position;
                while (nextPosition < leaderboard.length && nextScore <= currentScore) {
                    nextPosition++;
                    nextScore = leaderboard[nextPosition];
                }

                if (prize + currentScore > nextScore) {
                    // I like this challenge, because improve my position
                    // into the leader board
                    improvingLeaderboard.add(challenge);
                } else {
                    notImprovingLeaderboard.add(challenge);

                }
            }
            */

            result.add(challenge);

        }

        /* REMOVED LEADERBOARD

        // sorting both lists
        Collections.sort(improvingLeaderboard, new DifficultyWiComparator());
        Collections.sort(notImprovingLeaderboard, new DifficultyPrizeComparator());

        // add for first the improving leaderboard challenges
        if (!improvingLeaderboard.isEmpty()) {
            result.addAll(improvingLeaderboard);
        }
        if (!notImprovingLeaderboard.isEmpty()) {
            result.addAll(notImprovingLeaderboard);
        } */

        Collections.sort(result, new DifficultyPrizeComparator());

        return result;
    }

    private int findPosition(double[] leaderboard, Player player) {
        double score = 0;
        for (PointConcept pc : player.getState().getPointConcept()) {
            if (!pc.getName().equals(ChallengesConfig.gLeaves))
                continue;

            score = pc.getPeriodScore("weekly", execDate.getMillis() / 1000);
        }

        int pos = pos(score, leaderboard);
        if (pos < 0) {
            pos = -(pos) - 1;
        }

        return pos;
    }

    public Map<String, List<ChallengeDataDTO>> filterAndSort(
            Map<String, List<ChallengeDataDTO>> evaluatedChallenges,
            List<LeaderboardPosition> leaderboard) {
        Map<String, List<ChallengeDataDTO>> result = new HashMap<String, List<ChallengeDataDTO>>();
        Double wi = 0.0;
        for (String playerId : evaluatedChallenges.keySet()) {
            // creating two list for the challenges that can improve the player
            // in the leader board and not improving
            List<ChallengeDataDTO> improvingLeaderboard = new ArrayList<ChallengeDataDTO>();
            List<ChallengeDataDTO> notImprovingLeaderboard = new ArrayList<ChallengeDataDTO>();
            for (ChallengeDataDTO challenge : evaluatedChallenges.get(playerId)) {
                Double baseline = (Double) challenge.getData().get("baseline");
                Double target = 0.0;
                if (challenge.getData().get("target") instanceof Integer) {
                    target = new Double((Integer) challenge.getData().get(
                            "target"));
                } else {
                    target = (Double) challenge.getData().get("target");
                }
                Integer weight = ChallengesConfig.getWeight((String) challenge
                        .getData().get("counterName"));
                Double percentageImprovment = 0.0;
                if (baseline != null) {
                    if (challenge.getModelName().equals("percentageIncrement")) {
                        Double p = (Double) challenge.getData().get(
                                "percentage");
                        percentageImprovment = p;
                    } else {
                        percentageImprovment = Math.round(Math.abs(baseline
                                - target)
                                * 100.0 / baseline) / 100.0;
                    }
                } else {
                    percentageImprovment = 1.0;
                }
                Double prize = (Double) challenge.getData().get("bonusScore");
                // calculating the WI for each mode based on weight of Mode and
                // improvement percentage
                wi = percentageImprovment * weight;
                challenge.getData().put("wi", wi);
                // finding the position of the player in the leader board
                LeaderboardPosition position = findPosition(leaderboard,
                        playerId);
                if (position.getIndex() == 0) {
                    // all the challenges;
                    improvingLeaderboard.add(challenge);
                } else {
                    LeaderboardPosition pos = findScoreMoreThanMe(leaderboard,
                            position.getIndex(), position.getScore());
                    if (prize + position.getScore() > pos.getScore()) {
                        // I like this challenge, because improve my position
                        // into the leader board
                        improvingLeaderboard.add(challenge);
                    } else {
                        notImprovingLeaderboard.add(challenge);

                    }
                }
            }
            // make some initialization for result data structure
            if (result.get(playerId) == null) {
                result.put(playerId, new ArrayList<ChallengeDataDTO>());
            }
            // sorting both lists
            Collections
                    .sort(improvingLeaderboard, new DifficultyWiComparator());
            Collections.sort(notImprovingLeaderboard,
                    new DifficultyPrizeComparator());

            // add for first the improving leaderboard challenges
            if (!improvingLeaderboard.isEmpty()) {
                result.get(playerId).addAll(improvingLeaderboard);
            }
            if (!notImprovingLeaderboard.isEmpty()) {
                result.get(playerId).addAll(notImprovingLeaderboard);

            }
        }
        dbg(logger, "Filtering and sorting complete for "
                + result.keySet().size() + " players");
        return result;
    }

    private LeaderboardPosition findScoreMoreThanMe(
            List<LeaderboardPosition> leaderboard, Integer index, Integer score) {
        for (int i = index; i >= 0; i--) {
            if (leaderboard.get(i).getScore() > score) {
                return leaderboard.get(i);
            }
        }
        // in a game with no score or where all have the same value, we'll get a
        // null pointer exception
        return leaderboard.get(index);
    }

    private LeaderboardPosition findPosition(
            List<LeaderboardPosition> leaderboard, String playerId) {
        Integer currentIndex = 0;
        for (LeaderboardPosition pos : leaderboard) {
            if (playerId == pos.getPlayerId()) {
                pos.setIndex(currentIndex);
                return pos;
            } else {
                currentIndex++;
            }
        }
        return null;
    }

    public Map<String, List<ChallengeDataDTO>> removeDuplicates(
            Map<String, List<ChallengeDataDTO>> filteredChallenges) {
        List<ChallengeDataDTO> challengeIdToRemove = new ArrayList<ChallengeDataDTO>();
        for (String key : filteredChallenges.keySet()) {

            Iterator<ChallengeDataDTO> iter = filteredChallenges.get(key)
                    .iterator();
            while (iter.hasNext()) {
                ChallengeDataDTO dto = iter.next();
                Iterator<ChallengeDataDTO> innerIter = filteredChallenges.get(
                        key).iterator();
                int count = 0;
                while (innerIter.hasNext()) {
                    ChallengeDataDTO idto = innerIter.next();

                    if (dto.getModelName().equals(idto.getModelName())
                            && dto.getData().get("counterName")
                            .equals(idto.getData().get("counterName"))) {
                        double t = 0;
                        double ti = 0;
                        if (dto.getData().get("target") instanceof Double) {
                            t = (Double) dto.getData().get("target");
                        } else {
                            t = (Integer) dto.getData().get("target");
                        }
                        if (idto.getData().get("target") instanceof Double) {
                            ti = (Double) idto.getData().get("target");
                        } else {
                            ti = (Integer) idto.getData().get("target");
                        }
                        if (t == ti) {
                            count++;
                        }
                    }
                    if (count > 1) {
                        challengeIdToRemove.add(idto);
                        count = 1;
                    }
                }

            }
            filteredChallenges.get(key).removeAll(challengeIdToRemove);
            challengeIdToRemove.clear();
        }
        return filteredChallenges;
    }

}
