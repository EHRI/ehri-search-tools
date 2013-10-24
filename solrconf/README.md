24.10.2013
schema.xml has been updated to remove diacritics from query and indexes using ICUFoldingFilterFactory.


Libraries
Following libraries must be copied in the ...solr/lib/ directory

a) For Unicode normalization
   
* icu4j-49.1.jar --> Delivered with the Solr distribution in solr-4.X.X/contrib/analysis-extras/lib
* lucene-analyzers-icu-4.5.0.jar -->  Delivered with the Solr distribution in solr-4.X.X/contrib/analysis-extras/lucene-libs

b) For Polish text analyisis
   tbd
