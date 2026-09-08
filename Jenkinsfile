pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'Temurin-17'
    }

    environment {
        IMAGE_NAME = "vtec-dispenser-platform"
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
        DOCKER_REGISTRY = credentials('docker-registry-url')
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                // "mvn test" runs Surefire only: fast, mocked unit tests.
                sh 'mvn -B clean test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Static Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh 'mvn -B sonar:sonar -Dsonar.projectKey=vtec-dispenser-platform'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Integration Test') {
            steps {
                // "mvn verify" additionally runs Failsafe (*IT): full Spring
                // context against the H2 "test" profile. Surefire already
                // ran in the previous stage, so skip re-running it here.
                sh 'mvn -B verify -Dsurefire.skip=true'
            }
            post {
                always {
                    junit 'target/failsafe-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B -DskipTests package'
                archiveArtifacts artifacts: 'target/dispenser-platform.jar', fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest ."
            }
        }

        stage('Docker Push') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', usernameVariable: 'DUSER', passwordVariable: 'DPASS')]) {
                    sh 'echo "$DPASS" | docker login ${DOCKER_REGISTRY} -u "$DUSER" --password-stdin'
                    sh "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                    sh "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest"
                }
            }
        }

        stage('Deploy (Ansible)') {
            when { branch 'main' }
            steps {
                sh """
                    ansible-playbook -i ansible/inventory.ini ansible/deploy.yml \
                        -e app_image=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }
    }

    post {
        failure {
            echo "Pipeline failed at ${env.STAGE_NAME}. See console output for details."
        }
        always {
            cleanWs()
        }
    }
}
