package eu.trentorise.game.challenges;

import eu.trentorise.game.challenges.api.ChallengeFactoryInterface;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.Challenge;
import eu.trentorise.game.challenges.model.ChallengeType;

public class ChallengeFactory implements ChallengeFactoryInterface {

    @Override
    public Challenge createChallenge(ChallengeType chType, String templateDir)
            throws UndefinedChallengeException {
        switch (chType) {
            case PERCENT:
                return new PercentMobilityChallenge(templateDir);
            case BSPERCENT:
                return new BikeSharePercentMobilityChallenge(templateDir);
            case TRIPNUMBER:
                return new TripNumberChallenge(templateDir);
            case NEGATEDMODE:
                return new NegatedModeChallenge(templateDir);
            case BSTRIPNUMBER:
                return new BikeShareTripNumberChallenge(templateDir);
            case BADGECOLLECTION:
                return new BadgeCollectionCompletionChallenge(templateDir);
            case RECOMMENDATION:
                return new RecommendationChallenge(templateDir);
            case POINTSEARNED:
                return new PointsEarnedChallenge(templateDir);
            case ZEROIMPACT:
                return new ZeroImpactChallenge(templateDir);
            case NEXTBADGE:
                return new NextBadgeChallenge(templateDir);
            case SURVEYDATA:
                return new SurveyCompletionChallenge(templateDir);
            // case LEADERBOARDPOSITION:
            // return new ClassificationImprovementChallenge(templateDir);
            default:
                throw new UndefinedChallengeException("Unknown challenge type!"
                        + chType.toString());
        }
    }

}