# GeoCoding service 
Built using data from open street map indexed using lucene.

Download the osm data extract file ...osm.pbf you need from either geofabrik or planetosm

We will be using osm2psgl utiity to import osm data into a postgresql database. Simple scripts built on top of osm2pgsql has been provided to import data for using with this application.



## Import osm data to a staging schema (e.g osm) 

bin/stageosm osm **full-path-to-your-pbf-file**


## import from staging schema (osm) to application schema (osm-succinct) 

bin/importosm osm osm_succinct 


## now bring up the application 
bin/swfstart.real 

It takes close to 4 hours to index the database just created. So go to sleep and comeback later to find your shiny geo coding service. 

## To Test your application
curl -H "content-type:application/json" "http://localhost:3030/locations/search?q=Pride+Apartments+Bannergatta+Road+Billekahalli+Bangalore+560076&maxRecords=1"

