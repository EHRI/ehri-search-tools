package eu.ehri.project.indexer.converter;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Converter<T> {

    public static class ConverterException extends Exception {
        public ConverterException(String message) {
            super(message);
        }

        public ConverterException(String message, Exception e) {
            super(message, e);
        }
    }

    public Iterable<T> convert(T t) throws ConverterException;
}
