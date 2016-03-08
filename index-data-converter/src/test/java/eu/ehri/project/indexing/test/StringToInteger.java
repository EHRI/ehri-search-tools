package eu.ehri.project.indexing.test;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;


public class StringToInteger implements Converter<String, Integer> {
    @Override
    public Iterable<Integer> convert(String t) throws ConverterException {
        try {
            return Lists.newArrayList(Integer.valueOf(t));
        } catch (NumberFormatException e) {
            throw new ConverterException("Bad conversion", e);
        }
    }
}
