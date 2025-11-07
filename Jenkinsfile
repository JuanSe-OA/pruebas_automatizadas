pipeline {
	agent any

	tools { maven 'MAVEN' }

	options {
		timestamps()
		ansiColor('xterm')
	}

	environment {
		// Servicios (deben coincidir con los nombres del compose del orquestador)
		AUTH_APP_HOST       = 'auth-app'
		AUTH_APP_PORT       = '8080'
		ORCH_APP_HOST       = 'orchestrator'
		ORCH_APP_PORT       = '8082'
		NOTIF_APP_HOST      = 'notification-service'
		NOTIF_APP_PORT      = '8083'
		MONITORING_APP_HOST = 'monitoring-app'
		MONITORING_APP_PORT = '8084'

		SONARQUBE_ENV = 'sonar-local'

		// Repo de infraestructura (orquestador) que contiene el docker-compose.yml
		INFRA_REPO   = 'https://github.com/CamiloAst/Microservicios_Orquestador.git'
		INFRA_BRANCH = 'main'
		INFRA_DIR    = 'stack' // subcarpeta donde haremos el checkout
		COMPOSE_FILE = 'docker-compose.yml'
	}

	stages {
		stage('Checkout (pruebas)') {
			steps { checkout scm }
		}

		stage('Checkout orquestador (infra)') {
			steps {
				dir("${INFRA_DIR}") {
					// Público:
					git url: "${INFRA_REPO}", branch: "${INFRA_BRANCH}"
					// Privado (descomenta y crea el credential en Jenkins):
					// git url: "${INFRA_REPO}", branch: "${INFRA_BRANCH}", credentialsId: 'github-token'
				}
			}
		}

		stage('Levantar stack') {
			steps {
				dir("${INFRA_DIR}") {
					sh """
                        docker compose -f ${COMPOSE_FILE} up -d

                        echo "▶ Esperando endpoints de salud..."
                        AUTH_URL="http://${AUTH_APP_HOST}:${AUTH_APP_PORT}/actuator/health"
                        ORCH_URL="http://${ORCH_APP_HOST}:${ORCH_APP_PORT}/actuator/health"
                        NOTIF_URL="http://${NOTIF_APP_HOST}:${NOTIF_APP_PORT}/actuator/health"
                        MON_URL="http://${MONITORING_APP_HOST}:${MONITORING_APP_PORT}/monitor/health"

                        for url in "$AUTH_URL" "$ORCH_URL" "$NOTIF_URL" "$MON_URL"; do
                          echo "Esperando $url ..."
                          for i in \$(seq 1 60); do
                            if curl -fsS "$url" >/dev/null 2>&1; then
                              echo "OK: $url"
                              break
                            fi
                            sleep 3
                            if [ \$i -eq 60 ]; then
                              echo "ERROR: Timeout esperando $url"
                              exit 1
                            fi
                          done
                        done
                    """
				}
			}
		}

		stage('Test (Cucumber) - AUTH') {
			environment { BASE_URL = "http://${AUTH_APP_HOST}:${AUTH_APP_PORT}" }
			steps {
				sh """
                    mvn -q clean test -DBASE_URL=${BASE_URL}
                    mvn -q io.qameta.allure:allure-maven:2.12.0:report
                    mkdir -p target/reports-auth && cp -r target/site/allure-maven-plugin/* target/reports-auth/
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
						allowMissing: false
					])
				}
			}
		}

		stage('Test (Cucumber) - MONITORING') {
			environment { BASE_URL = "http://${MONITORING_APP_HOST}:${MONITORING_APP_PORT}" }
			steps {
				sh """
                    mvn -q clean test -DBASE_URL=${BASE_URL} -DfailIfNoTests=false
                    mvn -q io.qameta.allure:allure-maven:2.12.0:report
                    mkdir -p target/reports-monitoring && cp -r target/site/allure-maven-plugin/* target/reports-monitoring/
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
						allowMissing: false
					])
				}
			}
		}

		stage('SonarQube Analysis') {
			steps {
				withSonarQubeEnv("${SONARQUBE_ENV}") {
					sh '''
                        mvn -q -DskipTests sonar:sonar \
                          -Dsonar.projectKey=pruebas-e2e \
                          -Dsonar.projectName=pruebas-e2e
                    '''
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
		always {
			echo 'Bajando stack...'
			dir("${INFRA_DIR}") {
				sh 'docker compose -f ${COMPOSE_FILE} down -v || true'
			}
		}
		success { echo '✅ Build OK' }
		failure { echo '❌ Build FAIL' }
	}
}
