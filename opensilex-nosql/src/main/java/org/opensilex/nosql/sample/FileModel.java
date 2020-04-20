package org.opensilex.nosql.sample;


import java.io.Serializable;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 *
 * @author charlero
 */
@PersistenceCapable()
public class FileModel implements Serializable {

    @PrimaryKey  
    String name; 

    public FileModel( String name) {
        this.name = name;
    }
 
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
        
}
