package eu.ehri.project.indexing.converter;

/**
 * A class that converts items of one type into one or many items
 * of another type.
 */
public interface Converter<S, E> {

    class ConverterException extends Exception {

        public ConverterException(String message, Exception e) {
            super(message, e);
        }
    }

    /**
     * Convert a single item into an iterable of its counterpart items.
     *
     * @param t the input item type
     * @return an iterable of counterpart items
     * @throws ConverterException if there is a conversion error
     */
    Iterable<E> convert(S t) throws ConverterException;
}
