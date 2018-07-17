//**********************************************************************************************
//                                       ResponseFormCalls.java 
//
// Author(s): Alice Boizet
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: July 2018
// Contact: alice.boizet@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  July 2018
// Subject: Allows the formating of the results of the queries about calls
//***********************************************************************************************

package phis2ws.service.view.brapi.form;
import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultCalls;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Calls;
import phis2ws.service.view.brapi.Status;


public class ResponseFormCalls extends ResultForm<Calls>{
    
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param list liste des résultats
     * @param paginate 
     * @param statuslist
     */
    //private List<Status> statuslist  = new ArrayList<>();
    
    public ResponseFormCalls(int pageSize, int currentPage, ArrayList<Calls> list, boolean paginate, ArrayList<Status> statuslist) {
        metadata = new Metadata(pageSize, currentPage, list.size(), statuslist);
        if (list.size() > 1) {
            result = new ResultCalls(list, metadata.getPagination(), paginate); 
        } else {
            result = new ResultCalls(list);
        }
    }
}
