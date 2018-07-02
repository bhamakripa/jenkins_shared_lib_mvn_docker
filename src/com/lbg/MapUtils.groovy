package com.lbg

class MapUtils implements Serializable {

    static boolean isMap(object){
        return object in Map
    }

    static Map pruneNulls(Map m) {

        Map result = [:]

        m = m ?: [:]

        for(def e : m.entrySet())
            if(isMap(e.value))
                result[e.key] = pruneNulls(e.value)
            else if(e.value != null)
                result[e.key] = e.value
        return result
    }


    static Map merge(Map base, Map overlay) {

        Map result = [:]

        base = base ?: [:]

        result.putAll(base)

        for(def e : overlay.entrySet())
            result[e.key] = isMap(e.value) ? merge(base[e.key], e.value) : e.value

        return result
    }
}
