To run this POC we need kafka cluster that support replication that is why I created docker compose for its sake.
After launching docker compose open kafdrop (localhost:9000) and create two kafka topics:
-"media-input" with replication factor set to 3
-"media-output" with replication factor set to 3
After further investigation we might come to conclusion that we need more topics but for now It works with just two.
We also need to install image magick with version 7 or higher for sake of media topology (I used method 2 from this
guide: https://linuxopsys.com/topics/install-latest-imagemagick-on-ubuntu).

To start workflow we need to send sort of task definition to media-input queue.
Currently, we are using file that is present in ecloud mcs.
To input to kafka queue we can use binary that is attached to kafka package in kafka_
{version}/bin/kafka-console-producer.sh
Example method execution:
./kafka-console-producer.sh --topic media-input --bootstrap-server localhost:9092

minimal task definition syntax:
{"fileUrl": "{mcs-file-path}","throttleLevel":"{MEDIUM | LOW | HIGH}", "taskId":"{long number}", "outputDataset":"
{dataset id}"}

Example of task definition:
{"
fileUrl": "https://ecloud-test.apps.dcw1.paas.psnc.pl/mcs/records/F4VSSQKDRUEECT7OUSLLLBTLPI2SD57P7R5E6JCBOENATL3DVRFA/representations/metadataRecord/versions/3ef665b0-b933-11ee-8000-4e008e54a287/files/dceef4b7-d9c1-3632-8715-34301a7c7851","throttleLevel":"MEDIUM", "
taskId":"123", "outputDataset":"02749bdb-eaac-4794-98cd-ebdd6de1ce45"}