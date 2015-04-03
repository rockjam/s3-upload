# s3-upload
Test exercise with Amazon S3 file upload/download

```scala
sbt run
```
to launch server. 
Amazon credentials should be located in ~/.aws/credentials

There are two resources:
* GET /file/ID - download file with given ID from amazon s3 bucket 
* POST /file/ID - upload file with given ID to amazon s3 bucket
