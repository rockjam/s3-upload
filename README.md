# s3-upload
Test exercise with Amazon S3 chunked file upload/download

To run this example you should provide your Amazon AWS credentials
You can fill them in application.conf file or export as environment variables:
* ACCESS_KEY_ID
* SECRET_ACCESS_KEY

```scala
sbt run
```
to launch server.

Two routes are defined:
* GET /file/ID - download file with given ID from amazon s3 bucket 
* POST /file/ID - upload file with given ID to amazon s3 bucket