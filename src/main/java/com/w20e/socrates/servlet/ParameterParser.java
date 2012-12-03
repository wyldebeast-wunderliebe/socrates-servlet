package com.w20e.socrates.servlet;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

public final class ParameterParser {

    /**
     * Initialize this class' logging.
     */
    private static final Logger LOGGER = Logger
            .getLogger(ParameterParser.class.getName());

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseParams(final HttpServletRequest req) {
        
        String key;
        String[] values;
        final Map<String, Object> newparams = new HashMap<String, Object>();
        
        for (Enumeration<String> params = req.getParameterNames(); params.hasMoreElements(); ) {
            key = params.nextElement();
            values = req.getParameterValues(key);
                        
            if (values.length == 1) {
                LOGGER.fine("Parameter " + key + " holds value " + values[0]);
                newparams.put(key, values[0]);
            } else {
                newparams.put(key, Arrays.asList(values));
                LOGGER.fine("Parameter " + key + " holds values " + values);
            }
        }
        
        return newparams;
    }
}
