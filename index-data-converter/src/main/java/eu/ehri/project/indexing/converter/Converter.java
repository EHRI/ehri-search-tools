package eu.ehri.project.indexing.converter;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Converter<S, E> {

    class ConverterException extends Exception {

        public ConverterException(String message, Exception e) {
            super(message, e);
        }
    }

    Iterable<E> convert(S t) throws ConverterException;
}
