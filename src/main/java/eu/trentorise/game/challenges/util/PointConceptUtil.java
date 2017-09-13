package eu.trentorise.game.challenges.util;

import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.PointConcept;
import eu.trentorise.game.challenges.rest.PointConcept.PeriodInstanceImpl;
import eu.trentorise.game.challenges.rest.PointConcept.PeriodInternal;

/**
 * Utility class for {@link PointConcept}
 *
 */
public final class PointConceptUtil {

    private PointConceptUtil() {}

    /**
     * Return max value for given {@link PointConcept} type in a period for a specific user
     * 
     * @param user
     * @param pointType
     * @param periodIdentifier
     * @return max value or 0
     */
    public static Double getScoreMax(Content user, String pointType, String periodIdentifier) {
        PointConcept pc = getPointConcept(user, pointType);
        if (pc != null) {
            Double max = 0d;
            PeriodInternal period = pc.getPeriods().get(periodIdentifier);
            if (period != null) {
                for (PeriodInstanceImpl inst : period.getInstances().values()) {
                    if (inst.getScore() > max) {
                        max = inst.getScore();
                    }
                }
                return max;
            }
        }
        return 0d;
    }

    /**
     * Search for point concept of a given type in a user
     * 
     * @param user
     * @param pointType
     * @return {@link PointConcept} or null
     */
    public static PointConcept getPointConcept(Content user, String pointType) {
        if (user != null && user.getState() != null && user.getState().getPointConcept() != null
                && pointType != null) {
            for (PointConcept pc : user.getState().getPointConcept()) {
                if (pc.getName().equalsIgnoreCase(pointType)) {
                    return pc;
                }
            }
            return null;
        }
        return null;
    }

    /**
     * Get current score from a point oncept of a type and period for a user
     * 
     * @param user
     * @param pointType
     * @param periodIdentifier
     * @return
     */
    public static Double getScoreCurrent(Content user, String pointType, String periodIdentifier) {
        PointConcept pc = getPointConcept(user, pointType);
        if (pc != null) {
            return pc.getPeriodCurrentScore(periodIdentifier);
        }
        return 0d;
    }

    /**
     * Return value for a point concept with given name for a user
     * 
     * @param user
     * @param name
     * @return
     */
    public static Double getScoreFromConcept(Content user, String name) {
        for (PointConcept pc : user.getState().getPointConcept()) {
            if (pc.getName().equalsIgnoreCase(name)) {
                return pc.getScore();
            }
        }
        return 0d;
    }

    /**
     * Return previous score for a point concept of given type in a period for a user
     * 
     * @param user
     * @param pointType
     * @param periodIdentifier
     * @return
     */
    public static Double getScorePrevious(Content user, String pointType, String periodIdentifier) {
        PointConcept pc = getPointConcept(user, pointType);
        if (pc != null) {
            return pc.getPeriodPreviousScore(periodIdentifier);
        }
        return 0d;
    }

}
