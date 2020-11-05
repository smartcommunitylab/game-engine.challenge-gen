package eu.fbk.das.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.joda.time.DateTime;
import org.junit.Test;

public class ChallengeExtendedDTOTest {

    @Test
    public void fromDstToStandardTime() {
        final String startAsString = "2019-10-26T00:00:00+02:00";
        final String duration = "3d";
        ChallengeExpandedDTO challenge = new ChallengeExpandedDTO();
        challenge.setDates(startAsString, duration);
        assertThat(challenge.getStart(), is(new DateTime(2019, 10, 26, 0, 0, 0).toDate()));
        assertThat(challenge.getEnd(), is(new DateTime(2019, 10, 29, 0, 0, 0).toDate()));
    }

    @Test
    public void fromStandardTimeToDst() {
        final String startAsString = "2020-03-27T00:00:00+01:00";
        final String duration = "5d";
        ChallengeExpandedDTO challenge = new ChallengeExpandedDTO();
        challenge.setDates(startAsString, duration);
        assertThat(challenge.getStart(), is(new DateTime(2020, 3, 27, 0, 0, 0).toDate()));
        assertThat(challenge.getEnd(), is(new DateTime(2020, 4, 1, 0, 0, 0).toDate()));
    }
}
