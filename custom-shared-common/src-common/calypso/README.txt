--- COMPILING JARS ---
This jars are needed for compilation purposes only (CUP compilation),
they aren't included in the CUP distribution.

--- DISTRIBUTED JARS ---
This jars are going to be packaged within the CUP file, so they are
needed in runtime (both server or client). They could also be needed
for compilation purposes.

--- NOTES ---
Following jars must be included in Calypso's base installation,
but right now they aren't. That's why they are included in CUP's
distribution. These mustn't reach production inside the CUP file.

calypso-datauploader-7.6.2-16.1.0.0.jar         com.ibm.mq.commonservices.jar  com.ibm.mq.jmqi.jar        com.ibm.mqjms.jar   tibjms.jar
calypso-integration-service-2.1.0-16.1.0.0.jar  com.ibm.mq.headers.jar         com.ibm.mq.jms.Nojndi.jar  dozer-5.5.1.jar     tibjmsadmin.jar
calypsoml-datauploader-impl-7.6.2-16.1.0.0.jar  com.ibm.mq.jar                 com.ibm.mq.pcf.jar         fscontext.jar


----------------------------
Greetings developers...
