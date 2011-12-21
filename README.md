About IndexTank Engine
======================

This project contains IndexTank (http://indextank.com) search engine implementation.
Includes features like variables (boosts), categories (facets), faceted search, snippeting, custom scoring functions, suggest, and autocomplete.

### Homepage:

Find out more about at: TBD

### License:

Apache Public License (APL) 2.0

### Artifacts:

1. engine-1.0.0.jar <-- core library

### Maven:

groupId: com.flaptor.indextank

artifactId: engine

version: 1.0.0

### Package generation:

Build a single jar containing all dependencies by:

mvn assembly:single

This will create a single file in:

target/engine-1.0.0-jar-with-dependencies.jar

### Sample configuration:

Main class: com.flaptor.indextank.index.IndexEngine

VM args: -verbose:gc -Xloggc:logs/gc.log -XX:+PrintGCTimeStamps -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -Dorg.apache.lucene.FSDirectory.class=org.apache.lucene.store.MMapDirectory -Dapp=INDEX-ENGINE-dgmqn -Xmx600M

Program args: --facets --rti-size 500 --conf-file sample-engine-config --port 20220 --environment-prefix TEST --recover --dir index --snippets --suggest documents --boosts 3 --index-code dgmqn --functions 0:-age

Sample engine configuration file contents:
{
"max_variables": 3, 
"functions": {"0": "-age"}, 
"index_code": "dgmqn", 
"allows_facets": true, 
"ram": 600, 
"log_server_host": "index123.localhost", 
"autocomplete": true,
"log_server_port": 15100, 
"autocomplete_type": "documents",
"allows_snippets": true, 
"rti_size": 500, 
"facets_bits": 5, 
"base_port": 20220, 
"log_based_storage": false, 
"xmx": 600
}


### Eclipse:

Set up Eclipse for this project by executing the command below:

mvn eclipse:eclipse

Inside Eclipse, select Preferences > Java > Build Path > Classpath Variables. Define a new classpath variable M2_REPO and assign maven repository.

For more information, check out http://maven.apache.org/guides/mini/guide-ide-eclipse.html

