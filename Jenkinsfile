pipeline {
	agent any

	tools {
		// Usa el nombre EXACTO del Maven configurado en Manage Jenkins > Tools (en tus logs era "MAVEN")
		maven 'MAVEN'
		// Si definiste un JDK en Tools y quieres usarlo, descomenta:
		// jdk 'JDK17'
	}

	environment {
		// Desde el contenedor de Jenkins, usa el nombre del servicio del compose
		BASE_URL     = 'http://auth-app:8080'   // ajusta el puerto si tu auth-app expone otro
		SONARQUBE_ENV = 'sonar-local'           // nombre del servidor Sonar en Manage Jenkins > System
	}

	stages {
		stage('Checkout') {
			steps {
				// En "Pipeline script from SCM" usa la config del job
				checkout scm
			}
		}

		stage('Test (Cucumber)') {
			steps {
				sh "mvn -q clean test -DBASE_URL=${BASE_URL}"
			}
			post {
				always {
					// Publica resultados de JUnit
					junit '**/target/surefire-reports/*.xml'

					// Genera reporte Allure con Maven y publícalo como HTML (si está el plugin Publish HTML)
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
							echo "No se pudo publicar el reporte HTML (¿falta plugin Publish HTML?). Detalle: ${err}"
						}
					}
				}
			}
		}

		stage('SonarQube Analysis') {
			steps {
				// Inyecta SONAR_HOST_URL y credenciales configuradas en Jenkins
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
					// Configura en Sonar: Administration > Configuration > Webhooks -> http://jenkins:8080/sonarqube-webhook/
					timeout(time: 2, unit: 'MINUTES') {
						waitForQualityGate abortPipeline: true
					}
				}
			}
		}
	}

	post {
		success { echo '✅ Build OK: tests + Sonar + reportes listos.' }
		failure { echo '❌ Build FAIL.' }
	}
}
