package eu.ehri.project.indexing.converter.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;
import eu.ehri.project.indexing.test.StringToInteger;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Aggregation of converters produces a single output
 * stream with their results concatenated in input order.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class MultiConverterTest {

    static class StringToIntegerInc implements Converter<String, Number> {
        @Override
        public Iterable<Number> convert(String t) throws ConverterException {
            try {
                Integer out = Integer.valueOf(t);
                return Lists.<Number>newArrayList(out, out + 1);
            } catch (NumberFormatException e) {
                throw new ConverterException("conversion error", e);
            }
        }
    }

    @Test
    public void testConvert() throws Exception {
        List<Converter<String, ? extends Number>> converters = Lists.newArrayList();
        converters.add(new StringToInteger());
        converters.add(new StringToIntegerInc());

        MultiConverter<String, Number> mc = new MultiConverter<>(converters);
        List<Number> numbers = Lists.newArrayList(mc.convert("1"));
        assertEquals(Lists.newArrayList(1, 1, 2), numbers);
    }

    @Test(expected = Converter.ConverterException.class)
    public void testConvertWithError() throws Exception {
        List<Converter<String, ? extends Number>> converters = Lists.newArrayList();
        converters.add(new StringToInteger());
        converters.add(new StringToIntegerInc());

        MultiConverter<String, Number> mc = new MultiConverter<>(converters);
        Lists.newArrayList(mc.convert("NaN"));
    }
}