//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql;

import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 *
 * @author vincent
 */
public abstract class MongoDBServiceTest extends AbstractUnitTest {

    @AfterClass
    public static void destroy() {
        // Implement connection destruction
    }

    @Test
    public void fakeTest() {
        // TODO implement real tests
        assertTrue("Fake test", true);
    }

}
