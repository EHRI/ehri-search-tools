package eu.ehri.project.indexing.converter;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Converter<T> {

    class ConverterException extends Exception {
        public ConverterException(String message) {
            super(message);
        }

        public ConverterException(String message, Exception e) {
            super(message, e);
        }
    }

    Iterable<T> convert(T t) throws ConverterException;
}
