pipeline {
	agent any

	tools {
		maven 'MAVEN'
	}

	options {
		timestamps()
		ansiColor('xterm')
	}

	environment {
		AUTH_BASE_URL = 'http://auth-app:8080'
		MON_BASE_URL  = 'http://monitoring-app:8084'
		SONARQUBE_ENV = 'sonar-local'
	}

	stages {

		stage('Checkout') {
			steps {
				checkout scm
			}
		}

		// ===== PRUEBAS AUTH =====
		stage('Test (Cucumber) - AUTH') {
			steps {
				sh """
					echo "🔹 Ejecutando pruebas @auth..."
					mvn -q clean test \
						-DBASE_URL=${AUTH_BASE_URL} \
						-DfailIfNoTests=false \
						-Dcucumber.filter.tags='@auth'

					# Generar reporte Allure
					mvn -q io.qameta.allure:allure-maven:2.12.0:report
					mkdir -p target/reports-auth && cp -r target/site/allure-maven-plugin/* target/reports-auth/ || true
				"""
			}
			post {
				always {
					junit '**/target/surefire-reports/*.xml'
					publishHTML(target: [
						reportDir: 'target/reports-auth',
						reportFiles: 'index.html',
						reportName: 'Allure Report - AUTH',
						keepAll: true,
						alwaysLinkToLastBuild: true,
						allowMissing: true
					])
				}
			}
		}

		// ===== PRUEBAS MONITORING =====
		stage('Test (Cucumber) - MONITORING') {
			steps {
				sh """
					echo "🔹 Ejecutando pruebas @monitoring..."
					mvn -q clean test \
						-DBASE_URL=${MON_BASE_URL} \
						-DfailIfNoTests=false \
						-Dcucumber.filter.tags='@monitoring'

					# Generar reporte Allure
					mvn -q io.qameta.allure:allure-maven:2.12.0:report
					mkdir -p target/reports-monitoring && cp -r target/site/allure-maven-plugin/* target/reports-monitoring/ || true
				"""
			}
			post {
				always {
					junit '**/target/surefire-reports/*.xml'
					publishHTML(target: [
						reportDir: 'target/reports-monitoring',
						reportFiles: 'index.html',
						reportName: 'Allure Report - MONITORING',
						keepAll: true,
						alwaysLinkToLastBuild: true,
						allowMissing: true
					])
				}
			}
		}

		// ===== ANALISIS SONARQUBE =====
		stage('SonarQube Analysis') {
			steps {
				withSonarQubeEnv("${SONARQUBE_ENV}") {
					sh """
						echo "🔹 Ejecutando análisis SonarQube..."
						mvn -q -DskipTests sonar:sonar \
							-Dsonar.projectKey=pruebas-e2e \
							-Dsonar.projectName=pruebas-e2e
					"""
				}
			}
		}

		// ===== QUALITY GATE =====
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
			echo '✅ Build OK'
		}
		failure {
			echo '❌ Build FAIL'
		}
	}
}
