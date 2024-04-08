#!groovy
library 'global-alm-pipeline-library'


pipeline {
    agent {
        node {
            // set maven slave
            label 'maven'
        }
    }   
       stages {
            stage('invoke Fortify analyzer') {
                 steps{
                      script{
                                def scanresult = almFortify([

                                     application: 'calypso-stc',

                                     version: '11.2.0',

                                     bt: 'mvn',

                                     bc: 'compile'])

                                echo "Scan Result: " + scanresult
                      }//steps
                 }//scripts
            }//stage
        }
}
