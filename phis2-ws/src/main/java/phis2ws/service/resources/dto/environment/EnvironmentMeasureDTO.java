//******************************************************************************
//                                       EnvironmentMeasureDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 nov. 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.environment;

import phis2ws.service.view.model.phis.EnvironmentMeasure;

/**
 * Represents the exchange format used to get environment measures.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class EnvironmentMeasureDTO {

    //The uri of the sensor which has provide the measured value.
    //e.g. http://www.phenome-fppn.fr/mtp/2018/s18003
    protected String sensorUri;
    //The date corresponding to the given value. The format should be yyyy-MM-ddTHH:mm:ssZ
    //e.g. 2018-06-25T15:13:59+0200
    protected String date;
    //The measured value.
    //e.g. 1.2
    protected float value;
    
    public EnvironmentMeasureDTO(EnvironmentMeasure measure) {
        setDate(measure.getDate());
        setSensorUri(measure.getSensorUri());
        setValue(measure.getValue());
    }

    public String getSensorUri() {
        return sensorUri;
    }

    public void setSensorUri(String sensorUri) {
        this.sensorUri = sensorUri;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
  
}
