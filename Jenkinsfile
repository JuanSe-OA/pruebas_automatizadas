pipeline {
  agent any

  environment {
    BASE_URL = 'http://auth-app:8081'   // o 8080 según tu compose
    SONARQUBE_ENV = 'sonar-local'       // nombre configurado en Jenkins
  }

  tools {
    maven 'Maven 3.9.11'                  // el nombre que configuraste en Jenkins
    jdk 'JDK17'                          // opcional si definiste un JDK en Jenkins
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Test') {
      steps {
        sh "mvn -q clean test -DBASE_URL=${BASE_URL}"
      }
      post {
        always {
          junit '**/target/surefire-reports/*.xml'
          // Si activaste Allure en el pom:
          allure includeProperties: false, jdk: '', results: [[path: 'target/allure-results']]
        }
      }
    }

    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv("${SONARQUBE_ENV}") {
          sh """
             mvn -q -DskipTests sonar:sonar \
                -Dsonar.projectKey=pruebas-auth \
                -Dsonar.projectName=pruebas-auth \
                -Dsonar.host.url=$SONAR_HOST_URL
          """
        }
      }
    }

    stage('Quality Gate') {
      steps {
        timeout(time: 2, unit: 'MINUTES') {
          script { waitForQualityGate abortPipeline: true }
        }
      }
    }
  }
}
