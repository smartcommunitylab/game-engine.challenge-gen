package eu.fbk.das.rs.sortfilter;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.utils.ArrayUtils.pos;
import static eu.fbk.das.utils.Utils.dbg;

public class RecommendationSystemChallengeFilteringAndSorting {

    private static final Logger logger = Logger
            .getLogger(RecommendationSystemChallengeFilteringAndSorting.class);

    private double[] leaderboard;

    private DateTime execDate;

    public List<ChallengeExpandedDTO> filter(List<ChallengeExpandedDTO> challenges, PlayerStateDTO player, DateTime date) {
        this.execDate = date;

        List<ChallengeExpandedDTO> result = new ArrayList<ChallengeExpandedDTO>();

        /* REMOVED LEADERBOARD
        List<ChallengeExpandedDTO> improvingLeaderboard = new ArrayList<ChallengeExpandedDTO>();
        List<ChallengeExpandedDTO> notImprovingLeaderboard = new ArrayList<ChallengeExpandedDTO>();
        */

        for (ChallengeExpandedDTO challenge : challenges) {
            Double baseline = (Double) challenge.getData("baseline");
            Double target = 0.0;
            if (challenge.getData("target") instanceof Integer) {
                target = new Double((Integer) challenge.getData("target"));
            } else {
                target = (Double) challenge.getData("target");
            }
            Integer weight = ChallengesConfig.getWeight((String) challenge.getData("counterName"));
            Double percentageImprovment = 0.0;
            if (baseline != null) {
                if (challenge.getModelName().equals("percentageIncrement")) {
                    Double p = (Double) challenge.getData("percentage");
                    percentageImprovment = p;
                } else {
                    percentageImprovment = Math.round(Math.abs(baseline - target) * 100.0 / baseline) / 100.0;
                }
            } else {
                percentageImprovment = 1.0;
            }
            double prize = (double) challenge.getData("bonusScore");
            // calculating the WI for each mode based on weight of Mode and
            // improvement percentage
            double wi = percentageImprovment * weight;
            challenge.setData("wi", wi);
            // finding the position of the PlayerStateDTO in the leader board


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

    private int findPosition(double[] leaderboard, PlayerStateDTO player) {
        double score = 0;
        Set<GameConcept> scores =  player.getState().get("PointConcept");
        for (GameConcept gc : scores) {
            PointConcept pc = (PointConcept) gc;
            if (!gc.getName().equals(ChallengesConfig.gLeaves))
                continue;

            score = getPeriodScore(pc, "weekly", execDate);
        }

        int pos = pos(score, leaderboard);
        if (pos < 0) {
            pos = -(pos) - 1;
        }

        return pos;
    }

    public Map<String, List<ChallengeExpandedDTO>> filterAndSort(
            Map<String, List<ChallengeExpandedDTO>> evaluatedChallenges,
            List<LeaderboardPosition> leaderboard) {
        Map<String, List<ChallengeExpandedDTO>> result = new HashMap<String, List<ChallengeExpandedDTO>>();
        Double wi = 0.0;
        for (String playerId : evaluatedChallenges.keySet()) {
            // creating two list for the challenges that can improve the player
            // in the leader board and not improving
            List<ChallengeExpandedDTO> improvingLeaderboard = new ArrayList<ChallengeExpandedDTO>();
            List<ChallengeExpandedDTO> notImprovingLeaderboard = new ArrayList<ChallengeExpandedDTO>();
            for (ChallengeExpandedDTO challenge : evaluatedChallenges.get(playerId)) {
                Double baseline = (Double) challenge.getData("baseline");
                Double target = 0.0;
                if (challenge.getData("target") instanceof Integer) {
                    target = new Double((Integer) challenge.getData(
                            "target"));
                } else {
                    target = (Double) challenge.getData("target");
                }
                Integer weight = ChallengesConfig.getWeight((String) challenge
                        .getData("counterName"));
                Double percentageImprovment = 0.0;
                if (baseline != null) {
                    if (challenge.getModelName().equals("percentageIncrement")) {
                        Double p = (Double) challenge.getData(
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
                Double prize = (Double) challenge.getData("bonusScore");
                // calculating the WI for each mode based on weight of Mode and
                // improvement percentage
                wi = percentageImprovment * weight;
                challenge.setData("wi", wi);
                // finding the position of the PlayerStateDTO in the leader board
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
                result.put(playerId, new ArrayList<ChallengeExpandedDTO>());
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

    public Map<String, List<ChallengeExpandedDTO>> removeDuplicates(
            Map<String, List<ChallengeExpandedDTO>> filteredChallenges) {
        List<ChallengeExpandedDTO> challengeIdToRemove = new ArrayList<ChallengeExpandedDTO>();
        for (String key : filteredChallenges.keySet()) {

            Iterator<ChallengeExpandedDTO> iter = filteredChallenges.get(key)
                    .iterator();
            while (iter.hasNext()) {
                ChallengeExpandedDTO dto = iter.next();
                Iterator<ChallengeExpandedDTO> innerIter = filteredChallenges.get(
                        key).iterator();
                int count = 0;
                while (innerIter.hasNext()) {
                    ChallengeExpandedDTO idto = innerIter.next();

                    if (dto.getModelName().equals(idto.getModelName())
                            && dto.getData("counterName")
                            .equals(idto.getData("counterName"))) {
                        double t = 0;
                        double ti = 0;
                        if (dto.getData("target") instanceof Double) {
                            t = (Double) dto.getData("target");
                        } else {
                            t = (Integer) dto.getData("target");
                        }
                        if (idto.getData("target") instanceof Double) {
                            ti = (Double) idto.getData("target");
                        } else {
                            ti = (Integer) idto.getData("target");
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
