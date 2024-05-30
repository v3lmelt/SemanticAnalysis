package org.example;

import java.util.HashMap;
import java.util.Map;

public class Symbol {
    private Map<String, String> _attrMap;
    private String _identifier;
    public Symbol(){
        _attrMap = new HashMap<>();
    }

    public void addMapItem(String k, String v){
        _attrMap.put(k,v);
    }

    public void removeMapItem(String k){
        _attrMap.remove(k);
    }

    public void setIdentifier(String id){
        _identifier = id;
    }

    public String getIdentifier(String id){
        return _identifier;
    }


}
