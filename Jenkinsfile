pipeline {
	agent any

	tools { maven 'MAVEN' }

	options {
		timestamps()
		ansiColor('xterm')
	}

	parameters {
		// Si quieres poder cambiar endpoints al vuelo desde la UI de Jenkins
		string(name: 'AUTH_BASE_URL', defaultValue: 'http://auth-app:8080', description: 'Base URL Auth')
		string(name: 'MON_BASE_URL',  defaultValue: 'http://monitoring-app:8084', description: 'Base URL Monitoring')
	}

	environment {
		// Defaults si no usas parámetros
		AUTH_BASE_URL = "${params.AUTH_BASE_URL ?: 'http://auth-app:8080'}"
		MON_BASE_URL  = "${params.MON_BASE_URL  ?: 'http://monitoring-app:8084'}"

		SONARQUBE_ENV = 'sonar-local'
	}

	stages {
		stage('Checkout') {
			steps { checkout scm }
		}

		// (Opcional) Comprobar que los servicios estén vivos antes de probar
		stage('Health check (opcional)') {
			steps {
				sh '''
          set -e
          echo "▶ Verificando health de AUTH y MONITORING..."
          for URL in \
            "${AUTH_BASE_URL}/actuator/health" \
            "${MON_BASE_URL}/monitor/health"
          do
            echo "Comprobando $URL"
            for i in $(seq 1 30); do
              if curl -fsS "$URL" >/dev/null 2>&1; then
                echo "OK: $URL"
                break
              fi
              sleep 2
              if [ $i -eq 30 ]; then
                echo "ERROR: Timeout esperando $URL"
                exit 1
              fi
            done
          done
        '''
			}
		}

		// ====== SUITE @auth ======
		stage('Test @auth') {
			environment {
				BASE_URL = "${AUTH_BASE_URL}"
			}
			steps {
				// Si tus features usan variables como ${LOGIN_USERNAME}, ${LOGIN_PASSWORD}, etc.,
				// levántalas aquí. Ejemplo si las guardas en Jenkins Credentials:
				withCredentials([
					usernamePassword(credentialsId: 'auth-user-pass', usernameVariable: 'LOGIN_USERNAME', passwordVariable: 'LOGIN_PASSWORD'),
					string(credentialsId: 'register-email',  variable: 'REGISTER_EMAIL'),
					string(credentialsId: 'register-pass',   variable: 'REGISTER_PASSWORD'),
					string(credentialsId: 'register-phone',  variable: 'REGISTER_PHONE')
				]) {
					sh '''
            # Ejecuta solo escenarios con @auth
            mvn -q clean test \
              -DBASE_URL="${BASE_URL}" \
              -DfailIfNoTests=false \
              -Dcucumber.filter.tags='@auth'

            # Allure (si lo usas)
            mvn -q io.qameta.allure:allure-maven:2.12.0:report
            mkdir -p target/reports-auth && cp -r target/site/allure-maven-plugin/* target/reports-auth/ || true
          '''
				}
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

		// ====== SUITE @monitoring ======
		stage('Test @monitoring') {
			environment {
				BASE_URL = "${MON_BASE_URL}"
			}
			steps {
				sh '''
          mvn -q clean test \
            -DBASE_URL="${BASE_URL}" \
            -DfailIfNoTests=false \
            -Dcucumber.filter.tags='@monitoring'

          mvn -q io.qameta.allure:allure-maven:2.12.0:report
          mkdir -p target/reports-monitoring && cp -r target/site/allure-maven-plugin/* target/reports-monitoring/ || true
        '''
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

		// (Opcional) Sonar para este repo de pruebas
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
		success { echo '✅ Build OK' }
		failure { echo '❌ Build FAIL' }
	}
}
