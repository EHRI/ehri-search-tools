

# Multilingual indexing 
We identify the language of several fields in the metadata and we index them in the language in which the text is written. 
Query is searched simultaneously in the different languages and the results merged in a single response.

The actual request handler is /multilingsearch
e.g. for "Auschwitz" the query is
http://localhost:8983/solr/portal/multilingsearch?q=Auschwitz&wt=json&indent=true

# Term expansion with SKOS
Copy the *rdf files into the solr/portal/conf directory.
Follow the instructions of 
https://github.com/KepaJRodriguez/lucene-skos-ehri/blob/master/README.md


## Libraries
Following libraries must be copied in the ...solr/lib/ directory

a) For Unicode normalization
* icu4j-49.1.jar --> Delivered with the Solr distribution in solr-4.X.X/contrib/analysis-extras/lib
* lucene-analyzers-icu-4.5.0.jar --> Delivered with the Solr distribution in solr-4.X.X/contrib/analysis-extras/lucene-libs

b) For Polish text analyisis
* lucene-analyzers-morfologik-4.5.0.jar --> Delivered with the Solr distribution in solr-4.X.X/contrib/analysis-extras/lucene-libs
* morfologik-fsa-1.7.1.jar --> Delivered with the Solr distribution in solr-4.X.X/contrib/analysis-extras/lib
* morfologik-polish-1.7.1.jar --> Delivered with the Solr distribution in solr-4.X.X/contrib/analysis-extras/lib
* morfologik-stemming-1.7.1.jar --> Delivered with the Solr distribution in solr-4.X.X/contrib/analysis-extras/lib

   Together with the libraries, the list of stopwords stopwords_pl.txt must be included in portal/conf/lang folder

c) For language identification 
* langdetect-1.1-20120112.jar --> Delivered with the Solr distribution in solr-4.X.X/contrib/langid/lib 
* jsonic-1.2.7.jar --> Delivered with the Solr distribution in solr-4.X.X/contrib/langid/lib  
* solr-langid-4.5.0.jar--> Delivered with the Solr distribution in solr-4.X.X/contrib/distr


## Chronology of changes

24.10.2013
- schema.xml has been updated to remove diacritics from query and indexes using ICUFoldingFilterFactory.

29.10.2014
- schema.xml and solarconf.xml have been modified to allow multilingual indexing and multilingual simultaneous querying.
- polish list of stopwords have been added
- modification in code of the indexer in file JSONConversion.java to change the output and allow language identification in the desired fields

19.11.2014
- schema.xml modified for SKOS based term expansion
- three SKOS vocabularies added