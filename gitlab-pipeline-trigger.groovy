import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic

pipeline {
  agent any
  
  stages {
    stage('Call GitLab pipeline') {
      steps {
        script {
        
          def yourProjectID = <Your project ID>
          def yourGiblabToken = <Your project token>  
          
            def gitlabPipelineUrl = 'https://gitlab.com/api/v4/projects/${yourProjectID}/pipeline'
          def gitlabToken = yourGiblabToken
          
          def gitlabPipelineParams = [
            'ref': 'master'
          ]
          
          def gitlabStatusUrl = null
          
          def gitlabPipelineResponse = httpRequest url: gitlabPipelineUrl, acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_FORM', customHeaders: [[name: 'PRIVATE-TOKEN', value: gitlabToken]], httpMode: 'POST'

          if (gitlabPipelineResponse.status != 201) {
            error("Failed to trigger GitLab pipeline: ${gitlabPipelineResponse.status} - ${gitlabPipelineResponse.content}")
          } else {
            def json = new JsonSlurper().parseText(gitlabPipelineResponse.content)
            
            def gitlabPipelineId = json.id

            echo "GitLab pipeline ${gitlabPipelineId} has been triggered"
            json = null
            
            // Poll for GitLab pipeline status
            gitlabStatusUrl = "${gitlabPipelineUrl}s/${gitlabPipelineId}"
            def gitlabStatusResponse = null
            def gitlabStatus = null
            
        
            for (int i = 0; i < 60; i++) {

              gitlabStatusResponse = httpRequest url: gitlabStatusUrl, acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_FORM', customHeaders: [[name: 'PRIVATE-TOKEN', value: gitlabToken]], httpMode: 'GET'

              json = new JsonSlurper().parseText(gitlabStatusResponse.content)
              echo "GitLab status response: ${json}"
              gitlabStatus = json.status
              echo "GitLab pipeline status: ${gitlabStatus}"
              json=null
              
              if (gitlabStatus == 'success') {
                
                
                break
              } else if (gitlabStatus == 'failed' || gitlabStatus == 'canceled') {
                
                
                error("GitLab pipeline failed with status ${gitlabStatus}")
              }
              
              sleep(10)
            }
          }
          
          def gitlabJobsUrl = "${gitlabStatusUrl}/jobs"
          def gitlabJobsResponse= null
          def gitlabJobs= null
                def gitlabJobIDs = []
                def gitlabJobNames = []
                def gitlabJobLogResponse = null
                def gitlabJobLogUrl = null  
                def gitlabJobArtifactUrl = null
                def gitlabJobArtifactResponse = null
                
                gitlabJobsResponse = httpRequest url: gitlabJobsUrl, acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_FORM', customHeaders: [[name: 'PRIVATE-TOKEN', value: gitlabToken]], httpMode: 'GET'
                gitlabJobs = new JsonSlurper().parseText(gitlabJobsResponse.content)
                gitlabJobs.each{ gitlabJob ->
                gitlabJobID = gitlabJob.id
                gitlabJobName = gitlabJob.name

                echo("Retrieving names and ids for the gitlab pipeline jobs")
                echo("Job ID: ${gitlabJobID}, name: ${gitlabJobName}:")
                gitlabJobIDs << gitlabJobID
                gitlabJobNames << gitlabJobName
                gitlabJob = null
                }
                gitlabJobs = null
                
                echo ("===============================Showing the job logs and downloading the job artifacts: ================================")
                for (int i = 0; i < gitlabJobIDs.size(); i++) {
                gitlabJobLogUrl = "https://gitlab.com/api/v4/projects/44253509/jobs/${gitlabJobIDs[i]}/trace"
                gitlabJobArtifactUrl = "https://gitlab.com/api/v4/projects/44253509/jobs/artifacts/master/download?job=${gitlabJobNames[i]}"
                
                echo ("-------------------------------logs for job ${gitlabJobNames[i]}: ================================")
                def response = sh "curl --header \"PRIVATE-TOKEN: ${gitlabToken}\" \"${gitlabJobLogUrl}\""
                response = null
                
                echo ("-------------------------------Downloading artifact for job ${gitlabJobNames[i]}: ================================")
                response = sh "curl --location --output ${gitlabJobNames[i]}.zip --header \"PRIVATE-TOKEN: ${gitlabToken}\" \"${gitlabJobArtifactUrl}\""
                response = null

                }

        }
      }
    }
  }
}
