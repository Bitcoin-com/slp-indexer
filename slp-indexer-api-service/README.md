# Bitcoin.com SLP-indexer API
**last updated:** 2019-12-16

* 1. [What will you find here](#what)

##  1. <a name='what'></a>What will you find here?

This folder includes the API part of the indexer. It is currently built using java spring-boot and RxJava. The endpoints available corresponds to
the enpoints needed to serve the SLP data on rest.bitcoin.com

Since the indexer writer and api is completely independent you can write your own api on top of the data provided in mongodb.
Any language than can open a connection can build an api on top of the data provided by the writer.
