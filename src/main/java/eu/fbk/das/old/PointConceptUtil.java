package eu.fbk.das.old;

import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.PointConcept;

import java.util.Set;

/**
 * Utility class for {@link PointConcept}
 */
public final class PointConceptUtil {

    private PointConceptUtil() {
    }

    /**
     * Return max value for given {@link PointConcept} type in a period for a specific user
     *
     * @param user
     * @param pointType
     * @param periodIdentifier
     * @return max value or 0
     */
    public static Double getScoreMax(PlayerStateDTO user, String pointType, String periodIdentifier) {
        PointConcept pc = getPointConcept(user, pointType);
        if (pc != null) {
            Double max = 0d;
            PointConcept.PeriodInternal period = pc.getPeriods().get(periodIdentifier);
            if (period != null) {
                /* TODO FIX
                for (PointConcept.PeriodInstanceImpl inst : period.getInstances().values()) {
                    if (inst.getScore() > max) {
                        max = inst.getScore();
                    }
                }*/
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
    public static PointConcept getPointConcept(PlayerStateDTO user, String pointType) {
        if (user == null)
            return null;
        if (user.getState() == null)
            return null;

        Set<GameConcept> pbs = user.getState().get("PointConcept");
        if (pbs == null) return null;

            for (GameConcept pc : pbs) {
                if (pc.getName().equalsIgnoreCase(pointType)) {
                    return (PointConcept) pc;
                }
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
    public static Double getScoreCurrent(PlayerStateDTO user, String pointType, String periodIdentifier) {
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
    public static Double getScoreFromConcept(PlayerStateDTO user, String name) {
        Set<GameConcept> pbs = user.getState().get("PointConcept");
        for (GameConcept gc : pbs) {
            PointConcept pc = (PointConcept) gc;
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
    public static Double getScorePrevious(PlayerStateDTO user, String pointType, String periodIdentifier) {
        PointConcept pc = getPointConcept(user, pointType);
        if (pc != null) {
            return pc.getPeriodPreviousScore(periodIdentifier);
        }
        return 0d;
    }

}
