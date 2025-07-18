/*
 * See https://github.com/hibernate/hibernate-jenkins-pipeline-helpers
 */
@Library('hibernate-jenkins-pipeline-helpers') _

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  	print "INFO: Build skipped due to trigger being Branch Indexing"
	currentBuild.result = 'NOT_BUILT'
  	return
}

def checkoutReleaseScripts() {
	dir('.release/scripts') {
		checkout scmGit(branches: [[name: '*/main']], extensions: [],
				userRemoteConfigs: [[credentialsId: 'ed25519.Hibernate-CI.github.com',
									 url: 'https://github.com/hibernate/hibernate-release-scripts.git']])
	}
}

pipeline {
    agent {
        label 'Release'
    }
    tools {
        jdk 'OpenJDK 17 Latest'
    }
    options {
  		rateLimitBuilds(throttle: [count: 1, durationName: 'hour', userBoost: true])
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '3'))
        disableConcurrentBuilds(abortPrevious: true)
    }
	stages {
        stage('Checkout') {
        	steps {
				checkout scm
			}
		}
		stage('Publish') {
			steps {
				script {
					withCredentials([
							// TODO: Once we switch to maven-central publishing (from nexus2) we need to update credentialsId:
							//  https://docs.gradle.org/current/samples/sample_publishing_credentials.html#:~:text=via%20environment%20variables
							usernamePassword(credentialsId: 'central.sonatype.com', passwordVariable: 'ORG_GRADLE_PROJECT_snapshotsPassword', usernameVariable: 'ORG_GRADLE_PROJECT_snapshotsUsername'),
							gitUsernamePassword(credentialsId: 'username-and-token.Hibernate-CI.github.com', gitToolName: 'Default'),
							string(credentialsId: 'Hibernate-CI.github.com', variable: 'JRELEASER_GITHUB_TOKEN')
					]) {
						checkoutReleaseScripts()
						def version = sh(
								script: ".release/scripts/determine-current-version.sh reactive",
								returnStdout: true
						).trim()
						echo "Current version: '${version}'"
						sh "bash -xe .release/scripts/snapshot-deploy.sh reactive ${version}"
					}
				}
			}
        }
    }
    post {
        always {
    		configFileProvider([configFile(fileId: 'job-configuration.yaml', variable: 'JOB_CONFIGURATION_FILE')]) {
            	notifyBuildResult maintainers: (String) readYaml(file: env.JOB_CONFIGURATION_FILE).notification?.email?.recipients
            }
        }
    }
}
