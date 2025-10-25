pipeline {
	agent any

	tools {
		maven 'MAVEN'
	}

	environment {
		BASE_URL = 'http://auth-app:8080'
		SONARQUBE_ENV = 'sonar-local'
	}

	stages {
		stage('Checkout') {
			steps {
				checkout scm
			}
		}

		stage('Test (Cucumber)') {
			steps {
				sh "mvn -q clean test -DBASE_URL=${BASE_URL}"
			}
			post {
				always {
					junit '**/target/surefire-reports/*.xml'
					script {
						try {
							sh "mvn -q io.qameta.allure:allure-maven:2.12.0:report"
							publishHTML(target: [
								reportDir: 'target/site/allure-maven-plugin',
								reportFiles: 'index.html',
								reportName: 'Allure Report',
								keepAll: true,
								alwaysLinkToLastBuild: true,
								allowMissing: false
							])
						} catch (err) {
							echo "No se pudo publicar el reporte HTML: ${err}"
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
              -Dsonar.projectKey=pruebas-auth \
              -Dsonar.projectName=pruebas-auth
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
		success {
			echo 'Build OK'
		}
		failure {
			echo 'Build FAIL'
		}
	}
}
