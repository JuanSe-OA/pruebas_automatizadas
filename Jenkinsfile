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
		// Puertos/hosts según tu docker-compose (red interna)
		AUTH_APP_HOST        = 'auth-app'
		AUTH_APP_PORT        = '8080'

		MONITORING_APP_HOST  = 'monitoring-app'
		MONITORING_APP_PORT  = '8084'

		// Sonar en Jenkins (Manage Jenkins > Configure System)
		SONARQUBE_ENV        = 'sonar-local'
	}

	stages {

		stage('Checkout') {
			steps { checkout scm }
		}

		stage('Levantar stack') {
			steps {
				sh '''
          # Sube servicios (ajusta ruta si tu compose no está en la raíz)
          docker compose -f docker-compose.yml up -d

          echo "▶ Esperando /health de los servicios..."
          AUTH_URL="http://'${AUTH_APP_HOST}':'${AUTH_APP_PORT}'/health"
          MON_URL="http://'${MONITORING_APP_HOST}':'${MONITORING_APP_PORT}'/health"

          for url in "$AUTH_URL" "$MON_URL"; do
            echo "Esperando $url ..."
            # 60 intentos x 3s = ~3 minutos
            for i in $(seq 1 60); do
              if curl -fsS "$url" >/dev/null 2>&1; then
                echo "OK: $url"
                break
              fi
              sleep 3
              if [ $i -eq 60 ]; then
                echo "ERROR: Timeout esperando $url"
                exit 1
              fi
            done
          done
        '''
			}
		}

		stage('Test (Cucumber) - AUTH') {
			environment {
				BASE_URL = "http://${AUTH_APP_HOST}:${AUTH_APP_PORT}"
			}
			steps {
				sh """
          mvn -q clean test \
            -DBASE_URL=${BASE_URL}

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
			environment {
				BASE_URL = "http://${MONITORING_APP_HOST}:${MONITORING_APP_PORT}"
			}
			steps {
				sh """
          mvn -q clean test \
            -DBASE_URL=${BASE_URL} \
            -DfailIfNoTests=false
            # Si taggeas escenarios de monitoring, usa:
            # -Dcucumber.filter.tags='@monitoring'

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
			sh 'docker compose -f docker-compose.yml down -v || true'
		}
		success { echo '✅ Build OK' }
		failure { echo '❌ Build FAIL' }
	}
}
