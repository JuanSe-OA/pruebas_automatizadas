pipeline {
	agent any

	/***********************
	 * Herramientas/JDK
	 ***********************/
	tools {
		// Usa el nombre EXACTO del Maven que ves en Manage Jenkins > Tools (en tus logs salió "MAVEN")
		maven 'MAVEN'
		// Si definiste un JDK en Tools, puedes declararlo así:
		// jdk 'JDK17'
	}

	/***********************
	 * Variables de entorno
	 ***********************/
	environment {
		// Desde el contenedor de Jenkins, llama al auth por NOMBRE DE SERVICIO del compose
		// Ajusta el puerto si tu auth-app corre en otro (8081, etc.)
		BASE_URL = 'http://auth-app:8080'
		// Nombre del servidor Sonar configurado en Manage Jenkins > System
		SONARQUBE_ENV = 'sonar-local'
	}

	options {
		// Mantiene logs más limpios
		timestamps()
		ansiColor('xterm')
		// Falta una etapa => marca el build como fallo
		disableConcurrentBuilds()
	}

	stages {
		stage('Checkout') {
			steps {
				// En "Pipeline script from SCM" este checkout usa la config del job
				checkout scm
			}
		}

		stage('Test (Cucumber)') {
			steps {
				sh "mvn -q clean test -DBASE_URL=${BASE_URL}"
			}
			post {
				always {
					// Publica resultados JUnit para ver escenarios/steps fallidos
					junit '**/target/surefire-reports/*.xml'

					// --- Publicación de Allure sin depender del plugin de Jenkins ---
					// Genera el reporte estático con Maven y publícalo como HTML
					sh "mvn -q io.qameta.allure:allure-maven:2.12.0:report"
					publishHTML(target: [
						reportDir: 'target/site/allure-maven-plugin',
						reportFiles: 'index.html',
						reportName: 'Allure Report',
						keepAll: true,
						alwaysLinkToLastBuild: true,
						allowMissing: false
					])
				}
			}
		}

		stage('SonarQube Analysis') {
			steps {
				// Inyecta SONAR_HOST_URL + TOKEN configurados en Jenkins (Manage Jenkins > System > SonarQube servers)
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
					// Para que funcione de inmediato, crea el webhook en Sonar:
					// Administration > Configuration > Webhooks -> http://jenkins:8080/sonarqube-webhook/
					timeout(time: 2, unit: 'MINUTES') {
						waitForQualityGate abortPipeline: true
					}
				}
			}
		}
	}

	post {
		failure {
			echo '❌ Build failed.'
		}
		success {
			echo '✅ Build ok: tests + Sonar + reportes listos.'
		}
	}
}
