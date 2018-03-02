//**********************************************************************************************
//                                       ResultatAsk.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Janvier 30 2018
// Contact: eloan.lagire@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Janvier 30, 2018
// Subject: extend form Resultat adapted to the Ask
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Ask;

/**
 * A class which represents the result part in the response form, adapted to the 
 * exist queries (ask)
 * @author Eloan LAGIER
 */
public class ResultAsk extends Resultat<Ask> {

    /**
     * builder for a one-element list
     *
     * @param ask the exist results
     */
    public ResultAsk(ArrayList<Ask> ask) {
        super(ask);
    }

    /**
     * builder for a more than one element list
     *
     * @param askResults
     * @param pagination
     * @param paginate
     */
    public ResultAsk(ArrayList<Ask> askResults, Pagination pagination, boolean paginate) {
        super(askResults, pagination, paginate);
    }
}
