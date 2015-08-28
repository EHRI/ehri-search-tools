package eu.ehri.project.indexing.test;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class StringToInteger implements Converter<String, Integer> {
    @Override
    public Iterable<Integer> convert(String t) throws ConverterException {
        return Lists.newArrayList(Integer.valueOf(t));
    }
}
