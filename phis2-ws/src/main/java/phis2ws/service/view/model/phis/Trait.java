//**********************************************************************************************
//                                       Trait.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 17 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 17 2017
// Subject: Represents the instance of definition view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

public class Trait extends RdfResourceDefinition {
        
    //the variables linked to the trait (required in brapi)
    private ArrayList<String> variables;
    
    public Trait() {
      
    }
    
    public Trait(String uri) {
        super(uri);
    }

    public ArrayList<String> getVariables() {
        return variables;
    }

    public void setVariables(ArrayList<String> variables) {
        this.variables = variables;
    }
} 
