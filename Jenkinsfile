pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'Temurin-17'
    }

    environment {
        IMAGE_NAME = "dispenser-platform"
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
        // Falls back to "local" when no registry is configured on this
        // controller (e.g. a local trial run), instead of failing the whole
        // pipeline outright on a missing credential.
        DOCKER_REGISTRY = "${env.DOCKER_REGISTRY_URL ?: 'local'}"
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
            // Skipped unless a "sonarqube" server is configured on this
            // controller (Manage Jenkins > System > SonarQube servers) —
            // lets the pipeline run end-to-end on a fresh Jenkins without
            // failing on infrastructure that isn't there yet.
            when { expression { return env.SONAR_HOST_URL?.trim() } }
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh 'mvn -B sonar:sonar -Dsonar.projectKey=dispenser-platform'
                }
            }
        }

        stage('Quality Gate') {
            when { expression { return env.SONAR_HOST_URL?.trim() } }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Integration Test') {
            steps {
                // Calling the Failsafe goals directly (rather than "mvn
                // verify", which re-triggers the "test" phase and therefore
                // Surefire) reuses the classes already compiled and tested
                // in the previous stage, instead of running unit tests
                // twice. Full Spring context against the H2 "test" profile.
                sh 'mvn -B failsafe:integration-test failsafe:verify'
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

        stage('Publish to GitLab') {
            // Skipped unless GITLAB_PROJECT_ID is set on this controller
            // (Manage Jenkins > System > Global properties), same pattern
            // as the SonarQube guard above — lets this run on controllers
            // that have GitLab configured, and skip cleanly on ones that
            // don't.
            when { expression { return env.GITLAB_PROJECT_ID?.trim() } }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'gitlab-deploy-token',
                    usernameVariable: 'GITLAB_DEPLOY_USERNAME',
                    passwordVariable: 'GITLAB_DEPLOY_PASSWORD'
                )]) {
                    sh 'mvn -B -DskipTests deploy -s settings-gitlab.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest ."
            }
        }

        stage('Docker Push') {
            // Deploy is opt-in even on main: requires ENABLE_DEPLOY=true as
            // a controller/job env var, so a fresh or local Jenkins never
            // pushes/deploys by accident just because it built on main.
            when {
                allOf {
                    branch 'main'
                    expression { return env.ENABLE_DEPLOY == 'true' }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', usernameVariable: 'DUSER', passwordVariable: 'DPASS')]) {
                    sh 'echo "$DPASS" | docker login ${DOCKER_REGISTRY} -u "$DUSER" --password-stdin'
                    sh "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                    sh "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest"
                }
            }
        }

        stage('Deploy (Ansible)') {
            when {
                allOf {
                    branch 'main'
                    expression { return env.ENABLE_DEPLOY == 'true' }
                }
            }
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
            script {
                try {
                    cleanWs()
                } catch (err) {
                    echo "Workspace Cleanup plugin not installed; skipping cleanWs()."
                }
            }
        }
    }
}
