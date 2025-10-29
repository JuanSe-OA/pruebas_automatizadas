pipeline {
  agent any

  tools {
    maven 'MAVEN'
  }

  environment {
    // Puertos tomados del .env de tu docker-compose
    AUTH_APP_PORT = '8080'
    MONITORING_APP_PORT = '8083'    // definir en .env
    SONARQUBE_ENV = 'sonar-local'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Test (Cucumber) - AUTH') {
      environment {
        BASE_URL = "http://auth-app:${AUTH_APP_PORT}"
      }
      steps {
        sh "mvn -q clean test -DBASE_URL=${BASE_URL}"
      }
      post {
        always {
          junit '**/target/surefire-reports/*.xml'
          script {
            try {
              sh "mvn -q io.qameta.allure:allure-maven:2.12.0:report"
              // Mueve el reporte para no pisarlo luego
              sh "mkdir -p target/reports-auth && cp -r target/site/allure-maven-plugin/* target/reports-auth/"
              publishHTML(target: [
                reportDir: 'target/reports-auth',
                reportFiles: 'index.html',
                reportName: 'Allure Report - AUTH',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: false
              ])
            } catch (err) {
              echo "No se pudo publicar el reporte AUTH: ${err}"
            }
          }
        }
      }
    }

    stage('Test (Cucumber) - MONITORING') {
      environment {
        BASE_URL = "http://monitoring-app:${MONITORING_APP_PORT}"
      }
      steps {
        // Opción 1: si separas escenarios con tags, usa -Dcucumber.filter.tags='@monitoring'
        // Opción 2: si no usas tags, así como está (reutiliza tus stepdefs para apuntar al BASE_URL de monitoring)
        sh "mvn -q clean test -DBASE_URL=${BASE_URL} -DfailIfNoTests=false"
        // Ejemplo con tags:
        // sh \"mvn -q clean test -DBASE_URL=${BASE_URL} -Dcucumber.filter.tags='@monitoring' -DfailIfNoTests=false\"
      }
      post {
        always {
          junit '**/target/surefire-reports/*.xml'
          script {
            try {
              sh "mvn -q io.qameta.allure:allure-maven:2.12.0:report"
              sh "mkdir -p target/reports-monitoring && cp -r target/site/allure-maven-plugin/* target/reports-monitoring/"
              publishHTML(target: [
                reportDir: 'target/reports-monitoring',
                reportFiles: 'index.html',
                reportName: 'Allure Report - MONITORING',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: false
              ])
            } catch (err) {
              echo "No se pudo publicar el reporte MONITORING: ${err}"
            }
          }
        }
      }
    }

    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv("${SONARQUBE_ENV}") {
          sh """
            mvn -q -DskipTests sonar:sonar \
              -Dsonar.projectKey=pruebas-e2e \
              -Dsonar.projectName=pruebas-e2e
          """
        }
      }
    }

    stage('Quality Gate') {
      steps {
        script {
          timeout(time: 2, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: true
          }
        }
      }
    }
  }

  post {
    success { echo 'Build OK' }
    failure { echo 'Build FAIL' }
  }
}
