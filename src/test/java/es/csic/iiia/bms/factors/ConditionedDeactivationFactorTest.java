/*
 * Software License Agreement (BSD License)
 *
 * Copyright 2013-2014 Marc Pujol <mpujol@iiia.csic.es>
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 *   Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 *   Neither the name of IIIA-CSIC, Artificial Intelligence Research Institute
 *   nor the names of its contributors may be used to
 *   endorse or promote products derived from this
 *   software without specific prior written permission of
 *   IIIA-CSIC, Artificial Intelligence Research Institute
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package es.csic.iiia.bms.factors;

import es.csic.iiia.bms.CommunicationAdapter;
import es.csic.iiia.bms.Factor;
import es.csic.iiia.bms.MaxOperator;

import static es.csic.iiia.bms.factors.Constants.DELTA;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Toni Penya-Alba <tonipenya@iiia.csic.es>
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class ConditionedDeactivationFactorTest extends CrossFactorTestAbstract {
    private void init(Factor f, MaxOperator op, CommunicationAdapter com) {
        f.setIdentity(f);
        f.setMaxOperator(op);
        f.setCommunicationAdapter(com);
    }

    private void run(MaxOperator op, double[] values, double[] expected, int exemplarIdx) {
        CommunicationAdapter com = mock(CommunicationAdapter.class);

        // Setup incoming messages
        Factor[] sfs = new Factor[values.length];

        ConditionedDeactivationFactor f = new ConditionedDeactivationFactor();
        init(f, op, com);

        for (int i = 0; i < sfs.length; i++) {
            sfs[i] = mock(Factor.class);
            sfs[i].setMaxOperator(op);
            sfs[i].setIdentity(sfs[i]);
            sfs[i].setCommunicationAdapter(com);

            f.addNeighbor(sfs[i]);
            f.receive(values[i], sfs[i]);
        }

        f.setExemplar(f.getNeighbors().get(exemplarIdx));

        // This makes the factor run and send messages through the mocked com
        f.run();

        // Check expectations
        for (int i = 0; i < sfs.length; i++) {
            verify(com).send(eq(expected[i], DELTA), same(f.getIdentity()), same(sfs[i]));
        }
    }

    @Override
    public Factor[] buildFactors(MaxOperator op, Factor[] neighbors) {
        int exemplarIdx = getRandomIntValue(neighbors.length);

        return new Factor[]{
                buildSpecificFactor(op, neighbors, exemplarIdx),
                buildStandardFactor(op, neighbors, exemplarIdx),
        };
    }

    private Factor buildSpecificFactor(MaxOperator op, Factor[] neighbors, int exemplarIdx) {
        ConditionedDeactivationFactor factor = new ConditionedDeactivationFactor();
        factor.setMaxOperator(op);
        factor.setExemplar(neighbors[exemplarIdx]);
        link(factor, neighbors);

        return factor;
    }

    private Factor buildStandardFactor(MaxOperator op, Factor[] neighbors, int exemplarIdx) {
        StandardFactor factor = new StandardFactor();
        factor.setMaxOperator(op);
        link(factor, neighbors);
        factor.setPotential(buildPotential(op, neighbors, exemplarIdx));

        return factor;
    }

    private double[] buildPotential(MaxOperator op, Factor[] neighbors, int exemplarIdx) {
        final int nNeighbors = neighbors.length;

        // Initialize the cost/utilities array with "no goods"
        double[] values = new double[1 << nNeighbors];
        values[0] = 0;

        // Now set the value for the rest of the rows
        for (int i = 1; i < values.length; i++) {
            if ((i & ((1 << (nNeighbors - exemplarIdx - 1)))) > 0) {
                values[i] = 0;
            } else {
                values[i] = op.getWorstValue();
            }
        }

        return values;
    }
}
