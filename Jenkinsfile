pipeline {
    agent { node { label 'srs-build01' } }
    tools {
        maven 'maven3'
        jdk 'jdk8'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
    }

    parameters {
        booleanParam(name: "RELEASE",
                description: "If selected, build a release from current commit. Otherwise build currrent commit.",
                defaultValue: false)
        string(name: 'RELEASE_VERSION', defaultValue: '', description: 'If RELEASE is selected, the Release Version (e.g. 2.1)')
    }

    stages {
        stage ('Build') {
            steps {
                sh 'mvn -s /nfs/slac/g/srs/hudson/hudson-settings-srs.xml -U clean verify' 
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    junit testResults: 'target/surefire-reports/**/*.xml', allowEmptyResults: true
                }
            }
        }
        stage ('Deploy') {
            when {
                branch 'master'
            }
            steps {
                sh 'mvn -s /nfs/slac/g/srs/hudson/hudson-settings-srs.xml -U validate jar:jar deploy:deploy'
            }
        }
        stage('Release') {
            when {
                expression { params.RELEASE }
                branch 'master'
            }
            steps {
                script {
                    currentBuild.displayName = "${RELEASE_VERSION}"
                }
                // Always need a symbolic ref for maven release
                sh "mvn -s /nfs/slac/g/srs/hudson/hudson-settings-srs.xml \
                       -PHudson -U -Dresume=false \
                       release:prepare \
                       release:perform \
                       -DreleaseVersion=${RELEASE_VERSION} \
                   "
            }
            post {
                success {
                    withCredentials([usernamePassword(credentialsId: "feb9edc9-2596-4e5a-aba9-0fc8222f7ef3", passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USER')]) {
                        // Setup credentials helper so we can get credentials
                        sh """git config credential.helper '!f() { sleep 1; echo "username=${GIT_USER}"; echo "password=${GIT_PASSWORD}"; }; f'"""
                        sh "git push origin org-srs-datacat-${RELEASE_VERSION}"
                    }
                }
            }
        }
    }
    post {
        failure {
           emailext recipientProviders: [brokenTestsSuspects(), brokenBuildSuspects(), developers()],
           subject: '${DEFAULT_SUBJECT}',
           body: '${DEFAULT_CONTENT}'
        }
        always {
            deleteDir()
        }
    }
}
