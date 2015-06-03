# C3-PRO-Server #

C3-PRO-Server is a highly reliable and scalable FHIR DSTU2 compliant web server, designed to cope with the traffic from mobile apps. The current version can only be deployed in AWS. It populates an AWS SQS with the FHIR resources that are POST. It does not consume the queue. A consumer can be found in the project [c3pro-consumer] (https://bitbucket.org/ipinyol/c3pro-server)

The system servers the following rest methods:

    HTTP/1.1 GET /c3pro/fhir/Questionnaire
    HTTP/1.1 POST /c3pro/fhir/QuestionnaireAnswer
    HTTP/1.1 POST /c3pro/fhir/Contract

It uses oauth2 two legged for authorization, which needs an initial phase for registration:

**Registration request:**

    HTTP/1.1 POST /c3pro/register
    HTTP/1.1 Header Antispam: {{in-app-stored secret}}
    {
      “sandbox”: true/false,
      “receipt-data”: {{your apple-supplied app purchase receipt}}
    }

**Registration response:**

    HTTP/1.1 201 Created
    Content-Type: application/json
    {
      "client_id":"{{some opaque client id}}",
      "client_secret": "{{some high-entropy client secret}}",
      "grant_types": ["client_credentials"],
      "token_endpoint_auth_method":"client_secret_basic",
    }

The registration phase should be called only once per device. Once the device is registered, the same client_id and client_secret must be user in future oauth calls.

**Oauth2 authorization request**

    HTTP/1.1 POST /c3pro/oauth?grant_type=client_credentials
    Authentication: Basic BASE64(ClientId:Secret)

**Oauth2 authorization response**

    HTTP/1.1 201 Created
    Content-Type: application/json
    {
      "access_token":"{{some token}}",
      "expires_in": "{{seconds to expiration}}",
      "token_type": "bearer",
    } 

The Bearer token can be used in the rest calls that serve FHIR resources as authorization credentials.

# Configuration and Deployment #

## AWS prerequisites ##

The following services must be deployed in AWS:

* **S3 bucket**: The system uses an S3 bucket to serve static content like Questionnaire resources and to store the public key of the Consumer
* **SQS queue**: A queue to store the pushed FHIR resources
* **Oracle RDS DB**: the system uses an oracle schema to manage credentials. Technically, it is not necessary to use a db schema deployed in AWS, but is highly recommended.

The access to S3 and SQS can be configured in the {{config.properties}} of each resource directories (dev, qa and prod). The access to the oracle DB must be configured as a datasource in the jboss {{standalone.xml}} file. See below.

## Installing Maven, Java && JBoss AS7 ##

The system uses java 7 and we recommend to use JBoss AS7. To install the basic tools in a Debian-based Linux distribution:

    sudo apt-get clean
    sudo apt-get update
    sudo apt-get install openjdk-7-jdk
    sudo apt-get install unzip
    sudo apt-get install maven
    wget http://download.jboss.org/jbossas/7.1/jboss-as-7.1.1.Final/jboss-as-7.1.1.Final.zip
    sudo unzip jboss-as-7.1.1.Final.zip -d /usr/share/
    sudo chown -fR {{you_chosen_user}}:{{you_chosen_user}} /usr/share/jboss-as-7.1.1.Final/

## Oracle DB configuration ##

the systems uses an oracle DB to manage credentials and bearer token. Here are the steps to configure the DB properly:

1. Run the table creation script: {{src/main/scripts/create_tables.sql}} in the DB
2. Insert an antispam token:
    
    insert into AntiSpamToken (token) values ('{{the_token_hashed_with_sha1}}');

  To generate sha1 hashed token execute the script: {{src/main/scripts/generate_hashed_token.sql}} replacing {{"REPLACE by a high entropy token"}} by the desired anti spam token. 





