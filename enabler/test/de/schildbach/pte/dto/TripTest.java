/*
 * Copyright 2017 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.dto;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Patrick Kanzler
 */
public class TripTest {
    private Trip getDummyTripForChanges(Integer changes, Boolean numChangesNull, Integer mode) {
        Trip dummy;
        Location from = new Location(LocationType.ANY, null);
        Location to = new Location(LocationType.ANY, null);
        List<Trip.Leg> legs = getDummyLegsForChanges(mode, changes);
        if (numChangesNull) {
            dummy = new Trip(null, from, to, legs, null, null, null);
        } else {
            dummy = new Trip(null, from, to, legs, null, null, changes);
        }
        return dummy;
    }

    private List<Trip.Leg> getDummyLegsForChanges(Integer mode, Integer changes) {
        Location from = new Location(LocationType.ANY, null);
        Location to = new Location(LocationType.ANY, null);
        List<Trip.Leg> legs = new LinkedList<>();
        Stop departureStop = new Stop(from, null, null, new Date(42), null);
        Stop arrivalStop = new Stop(to, new Date(43), null, null, null);
        Line dummyLine = new Line(null, null, null, null);

        switch (mode) {
        case 0:
            // only Public
            for (int i = 0; i < changes + 1; i++) {
                legs.add(new Trip.Public(dummyLine, null, departureStop, arrivalStop, null, null, null));
            }
            break;
        case 1:
            // only Individual
            for (int i = 0; i < changes + 1; i++) {
                legs.add(
                        new Trip.Individual(Trip.Individual.Type.BIKE, from, new Date(42), to, new Date(43), null, 42));
            }
            break;
        case 2:
            // mixed
            for (int i = 0; i < changes + 1; i++) {
                if ((i % 2) == 0) {
                    legs.add(new Trip.Individual(Trip.Individual.Type.BIKE, from, new Date(42), to, new Date(43), null,
                            42));
                } else {
                    legs.add(new Trip.Public(dummyLine, null, departureStop, arrivalStop, null, null, null));
                }
            }
            break;
        default:
            break;
        }
        return legs;
    }

    @Test
    public void getNumChangesNullPublic() {
        Integer numChangesExpected = 0;
        Trip dummy = getDummyTripForChanges(numChangesExpected, true, 0);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 1;
        dummy = getDummyTripForChanges(numChangesExpected, true, 0);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 2;
        dummy = getDummyTripForChanges(numChangesExpected, true, 0);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());
    }

    @Test
    public void getNumChangesNotNullPublic() {
        Integer numChangesExpected = 0;
        Trip dummy = getDummyTripForChanges(numChangesExpected, false, 0);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 1;
        dummy = getDummyTripForChanges(numChangesExpected, false, 0);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 2;
        dummy = getDummyTripForChanges(numChangesExpected, false, 0);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());
    }

    @Test
    public void getNumChangesNullIndividual() {
        Integer numChangesExpected = 0;
        Trip dummy = getDummyTripForChanges(numChangesExpected, true, 1);
        Assert.assertNull(dummy.getNumChanges());

        numChangesExpected = 1;
        dummy = getDummyTripForChanges(numChangesExpected, true, 1);
        Assert.assertNull(dummy.getNumChanges());

        numChangesExpected = 2;
        dummy = getDummyTripForChanges(numChangesExpected, true, 1);
        Assert.assertNull(dummy.getNumChanges());
    }

    @Test
    public void getNumChangesNotNullIndividual() {
        Integer numChangesExpected = 0;
        Trip dummy = getDummyTripForChanges(numChangesExpected, false, 1);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 1;
        dummy = getDummyTripForChanges(numChangesExpected, false, 1);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 2;
        dummy = getDummyTripForChanges(numChangesExpected, false, 1);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());
    }

    @Test
    public void getNumChangesNullMixed() {
        Integer numChangesExpected = 0;
        Trip dummy = getDummyTripForChanges(numChangesExpected, true, 2);
        Assert.assertNull(dummy.getNumChanges());

        numChangesExpected = 1;
        dummy = getDummyTripForChanges(numChangesExpected, true, 2);
        numChangesExpected = numChangesExpected - 1;
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 2;
        dummy = getDummyTripForChanges(numChangesExpected, true, 2);
        numChangesExpected = numChangesExpected - 2;
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 3;
        dummy = getDummyTripForChanges(numChangesExpected, true, 2);
        numChangesExpected = numChangesExpected - 2;
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());
    }

    @Test
    public void getNumChangesNotNullMixed() {
        Integer numChangesExpected = 0;
        Trip dummy = getDummyTripForChanges(numChangesExpected, false, 2);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 1;
        dummy = getDummyTripForChanges(numChangesExpected, false, 2);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());

        numChangesExpected = 2;
        dummy = getDummyTripForChanges(numChangesExpected, false, 2);
        Assert.assertEquals(numChangesExpected, dummy.getNumChanges());
    }
}
